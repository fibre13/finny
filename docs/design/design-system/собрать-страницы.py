# -*- coding: utf-8 -*-
"""Сборка страниц предпросмотра дизайн-системы «Питомец Финни»."""
import io, os, json

# Каталог самого скрипта: путь не зависит от места, куда склонирован репозиторий.
ROOT = os.path.dirname(os.path.abspath(__file__))


def write(rel, text):
    p = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    io.open(p, "w", encoding="utf-8", newline="\n").write(text)
    print("записан:", rel)


def page(card, css_depth, title, body):
    up = "../" * css_depth
    return (
        f'<!-- @dsCard {card} -->\n'
        '<!DOCTYPE html>\n<html lang="ru">\n<head>\n<meta charset="utf-8">\n'
        '<meta name="viewport" content="width=device-width, initial-scale=1">\n'
        f'<title>{title}</title>\n'
        f'<link rel="stylesheet" href="{up}styles.css">\n'
        '</head>\n<body>\n' + body + '\n</body>\n</html>\n'
    )


# --- контраст -----------------------------------------------------------

def lum(h):
    h = h.lstrip('#')
    c = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    c = [x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4 for x in c]
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]


def ratio(a, b):
    la, lb = lum(a), lum(b)
    hi, lo = max(la, lb), min(la, lb)
    return (hi + 0.05) / (lo + 0.05)


LIGHT = {
    "primary": "#3D7C47", "onPrimary": "#FFFFFF",
    "secondary": "#F2B705", "onSecondary": "#2B2B2B",
    "background": "#FFFBF2", "onBackground": "#2B2B2B",
    "surface": "#FFFFFF", "onSurface": "#2B2B2B",
    "outline": "#8A8578", "outlineVariant": "#DAD4C4",
    "error": "#B3261E",
    "Нужное": "#2E6B4F", "Хочу": "#B4552A", "Копилка": "#35558A",
}
DARK = {
    "primary": "#8CCB96", "onPrimary": "#10230F",
    "secondary": "#D8A82E", "onSecondary": "#221A00",
    "background": "#14170F", "onBackground": "#F0EDE2",
    "surface": "#1F241A", "onSurface": "#F0EDE2",
    "outline": "#7C8473", "outlineVariant": "#4A5142",
    "error": "#F2B8B5",
    "Нужное": "#7FC9A0", "Хочу": "#E6A377", "Копилка": "#9DB6E8",
}

# С чем считается контраст для каждой роли.
AGAINST = {
    "onPrimary": "primary", "onSecondary": "secondary",
    "onBackground": "background", "onSurface": "surface",
    "outline": "background", "outlineVariant": "background",
    "error": "background",
    "Нужное": "surface", "Хочу": "surface", "Копилка": "surface",
}
THRESHOLD = {"outline": 3.0, "outlineVariant": None}


def swatch_rows(scheme):
    rows = []
    for role, value in scheme.items():
        base = AGAINST.get(role)
        if base:
            r = ratio(value, scheme[base])
            need = THRESHOLD.get(role, 4.5)
            if need is None:
                verdict = "линейка, порог не применяется"
            else:
                verdict = f"{r:.2f}:1 к {base}" + ("" if r >= need else f" — ниже {need}")
        else:
            verdict = "—"
        rows.append(
            f'<div class="kv" style="padding:6px 0;border-bottom:1px solid var(--c-outline-variant)">'
            f'<span class="swatch" style="background:{value};border:1px solid var(--c-outline)"></span>'
            f'<span class="k" style="margin-left:12px">{role}</span>'
            f'<span class="v" style="font-family:ui-monospace,monospace">{value}</span>'
            f'<span style="flex:1;text-align:right;font-size:14px;color:var(--c-disabled-text)">{verdict}</span>'
            f'</div>'
        )
    return "\n".join(rows)


# --- foundations/color --------------------------------------------------

