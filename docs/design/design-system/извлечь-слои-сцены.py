# -*- coding: utf-8 -*-
"""Извлечение слоёв сцены из канвы экранов в отдельные SVG.

Сцена нарисована в документе одной разметкой, но слоями: земля с деревом,
дневное небо, ночное небо и дом. В приложении они нужны порознь: небо
подменяется при смене темы, дом появляется после покупки.

Скрипт вытаскивает группы по идентификаторам и оборачивает каждую в
самостоятельный SVG с той же системой координат. Дальше их переводит
в Vector Drawable соседний скрипт.
"""
import io
import os
import re
import sys

LAYERS = ("scene-bg", "sky-day", "sky-night", "pet-house")


def group_bounds(text, start):
    """Границы группы <g>, начинающейся в позиции start."""
    depth = 0
    pos = start
    while True:
        m = re.compile(r"<g\b|</g>").search(text, pos)
        if not m:
            raise SystemExit("не найден конец группы")
        if m.group(0) == "<g":
            depth += 1
        else:
            depth -= 1
            if depth == 0:
                return m.end()
        pos = m.end()


def main(doc_path, out_dir):
    s = io.open(doc_path, encoding="utf-8").read()
    os.makedirs(out_dir, exist_ok=True)

    for layer in LAYERS:
        marker = s.index(f'id="{layer}"')
        start = s.rindex("<g", 0, marker)
        body = s[start:group_bounds(s, start)]

        # Система координат берётся у объемлющего SVG, а не назначается:
        # иначе слои разъедутся между собой.
        svg_start = s.rindex("<svg", 0, start)
        vb = re.search(r'viewBox="([^"]+)"', s[svg_start:svg_start + 400])
        if not vb:
            raise SystemExit(f"у слоя {layer} не найден viewBox")

        out = (
            f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{vb.group(1)}">\n'
            f"{body}\n</svg>\n"
        )
        path = os.path.join(out_dir, f"{layer}.svg")
        io.open(path, "w", encoding="utf-8", newline="\n").write(out)
        print(f"{layer:10} viewBox {vb.group(1):14} -> {path}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
