# -*- coding: utf-8 -*-
"""Расчёт контраста пар цвета по WCAG 2.1.

Порог 4.5:1 для текста (1.4.3) и 3:1 для нетекстовых элементов —
контуров, полос и разделителей (1.4.11).

Роль outlineVariant в перечень не входит: разделителей в приложении нет,
и ни один компонент этот цвет не рисует. Появится разделитель — пару
нужно завести и посчитать.

Запуск: python контраст.py
"""


def luminance(hex_color):
    v = hex_color.lstrip("#")
    parts = [int(v[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    lin = [c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4 for c in parts]
    return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2]


def ratio(a, b):
    la, lb = luminance(a), luminance(b)
    hi, lo = max(la, lb), min(la, lb)
    return (hi + 0.05) / (lo + 0.05)


def over(fg, bg, alpha):
    """Цвет полупрозрачной заливки поверх подложки."""
    f = fg.lstrip("#")
    b = bg.lstrip("#")
    out = [
        round(int(f[i:i + 2], 16) * alpha + int(b[i:i + 2], 16) * (1 - alpha))
        for i in (0, 2, 4)
    ]
    return "#%02X%02X%02X" % tuple(out)


# Земля на тёмной паре: заливка primary поверх фона, ближний холм — 0.34.
DARK_HILL = over("#8CCB96", "#14170F", 0.34)

# Тёмная пара: роль, цвет, подложка, порог
PAIRS = [
    ("onBackground на background", "#F0EDE2", "#14170F", 4.5),
    ("onSurface на surface", "#F0EDE2", "#1F241A", 4.5),
    ("onPrimary на primary", "#10230F", "#8CCB96", 4.5),
    ("onSecondary на secondary", "#221A00", "#D8A82E", 4.5),
    ("error на background", "#F2B8B5", "#14170F", 4.5),
    ("needs на surface", "#7FC9A0", "#1F241A", 4.5),
    ("wants на surface", "#E6A377", "#1F241A", 4.5),
    ("savings на surface", "#9DB6E8", "#1F241A", 4.5),
    ("disabledContent на disabledContainer", "#909787", "#2A2F24", 4.5),
    ("чернила 1b на background", "#F0EDE2", "#14170F", 3.0),
    ("чернила 1b на surface", "#F0EDE2", "#1F241A", 3.0),
    ("disabledOutline на disabledContainer", "#71796A", "#2A2F24", 3.0),
    ("outline (рамка поля ввода) на background", "#7C8473", "#14170F", 3.0),
    ("outline (рамка поля ввода) на surface", "#7C8473", "#1F241A", 3.0),
    ("primary (полоса) на background", "#8CCB96", "#14170F", 3.0),
    ("secondary (солнце) на background", "#D8A82E", "#14170F", 3.0),
    # Окрас питомца на тёмной паре не подменяется, как и в канве. Порог
    # нетекстовый: фигура отделяется от фона заливкой, а не подписью.
    ("окрас «рыжий» на ближнем холме", "#E8913A", DARK_HILL, 3.0),
    ("окрас «серый» на ближнем холме", "#9AA5B1", DARK_HILL, 3.0),
    ("окрас «белый» на ближнем холме", "#EDE4D6", DARK_HILL, 3.0),
    ("окрас «рыжий» на ночном небе", "#E8913A", "#14170F", 3.0),
    ("окрас «серый» на ночном небе", "#9AA5B1", "#14170F", 3.0),
    ("окрас «белый» на ночном небе", "#EDE4D6", "#14170F", 3.0),
]

if __name__ == "__main__":
    worst = None
    for name, fg, bg, need in PAIRS:
        r = ratio(fg, bg)
        mark = "да " if r >= need else "НЕТ"
        print(f"{mark} {r:5.2f}:1 (нужно {need}) — {name}: {fg} на {bg}")
        if r < need:
            worst = name
    print()
    print("все пороги пройдены" if worst is None else f"порог не пройден: {worst}")