write("foundations/color.html", page(
    'group="Основы" name="Цвет" subtitle="Роли, обе пары, контраст к фону" viewport="700x820"',
    1, "Цвет",
    f"""<h1>Цвет</h1>
<p class="note">Роли совпадают с Theme.kt. Контраст рассчитан по WCAG: для текста порог 4,5:1,
для контура интерактивного элемента — 3:1 по 1.4.11. Служебная линейка outlineVariant под порог
не подпадает.</p>

<h2>Светлая пара</h2>
<div class="card">{swatch_rows(LIGHT)}</div>

<h2>Тёмная пара</h2>
<div class="card" data-theme="dark" style="background:{DARK['surface']};color:{DARK['onSurface']}">
{swatch_rows(DARK)}
</div>

<p class="note">Цвета направлений «Нужное», «Хочу», «Копилка» — не роли интерфейса, а обозначения
учебных категорий. В коде вынесены отдельно от схемы Material. Цвет только дополняет подпись
и пиктограмму: категория читается без него.</p>"""))

# --- foundations/type ---------------------------------------------------

TYPE = [
    ("Заголовок экрана", "24 / 32", "700", "titleLarge", "День 1"),
    ("Заголовок карточки", "16 / 24", "700", "titleMedium", "Как себя чувствует Финни"),
    ("Крупный текст", "18 / 26", "400", "bodyLarge", "Монет мало, а хочется многого"),
    ("Основной текст", "16 / 24", "400", "bodyMedium", "Корм, вода, уход. Это покупают в первую очередь."),
    ("Надпись на кнопке", "18 / 24", "700", "labelLarge", "Утвердить план"),
    ("Подпись под фигурой", "14 / 20", "400", "labelMedium", "Малыш, бантик"),
]

rows = "\n".join(
    f"""<div style="padding:12px 0;border-bottom:1px solid var(--c-outline-variant)">
  <div class="kv" style="margin-bottom:6px">
    <span class="k" style="font-size:14px;color:var(--c-disabled-text)">{n} · {s} · {w} · <code>{k}</code></span>
  </div>
  <div style="font-size:{s.split(' / ')[0]}px;line-height:{s.split(' / ')[1]}px;font-weight:{w}">{sample}</div>
</div>"""
    for n, s, w, k, sample in TYPE
)

write("foundations/type.html", page(
    'group="Основы" name="Типографика" subtitle="Системный Roboto, основной текст от 16 sp" viewport="700x680"',
    1, "Типографика",
    f"""<h1>Типографика</h1>
<p class="note">Системный Roboto. Веб-шрифты не подключаются: приложение работает без сети,
а распространённые пары вроде Caprasimo и Figtree вдобавок не содержат кириллицы.</p>
<div class="card">{rows}</div>
<p class="note">Основной текст 16 sp — нижняя граница, мельче нельзя. Раскладка обязана
выдерживать системное увеличение шрифта до 1,5× без наложений: подпись занимает свободную
ширину и переносится, числовое значение отделено отступом и не переносится.</p>"""))

# --- foundations/layout -------------------------------------------------

SPACES = [4, 6, 8, 10, 12, 14, 16, 18, 24]
RADII = [("Ярлык", "--r-tag", 10), ("Кнопка", "--r-button", 16),
         ("Карточка", "--r-card", 24), ("Рама питомца", "--r-pet", 28)]

space_bars = "\n".join(
    f'<div class="kv" style="padding:4px 0"><span class="k">{v} dp</span>'
    f'<span style="flex:3"><span style="display:inline-block;height:14px;width:{v * 4}px;'
    f'background:var(--c-primary);border-radius:3px"></span></span></div>'
    for v in SPACES
)

radius_boxes = "\n".join(
    f'<div style="text-align:center"><div style="width:96px;height:72px;background:var(--c-surface);'
    f'border:1px solid var(--c-outline);border-radius:{v}px"></div>'
    f'<div style="font-size:14px;margin-top:6px">{n}<br><code>{t}</code> · {v} dp</div></div>'
    for n, t, v in RADII
)

