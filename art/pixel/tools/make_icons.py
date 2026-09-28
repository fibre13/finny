"""Значки интерфейса и товаров лавки в пиксельном стиле двора.

Товары — 16 × 16, как картинки карточек заданий (make_items.py).
Служебные знаки — стрелка «открыть», крестик «закрыть», флажок цели.
Записываются исходниками src/yard/*.txt в формате art/pixel.
Запуск: python tools/make_icons.py, затем python tools/build.py.
"""
import os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "yard")


ICONS = {
    "item_food": ("миска корма", [
        "................",
        "................",
        "................",
        "................",
        ".....KKKKKK.....",
        "....KYYYOYYK....",
        "...KYYOYYYOYK...",
        "..KKKKKKKKKKKK..",
        "..KggEEEEEEEEK..",
        "..KEEEEEEEEEEK..",
        "...KEEEEEEEEK...",
        "...KEEEEEEEEK...",
        "....KDDDDDDK....",
        ".....KKKKKK.....",
        "................",
        "................",
    ]),
    "item_vitamins": ("баночка витаминов", [
        "................",
        "................",
        ".....KKKKKK.....",
        ".....KRRRRK.....",
        ".....KrrrrK.....",
        "....KKKKKKKK....",
        "....KccccccK....",
        "....KccRRccK....",
        "....KcRRRRcK....",
        "....KcRRRRcK....",
        "....KccRRccK....",
        "....KccccccK....",
        "....KyyyyyyK....",
        "....KKKKKKKK....",
        "................",
        "................",
    ]),
    "item_brush": ("расчёска", [
        "................",
        "................",
        "................",
        "................",
        "..KKKKKKKKKKKK..",
        "..KTTTTTTTTTTK..",
        "..KttttttttttK..",
        "..KKKKKKKKKKKK..",
        "..KT.KT.KT.KTK..",
        "..KT.KT.KT.KTK..",
        "..KT.KT.KT.KTK..",
        "..KK.KK.KK.KKK..",
        "................",
        "................",
        "................",
        "................",
    ]),
    "item_ball": ("мячик", [
        "................",
        "................",
        ".....KKKKKK.....",
        "....KRRRRcRK....",
        "...KRRRRRRcRK...",
        "..KRRRRRRRRRRK..",
        "..KccccccccccK..",
        "..KuuuuuuuuuuK..",
        "..KuuuuuuuuuuK..",
        "..KccccccccccK..",
        "..KRRRRRRRRRRK..",
        "...KRRRRRRRRK...",
        "....KrrrrrrK....",
        ".....KKKKKK.....",
        "................",
        "................",
    ]),
    "item_bow": ("бантик", [
        "................",
        "................",
        "................",
        "................",
        "..KK........KK..",
        "..KPKK....KKPK..",
        "..KPPPKKKKPPPK..",
        "..KPPPKPPKPPPK..",
        "..KPPPKPPKPPPK..",
        "..KpppKKKKpppK..",
        "..KpKK.KK.KKpK..",
        "..KK..KpK...KK..",
        "......KpK.......",
        "......KKK.......",
        "................",
        "................",
    ]),
    "item_tent": ("домик-палатка", [
        "................",
        "................",
        ".......KK.......",
        "......KRrK......",
        "......KRrK......",
        ".....KRRrrK.....",
        ".....KRRrrK.....",
        "....KRRRrrrK....",
        "....KRRttrrK....",
        "...KRRRttrrrK...",
        "...KRRttttrrK...",
        "..KRRRttttrrrK..",
        "..KRRttttttrrK..",
        ".KRRRttttttrrrK.",
        ".KKKKKKKKKKKKKK.",
        "................",
    ]),
    "item_scarf": ("шарфик", [
        "................",
        "................",
        "..KKKKKKKKKKKK..",
        "..KRRccRRccRRK..",
        "..KrrccrrccrrK..",
        "..KKKKKKKRRccK..",
        "........KRRccK..",
        "........KccRRK..",
        "........KccRRK..",
        "........KRRccK..",
        "........KRRccK..",
        "........KKKKKK..",
        "........K.K.K...",
        "................",
        "................",
        "................",
    ]),
    "item_hat": ("шапочка", [
        "................",
        "................",
        "......KKKK......",
        ".....KYYYYK.....",
        ".....KYYOYK.....",
        "....KKKKKKKK....",
        "...KRRRRRRRRK...",
        "...KRRRRRRRRK...",
        "..KRRRRRRRRrrK..",
        "..KKKKKKKKKKKK..",
        "..KccccccccccK..",
        "..KyyyyyyyyyyK..",
        "..KKKKKKKKKKKK..",
        "................",
        "................",
        "................",
    ]),
    # Стрелка «открыть» в конце нажимаемой строки.
    "ui_chevron": ("стрелка «открыть»", [
        "........",
        ".KK.....",
        ".KKK....",
        "..KKK...",
        "...KKK..",
        "...KKK..",
        "..KKK...",
        ".KKK....",
        ".KK.....",
        "........",
    ]),
    # Крестик «закрыть сообщение».
    "ui_close": ("крестик", [
        "KK......KK",
        "KKK....KKK",
        ".KKK..KKK.",
        "..KKKKKK..",
        "...KKKK...",
        "...KKKK...",
        "..KKKKKK..",
        ".KKK..KKK.",
        "KKK....KKK",
        "KK......KK",
    ]),
    # Флажок цели у суммы на копилку.
    "ui_flag": ("флажок цели", [
        ".KKKKKK...",
        ".KRRRRRK..",
        ".KRRRRRRK.",
        ".KrrrrrK..",
        ".KKKKKK...",
        ".K........",
        ".K........",
        ".K........",
        ".K........",
        ".K........",
        "KKK.......",
    ]),
}

PALETTE = set("KFfGzcblDEegYyORrPpnNAaTtmu.")


def main():
    for sid, (title, rows) in ICONS.items():
        w, h = len(rows[0]), len(rows)
        for i, r in enumerate(rows):
            assert len(r) == w, f"{sid}: строка {i} длиной {len(r)}, ожидалось {w}"
            bad = set(r) - PALETTE
            assert not bad, f"{sid}: символы вне палитры {bad}"
        with open(os.path.join(OUT, sid + ".txt"), "w", encoding="utf-8", newline="\n") as f:
            f.write(f"# {w} {h}\n# pivot 0 0\n" + "\n".join(rows) + "\n")
        print(f"{sid}: {w}×{h} — {title}")


if __name__ == "__main__":
    main()
