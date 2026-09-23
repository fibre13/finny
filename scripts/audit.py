# -*- coding: utf-8 -*-
"""Проверяет состав сборки на известные уязвимости по базе OSV.

Источник — OSV (https://osv.dev), открытая база уязвимостей Google,
сводящая в том числе GitHub Advisory Database. Ключ доступа не нужен.

Что уходит наружу: только координаты библиотек с открытым исходным кодом
и их версии — то же, что уже записано в SBOM и в файле блокировок. Ни
данных пользователя, ни исходного кода, ни ключей запрос не содержит.

Что проверяется: состав релизной сборки, то есть то, что попадает в APK
и доходит до пользователя. Средства сборки и зависимости тестов сюда
не входят — они до устройства не доезжают.

Запуск из корня репозитория:

    python scripts/audit.py

Результат: docs/sbom/audit-<версия>.md. Возвращает ненулевой код, если
найдена хотя бы одна уязвимость, — чтобы проверку можно было включить
в сборку.
"""

import glob
import io
import json
import socket
import sys
import urllib.error
import urllib.request
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OSV_BATCH = 'https://api.osv.dev/v1/querybatch'
# Контрольная проба: заведомо уязвимая версия с давно известными записями.
# Нужна, чтобы пустой результат означал «уязвимостей нет», а не «запрос
# не работает»: если проба не срабатывает, проверке верить нельзя.
CONTROL = {'name': 'org.apache.logging.log4j:log4j-core', 'version': '2.14.1'}
OSV_VULN = 'https://api.osv.dev/v1/vulns/'
TIMEOUT = 30


def latest_sbom():
    """Самый свежий перечень состава сборки."""
    files = sorted(glob.glob(str(ROOT / 'docs' / 'sbom' / '*-cyclonedx.json')))
    if not files:
        sys.exit('Перечень состава не найден. Сначала: python scripts/sbom.py')
    return Path(files[-1])


def post(url, payload):
    request = urllib.request.Request(
        url, data=json.dumps(payload).encode('utf-8'),
        headers={'Content-Type': 'application/json'},
    )
    with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
        return json.loads(response.read().decode('utf-8'))


def get(url):
    with urllib.request.urlopen(url, timeout=TIMEOUT) as response:
        return json.loads(response.read().decode('utf-8'))


def severity_of(vuln):
    """Наибольшая заявленная оценка серьёзности, если она указана."""
    ratings = []
    for entry in vuln.get('severity', []):
        ratings.append('%s %s' % (entry.get('type', ''), entry.get('score', '')))
    for affected in vuln.get('affected', []):
        ecosystem = affected.get('ecosystem_specific', {})
        if 'severity' in ecosystem:
            ratings.append(str(ecosystem['severity']))
    return '; '.join(sorted(set(r.strip() for r in ratings if r.strip()))) or 'не указана'


def main():
    sbom_path = latest_sbom()
    sbom = json.loads(sbom_path.read_text(encoding='utf-8'))
    version = sbom['metadata']['component']['version']
    components = [c for c in sbom['components'] if c['type'] != 'framework']

    queries = [
        {'package': {'ecosystem': 'Maven', 'name': '%s:%s' % (c['group'], c['name'])},
         'version': c['version']}
        for c in components
    ]

    control_query = {'package': {'ecosystem': 'Maven', 'name': CONTROL['name']},
                     'version': CONTROL['version']}

    socket.setdefaulttimeout(TIMEOUT)
    try:
        answer = post(OSV_BATCH, {'queries': queries + [control_query]})
    except (urllib.error.URLError, socket.timeout, OSError) as error:
        sys.exit(
            'База уязвимостей недоступна: %s.\n'
            'Проверка требует выхода в сеть; запустите её там, где он есть.' % error
        )

    results = answer.get('results', [])
    if len(results) != len(queries) + 1:
        sys.exit('База вернула %d ответов на %d запросов: результату верить нельзя.'
                 % (len(results), len(queries) + 1))

    control_hits = len(results[-1].get('vulns', []))
    if control_hits == 0:
        sys.exit(
            'Контрольная проба не сработала: для %s:%s база не вернула ни одной записи. '
            'Значит запрос построен неверно или база отвечает не тем — '
            'пустой результат по нашим зависимостям ничего не доказывает.'
            % (CONTROL['name'], CONTROL['version'])
        )

    findings = []
    for component, result in zip(components, results):
        for short in result.get('vulns', []):
            try:
                vuln = get(OSV_VULN + short['id'])
            except (urllib.error.URLError, socket.timeout, OSError):
                vuln = {'id': short['id'], 'summary': 'подробности получить не удалось'}
            findings.append({
                'component': '%s:%s:%s' % (component['group'], component['name'], component['version']),
                'id': vuln.get('id', short['id']),
                'aliases': vuln.get('aliases', []),
                'summary': (vuln.get('summary') or '').strip(),
                'severity': severity_of(vuln),
            })

    checked = len(components)
    skipped = len(sbom['components']) - checked

    lines = [
        '# Проверка состава сборки на известные уязвимости',
        '',
        'Проверено автоматически скриптом [`scripts/audit.py`](../../scripts/audit.py) '
        'по базе [OSV](https://osv.dev).',
        '',
        '| Что | Значение |',
        '|---|---|',
        '| Версия приложения | %s |' % version,
        '| Дата проверки | %s |' % date.today().isoformat(),
        '| Источник перечня | `%s` |' % sbom_path.name,
        '| Проверено компонентов | %d |' % checked,
        '| Пропущено спецификаций версий | %d |' % skipped,
        '| Найдено уязвимостей | %d |' % len(findings),
        '| Контрольная проба | `%s:%s` — %d записей, запрос работает |'
        % (CONTROL['name'], CONTROL['version'], control_hits),
        '',
    ]

    if findings:
        lines += ['## Найденное', '',
                  '| Компонент | Идентификатор | Серьёзность | Описание |', '|---|---|---|---|']
        for f in findings:
            ids = f['id'] + ((' (' + ', '.join(f['aliases']) + ')') if f['aliases'] else '')
            lines.append('| `%s` | %s | %s | %s |'
                         % (f['component'], ids, f['severity'], f['summary'] or '—'))
    else:
        lines.append('Уязвимостей в составе релизной сборки не найдено.')

    lines += [
        '',
        '## Границы проверки',
        '',
        '1. Проверяется только состав релизной сборки — то, что попадает в APK. '
        'Средства сборки и зависимости тестов не проверяются: до устройства они не доходят.',
        '2. Проверка показывает то, что известно базе OSV на дату запуска. '
        'Отсутствие находок не означает отсутствия уязвимостей.',
        '3. Собственный код приложения этой проверкой не охватывается.',
        '4. Пустой результат подтверждён контрольной пробой: в тот же запрос '
        'добавляется заведомо уязвимая версия, и если база не возвращает по ней '
        'записей, проверка останавливается с ошибкой вместо вывода «ничего не найдено».',
        '',
        'Наружу уходят только координаты библиотек с открытым исходным кодом и их версии.',
        '',
    ]

    out = ROOT / 'docs' / 'sbom' / ('audit-%s.md' % version)
    io.open(out, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines))

    print('Проверено компонентов: %d' % checked)
    print('Найдено уязвимостей: %d' % len(findings))
    for f in findings:
        print('  %s — %s (%s)' % (f['component'], f['id'], f['severity']))
    print('Записано: %s' % out.relative_to(ROOT))
    return 1 if findings else 0


if __name__ == '__main__':
    sys.exit(main())