write("foundations/layout.html", page(
    'group="Основы" name="Отступы и размеры" subtitle="Шкала отступов, радиусы, минимальная зона нажатия" viewport="700x700"',
    1, "Отступы и размеры",
    f"""<h1>Отступы и размеры</h1>

<h2>Шкала отступов</h2>
<div class="card">{space_bars}</div>

<h2>Радиусы</h2>
<div class="card" style="display:flex;gap:24px;flex-wrap:wrap">{radius_boxes}</div>

<h2>Обязательные размеры</h2>
<div class="card">
  <div class="kv" style="padding:6px 0"><span class="k">Минимальная высота интерактивного элемента</span><span class="v">48 dp</span></div>
  <div class="kv" style="padding:6px 0"><span class="k">Максимальная ширина содержимого</span><span class="v">640 dp</span></div>
  <div class="kv" style="padding:6px 0"><span class="k">Поля экрана</span><span class="v">16 dp</span></div>
  <div class="kv" style="padding:6px 0"><span class="k">Внутренний отступ карточки</span><span class="v">16 dp</span></div>
  <div class="kv" style="padding:6px 0"><span class="k">Фигура питомца</span><span class="v">140 dp</span></div>
  <div class="kv" style="padding:6px 0"><span class="k">Зона ползунка / ручка</span><span class="v">48 / 32 dp</span></div>
</div>
<p class="note">48 dp — требование, а не рекомендация: элемент мельче не проходит проверку.
На широких экранах содержимое ограничивается 640 dp и центрируется, строки текста
не растягиваются на всю ширину планшета.</p>"""))

# --- components/buttons -------------------------------------------------

write("components/buttons.html", page(
    'group="Компоненты" name="Кнопки и выбор" subtitle="Основная, вторичная, выбранная, неактивная с причиной" viewport="700x760"',
    1, "Кнопки и выбор",
    """<h1>Кнопки и выбор</h1>

<h2>Действия</h2>
<div class="card">
  <button class="btn btn-primary" style="margin-bottom:12px">Утвердить план</button>
  <button class="btn btn-secondary" style="margin-bottom:12px">Покупки</button>
  <button class="btn btn-primary is-disabled" disabled>Закончить день</button>
  <p class="note" style="margin-top:8px">Чтобы закончить день, сначала составь план.</p>
</div>
<p class="note">Неактивная кнопка обязательно сопровождается причиной текстом рядом.
Одного приглушённого цвета недостаточно: ребёнок должен понять, что делать дальше.</p>

<h2>Выбор из списка</h2>
<div class="card">
  <div style="display:flex;align-items:center;gap:12px;margin-bottom:8px">
    <span class="swatch" style="background:var(--c-pet-ginger)"></span>
    <button class="btn is-selected">Рыжий — надето</button>
  </div>
  <div style="display:flex;align-items:center;gap:12px;margin-bottom:8px">
    <span class="swatch" style="background:var(--c-pet-grey)"></span>
    <button class="btn btn-secondary">Серый</button>
  </div>
  <div style="display:flex;align-items:center;gap:12px">
    <span class="swatch" style="background:var(--c-pet-white)"></span>
    <button class="btn btn-secondary">Белый</button>
  </div>
</div>

<h2>Недоступное украшение</h2>
<div class="card">
  <button class="btn is-selected" style="margin-bottom:8px">Бантик — надето</button>
  <button class="btn is-disabled" disabled>Шарфик — не куплено</button>
  <p class="note" style="margin-top:8px">Украшения появляются здесь после покупки в разделе «Покупки».</p>
</div>
<p class="note">Выбранное помечается словом «— надето», недоступное — «— не куплено».
Подпись обязательна: состояние читается без цвета.</p>"""))

# --- components/cards ---------------------------------------------------

