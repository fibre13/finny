"""Предметы двора на главной: лавка, доски, книга на пеньке, сундучок, мячик, значки.

Рисуются примитивами в сетке палитры и записываются исходниками
src/yard/*.txt в формате art/pixel. Запуск: python tools/make_yard.py
"""
import os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "yard")


class G:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.g = [["."] * w for _ in range(h)]

    def rect(self, x, y, w, h, c):
        for j in range(y, y + h):
            for i in range(x, x + w):
                if 0 <= i < self.w and 0 <= j < self.h:
                    self.g[j][i] = c
        return self

    def box(self, x, y, w, h, fill, out="K"):
        self.rect(x, y, w, h, out)
        return self.rect(x + 1, y + 1, w - 2, h - 2, fill)

    def px(self, x, y, c):
        return self.rect(x, y, 1, 1, c)

    def stamp(self, rows, x, y):
        for j, r in enumerate(rows):
            for i, c in enumerate(r):
                if c != ".":
                    self.px(x + i, y + j, c)
        return self

    def save(self, name, comment):
        os.makedirs(OUT, exist_ok=True)
        with open(os.path.join(OUT, name + ".txt"), "w", encoding="utf-8", newline="\n") as f:
            # Назначение спрайта — в строке вызова save; в файле только заголовок.
            f.write(f"# {self.w} {self.h}\n# pivot 0 0\n")
            for r in self.g:
                f.write("".join(r) + "\n")


