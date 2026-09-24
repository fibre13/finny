"""Статические начертания Inter из вариативного файла: 400, 700, 800 при opsz 14,
с подмножеством знаков для русского и латиницы. Результат — app/src/main/res/font.

Запуск из корня проекта: python scripts/inter-static.py <InterVariable.ttf>
Нужен пакет fontTools."""
import sys
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools import subset

SRC = sys.argv[1]
OUT = 'app/src/main/res/font/'
UNICODES = (list(range(0x20, 0x7F)) + list(range(0xA0, 0x180)) + list(range(0x400, 0x460))
            + list(range(0x2010, 0x2050)) + [0x20BD, 0x2116, 0x2122, 0x2212, 0x2248, 0x2260,
            0x2264, 0x2265, 0x2713, 0x2714, 0x2715, 0x2190, 0x2191, 0x2192, 0x2193, 0x25CF, 0x2022])
for name, w in (('inter_regular', 400), ('inter_bold', 700), ('inter_extrabold', 800)):
    f = TTFont(SRC)
    inst = instantiateVariableFont(f, {'wght': w, 'opsz': 14}, updateFontNames=True)
    opts = subset.Options()
    opts.layout_features = ['*']
    opts.name_IDs = ['*']
    opts.name_languages = ['*']
    opts.notdef_outline = True
    opts.glyph_names = False
    s = subset.Subsetter(opts)
    s.populate(unicodes=UNICODES)
    s.subset(inst)
    inst['OS/2'].usWeightClass = w
    inst.save(OUT + name + '.ttf')
    print(name, w)