write("components/cards.html", page(
    'group="Компоненты" name="Карточки и сообщения" subtitle="Раздел, сообщение, проблемное сообщение, строка значения" viewport="700x740"',
    1, "Карточки и сообщения",
    """<h1>Карточки и сообщения</h1>

<h2>Карточка раздела</h2>
<div class="card">
  <p class="card-title">Монеты</p>
  <div class="kv" style="padding:4px 0"><span class="k">Можно потратить</span><span class="v">50 монет</span></div>
  <div class="kv" style="padding:4px 0"><span class="k">В копилке</span><span class="v">0 монет</span></div>
  <p class="note" style="margin:8px 0 0">Цель пока не выбрана. Загляни в копилку.</p>
</div>

<h2>Сообщение о произошедшем</h2>
<div class="card feedback">
  <p class="kicker">Что произошло</p>
  <p style="margin:0">Купили: Шарфик за 9 монет. У Финни радость выросла на 10.</p>
  <p class="next">Что дальше: Теперь это украшение можно надеть в гардеробе.</p>
  <button class="btn btn-secondary" style="margin-top:12px">Понятно</button>
</div>

<h2>Затруднение</h2>
<div class="card feedback problem">
  <p class="kicker">Внимание</p>
  <p style="margin:0">«Домик-палатка» стоит 25 монет, а у тебя 18. Не хватает 7 монет.</p>
  <p class="next">Что дальше: Выполни задание, выбери что-то дешевле или отложи покупку на завтра.</p>
  <button class="btn btn-secondary" style="margin-top:12px">Понятно</button>
</div>
<p class="note">Затруднение помечается словом «Внимание» и объясняет следующий шаг.
Ошибка ребёнка не наказывается: тон нейтральный, красный оставлен для сбоев ввода.</p>"""))

# --- components/stats ---------------------------------------------------

write("components/stats.html", page(
    'group="Компоненты" name="Показатели" subtitle="Пиктограмма, метка, число и полоса; цвет только дополняет" viewport="700x620"',
    1, "Показатели",
    """<h1>Показатели</h1>

<h2>Состояние питомца</h2>
<div class="card">
  <div class="stat">
    <div class="row"><span class="label">Забота: Голодный</span><span class="value">20 из 100</span></div>
    <div class="bar"><span style="width:20%"></span></div>
  </div>
  <div class="stat">
    <div class="row"><span class="label">Радость: Спокойный</span><span class="value">60 из 100</span></div>
    <div class="bar"><span style="width:60%"></span></div>
  </div>
  <div class="stat">
    <div class="row"><span class="label">Забота: Довольный</span><span class="value">90 из 100</span></div>
    <div class="bar"><span style="width:90%"></span></div>
  </div>
</div>
<p class="note">Показатель собран из четырёх частей: пиктограмма уровня, текстовая метка,
число и полоса. Программа чтения с экрана озвучивает его одной фразой: «Забота: Довольный, 90 из 100».</p>

<h2>Направления плана бюджета</h2>
<div class="card">
  <div class="stat">
    <div class="row"><span class="label">Нужное</span><span class="value">23 монеты</span></div>
    <div class="bar needs"><span style="width:46%"></span></div>
  </div>
  <div class="stat">
    <div class="row"><span class="label">Хочу</span><span class="value">11 монет</span></div>
    <div class="bar wants"><span style="width:22%"></span></div>
  </div>
  <div class="stat">
    <div class="row"><span class="label">Копилка</span><span class="value">15 монет</span></div>
    <div class="bar savings"><span style="width:30%"></span></div>
  </div>
</div>
<p class="note">Цвет направления только дополняет название и пиктограмму. Различать «Нужное»
и «Хочу» по одному цвету ребёнок не обязан.</p>"""))

# --- theme.json ---------------------------------------------------------

theme = {
    "name": "Питомец Финни",
    "source": "app/src/main/kotlin/ru/onefortwo/finny/ui/theme/Theme.kt",
    "direction": "1b «Аппликация»",
    "palette": {"light": LIGHT, "dark": DARK},
    "fonts": {"body": {"family": "Roboto", "system": True},
              "heading": {"family": "Roboto", "system": True}},
    "radius": {"tag": 10, "button": 16, "card": 24, "pet": 28},
    "space": SPACES,
    "constraints": {
        "minTouchTarget": 48,
        "minBodyText": 16,
        "maxContentWidth": 640,
        "minTextContrast": 4.5,
        "minNonTextContrast": 3.0,
        "offline": True,
        "animations": False,
    },
}
write("theme.json", json.dumps(theme, ensure_ascii=False, indent=2) + "\n")
