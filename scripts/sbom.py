# -*- coding: utf-8 -*-
"""Составляет перечень состава сборки (SBOM) в формате CycloneDX 1.5.

Состав берётся у самой сборки, а не выписывается из файлов зависимостей:
запускается `gradlew :app:dependencies --configuration releaseRuntimeClasspath`,
и в перечень попадает то, что действительно входит в APK. Лицензии читаются
из описаний артефактов (`.pom`), контрольные суммы считаются по самим файлам
в кэше Gradle.

Плагин для этого не подключается сознательно: он добавил бы зависимость
сборки ради документа, который собирается из уже доступных данных.

Запуск из корня репозитория:

    python scripts/sbom.py

Результат: docs/sbom/finny-<версия>-cyclonedx.json
"""

import hashlib
import io
import json
import os
import re
import subprocess
import sys
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CACHE = Path(os.path.expanduser('~/.gradle/caches/modules-2/files-2.1'))

# Написания одной и той же лицензии в описаниях артефактов.
SPDX = {
    'the apache software license, version 2.0': 'Apache-2.0',
    'the apache license, version 2.0': 'Apache-2.0',
    'apache license, version 2.0': 'Apache-2.0',
    'apache license v2.0': 'Apache-2.0',
    'apache-2.0': 'Apache-2.0',
    'apache 2.0': 'Apache-2.0',
    'apache 2': 'Apache-2.0',
    'the mit license': 'MIT',
    'mit license': 'MIT',
    'eclipse public license 1.0': 'EPL-1.0',
}


def app_version():
    """versionName и versionCode из файла сборки приложения."""
    text = (ROOT / 'app' / 'build.gradle.kts').read_text(encoding='utf-8')
    name = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    code = re.search(r'versionCode\s*=\s*(\d+)', text)
    return name.group(1), int(code.group(1))


def dependency_report():
    """Классы путей выполнения релиза по данным самой сборки."""
    gradlew = ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew')
    result = subprocess.run(
        [str(gradlew), ':app:dependencies',
         '--configuration', 'releaseRuntimeClasspath', '--console=plain'],
        cwd=str(ROOT), capture_output=True, text=True, encoding='utf-8', errors='replace',
    )
    if result.returncode != 0:
        sys.exit('Не удалось получить состав сборки:\n' + (result.stderr or result.stdout))
    return result.stdout


def resolved(report):
    """Пары «группа:артефакт» и разрешённая версия."""
    found = {}
    for line in report.split('\n'):
        if 'project ' in line:
            continue
        m = re.search(r'([A-Za-z][\w.\-]*):([\w.\-]+):([\w.\-]+)(\s*->\s*([\w.\-]+))?', line)
        if m:
            version = (m.group(5) or m.group(3)).strip()
            found[(m.group(1), m.group(2))] = version
    return found


def licenses_of(group, artifact, version):
    """Лицензии из описания артефакта; при отсутствии — из родительского."""
    names, parent = set(), None
    for pom in CACHE.glob(f'{group}/{artifact}/{version}/*/*.pom'):
        text = pom.read_text(encoding='utf-8', errors='ignore')
        for block in re.findall(r'<licenses>.*?</licenses>', text, re.S):
            names.update(n.strip() for n in re.findall(r'<name>([^<]+)</name>', block))
        if not names:
            p = re.search(r'<parent>(.*?)</parent>', text, re.S)
            if p:
                g = re.search(r'<groupId>([^<]+)</groupId>', p.group(1))
                a = re.search(r'<artifactId>([^<]+)</artifactId>', p.group(1))
                v = re.search(r'<version>([^<]+)</version>', p.group(1))
                if g and a and v:
                    parent = (g.group(1), a.group(1), v.group(1))
    if not names and parent:
        names = licenses_of(*parent)
    return names


def hashes_of(group, artifact, version):
    """
    Контрольные суммы двоичного файла артефакта.

    У части модулей AndroidX и kotlinx двоичный файл лежит не под именем
    самого модуля, а под именем платформенного варианта: `ui` собирается
    из `ui-android`, `kotlinx-coroutines-core` — из `kotlinx-coroutines-core-jvm`.
    Поэтому перебираются и такие имена.
    """
    names = (artifact, f'{artifact}-android', f'{artifact}-jvm')
    for name in names:
        for suffix in ('.aar', '.jar'):
            for path in CACHE.glob(f'{group}/{name}/{version}/*/*{suffix}'):
                data = path.read_bytes()
                return [
                    {'alg': 'SHA-1', 'content': hashlib.sha1(data).hexdigest()},
                    {'alg': 'SHA-256', 'content': hashlib.sha256(data).hexdigest()},
                ], path.name
    return [], None


def is_platform(artifact):
    """Спецификация согласованных версий: собственного двоичного файла не имеет."""
    return artifact.endswith('-bom')


def main():
    version_name, version_code = app_version()
    report = dependency_report()
    deps = resolved(report)

    components, without_license, without_binary = [], [], []
    for (group, artifact), version in sorted(deps.items()):
        names = licenses_of(group, artifact, version)
        if not names:
            without_license.append(f'{group}:{artifact}:{version}')
        checksums, file_name = hashes_of(group, artifact, version)
        if not checksums and not is_platform(artifact):
            without_binary.append(f'{group}:{artifact}:{version}')

        component = {
            'type': 'framework' if is_platform(artifact) else 'library',
            'bom-ref': f'pkg:maven/{group}/{artifact}@{version}',
            'group': group,
            'name': artifact,
            'version': version,
            'purl': f'pkg:maven/{group}/{artifact}@{version}',
            'scope': 'required',
        }
        if names:
            component['licenses'] = [
                ({'license': {'id': SPDX[n.lower()]}} if n.lower() in SPDX
                 else {'license': {'name': n}})
                for n in sorted(names)
            ]
        if checksums:
            component['hashes'] = checksums
        if file_name:
            # Имя файла, по которому посчитаны суммы: у части модулей
            # двоичный файл приходит из платформенного варианта.
            component['properties'] = [{'name': 'hashedFile', 'value': file_name}]
        components.append(component)

    bom = {
        '$schema': 'http://cyclonedx.org/schema/bom-1.5.schema.json',
        'bomFormat': 'CycloneDX',
        'specVersion': '1.5',
        'version': 1,
        'metadata': {
            'timestamp': date.today().isoformat(),
            'tools': [{'name': 'scripts/sbom.py', 'vendor': 'Finny'}],
            'component': {
                'type': 'application',
                'bom-ref': f'pkg:maven/ru.onefortwo/finny@{version_name}',
                'group': 'ru.onefortwo',
                'name': 'finny',
                'version': version_name,
                'purl': f'pkg:maven/ru.onefortwo/finny@{version_name}',
                'properties': [{'name': 'versionCode', 'value': str(version_code)}],
            },
        },
        'components': components,
    }

    out_dir = ROOT / 'docs' / 'sbom'
    out_dir.mkdir(parents=True, exist_ok=True)
    out = out_dir / f'finny-{version_name}-cyclonedx.json'
    io.open(out, 'w', encoding='utf-8', newline='\n').write(
        json.dumps(bom, ensure_ascii=False, indent=2) + '\n')

    print(f'Записано: {out.relative_to(ROOT)}')
    print(f'Компонентов: {len(components)}')
    print(f'Без объявленной лицензии: {len(without_license)}')
    for item in without_license:
        print('  ', item)
    print(f'Без двоичного файла (кроме спецификаций версий): {len(without_binary)}')
    for item in without_binary:
        print('  ', item)


if __name__ == '__main__':
    main()