# Лавка — раздел «Покупки»
s = G(38, 34)
for i in range(0, 38, 4):
    s.rect(i, 0, 4, 7, "c" if (i // 4) % 2 else "R")
s.rect(0, 0, 38, 1, "K").rect(0, 0, 1, 7, "K").rect(37, 0, 1, 7, "K").rect(0, 7, 38, 1, "K")
for i in range(0, 38, 4):
    c = "c" if (i // 4) % 2 else "R"
    s.rect(i + 1, 8, 2, 1, c).px(i + 1, 9, "K").px(i + 2, 9, "K").px(i, 8, "K").px(i + 3, 8, "K")
s.rect(6, 9, 26, 14, "y").box(2, 8, 4, 24, "T").box(32, 8, 4, 24, "T")
s.box(7, 16, 5, 5, "R").px(9, 15, "E").box(13, 17, 6, 4, "O").rect(14, 18, 4, 1, "Y")
s.box(20, 15, 4, 6, "c").rect(21, 15, 2, 1, "u").box(25, 16, 6, 5, "g")
s.box(0, 21, 38, 12, "T").rect(1, 25, 36, 1, "t").rect(1, 29, 36, 1, "t").rect(1, 31, 36, 1, "t")
s.save("yard_shop", "лавка — раздел «Покупки»")

# Доска объявлений — раздел «План»
b = G(26, 32)
b.box(3, 14, 4, 18, "t").box(19, 14, 4, 18, "t").box(0, 0, 26, 19, "T").rect(1, 6, 24, 1, "t").rect(1, 12, 24, 1, "t")
b.box(4, 3, 18, 13, "c").rect(7, 7, 12, 1, "m").rect(7, 10, 12, 1, "m").rect(7, 13, 8, 1, "m").box(11, 1, 4, 4, "R")
b.save("yard_plan", "доска объявлений — раздел «План»")

# Школьная доска — раздел «Задания»; надпись мелом выводится текстом поверх
c = G(66, 50)
c.box(0, 0, 66, 40, "T").rect(2, 2, 62, 36, "D").box(4, 39, 58, 3, "t").rect(8, 39, 4, 1, "c").rect(16, 39, 3, 1, "Y")
c.box(8, 41, 3, 9, "t").box(55, 41, 3, 9, "t")
c.save("yard_tasks", "школьная доска — раздел «Задания»; поле мела: клетки 3..62 по x, 3..37 по y")

# Книга на пеньке — раздел «Словарик»
k = G(24, 24)
k.box(4, 13, 16, 11, "T").rect(5, 16, 14, 1, "t").rect(5, 20, 14, 1, "t").box(3, 11, 18, 4, "O").rect(5, 12, 14, 2, "y")
k.box(1, 2, 11, 9, "c").box(12, 2, 11, 9, "c").rect(11, 2, 2, 9, "K").rect(1, 10, 22, 1, "u")
for x0 in (3, 15):
    k.rect(x0, 4, 6, 1, "m").rect(x0, 6, 6, 1, "m").rect(x0, 8, 4, 1, "m")
k.rect(17, 9, 2, 3, "R")
k.save("yard_glossary", "книга на пеньке — раздел «Словарик»")

# Сундучок — раздел «Копилка»
h = G(26, 21)
h.box(0, 2, 26, 8, "T").rect(1, 3, 24, 1, "O").box(0, 9, 26, 12, "T").rect(1, 14, 24, 1, "t").rect(1, 18, 24, 1, "t")
h.box(4, 2, 3, 19, "m").box(19, 2, 3, 19, "m").box(10, 7, 6, 7, "Y").rect(12, 9, 2, 3, "K")
h.box(8, 0, 4, 4, "Y").box(13, 0, 4, 3, "Y")
h.save("yard_savings", "сундучок — раздел «Копилка»")

# Мячик — желание поиграть в облачке
m = G(7, 7)
m.stamp(["..KKK..", ".KRRcK.", "KRRccRK", "KccRRRK", "KcRRRrK", ".KRrrK.", "..KKK.."], 0, 0)
m.save("yard_ball", "мячик — питомец хочет играть")

# Буква храпа «Z» над палаткой, когда день закончен
G(5, 5).stamp(["KKKKK", "...K.", "..K..", ".K...", "KKKKK"], 0, 0).save("yard_z", "храп: буква Z")

# Значки кнопок шапки 12 × 12
G(12, 12).stamp(["....RRRR....", "...RRRRRR...", "..RR....RR..", "........RR..", ".......RR...",
                  "......RR....", ".....RR.....", ".....RR.....", "............", ".....RR.....",
                  ".....RR.....", "............"], 0, 0).save("yard_icon_help", "значок «Как играть»")
G(12, 12).stamp(["....KKKK....", "...K....K...", "...K....K...", "...K....K...", "..KKKKKKKK..",
                  "..KYYYYYYK..", "..KYYKKYYK..", "..KYYKKYYK..", "..KYYYYYYK..", "..KOOOOOOK..",
                  "..KKKKKKKK..", "............"], 0, 0).save("yard_icon_adult", "значок «Для взрослого»")
G(12, 12).stamp(["............", ".........EE.", "........KEEK", "......EEKEEK", ".....KEEKEEK",
                  "...EEKEEKEEK", "..KEEKEEKEEK", "EEKEEKEEKEEK", "EEKEEKEEKEEK", "KKKKKKKKKKKK",
                  "............", "............"], 0, 0).save("yard_icon_progress", "значок «Мой прогресс»")

# Подарок: коробка с бантом (знакомство, шаг 1)
gift = G(34, 32)
gift.box(6, 0, 10, 9, "Y").rect(9, 3, 4, 3, "O").box(18, 0, 10, 9, "Y").rect(21, 3, 4, 3, "O").box(14, 4, 6, 6, "O")
gift.box(0, 8, 34, 8, "R").rect(1, 9, 32, 1, "n").rect(1, 14, 32, 1, "r")
gift.box(2, 15, 30, 17, "R").rect(26, 16, 5, 15, "r")
gift.rect(15, 8, 4, 24, "Y").rect(18, 9, 1, 22, "O").rect(15, 8, 4, 1, "K").rect(15, 15, 4, 1, "K").rect(15, 31, 4, 1, "K")
gift.save("yard_gift", "подарок: коробка с бантом")

# Шарики четырёх цветов
BALLOON = ["..KKKKKK..", ".KaccCCCK.", "KcCCCCCCCK", "KCCCCCCCCK", "KCCCCCCCCK", "KCCCCCCCCK",
           ".KCCCCCCK.", "..KCCCCK..", "...KKKK...", "....KK....", "....K.....", ".....K....",
           "....K.....", ".....K....", "....K....."]
for name, ch in (("red", "R"), ("blue", "u"), ("yellow", "Y"), ("green", "g")):
    G(10, 15).stamp([r.replace("C", ch) for r in BALLOON], 0, 0).save("yard_balloon_" + name, "шарик")

# Стрелка «Назад» 12 × 8
G(12, 8).stamp(["............", "....K.......", "...KK.......", "..KKKKKKKKK.", "..KKKKKKKKK.", "...KK.......",
                "....K.......", "............"], 0, 0).save("yard_icon_back", "значок «Назад»")

print("ok")
