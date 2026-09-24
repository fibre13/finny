#!/usr/bin/env python3
"""Сборка пиксельной графики «Питомец Финни».

Читает исходники из art/pixel/src, проверяет их и пишет:
  art/pixel/build/pixel_art.json — палитра, спрайты, якоря, анимация;
  art/pixel/preview/index.html   — страница просмотра со встроенными данными.

Запуск из корня репозитория:
  python art/pixel/tools/build.py

Ошибки печатаются в виде «файл:строка: сообщение»; при ошибках код выхода 1.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.dirname(HERE)
SRC = os.path.join(ART, "src")
OUT_JSON = os.path.join(ART, "build", "pixel_art.json")
TEMPLATE = os.path.join(HERE, "preview_template.html")
OUT_HTML = os.path.join(ART, "preview", "index.html")
PET_PARTS = os.path.join(ART, "..", "..", "content", "src", "main", "assets", "pet_parts.json")

PET_W = PET_H = 48
SCENE_W, SCENE_H = 180, 100
GOAL_W = GOAL_H = 32
FX_MAX = 7
BASELINE = 45
OUTLINE = "#23201A"
FUR = "FfG"
MAX_COLORS = 32
MAX_JSON_BYTES = 150 * 1024
MAX_REACTION_TICKS = 16  # 2 с при такте 125 мс

STAGES = ("baby", "teen", "adult")
EYES = ("normal", "happy", "tired", "blink")
MOUTHS = ("smile", "neutral", "sad")
ANCHOR_KEYS = ("eyes", "mouth", "bow", "scarf", "fx_head")
GOALS = ("scooter", "aquarium", "party")
FX_NAMES = ("coin", "heart", "sparkle", "crumb", "note")

errors = []
warnings = []


def rel(path):
    return os.path.relpath(path, os.path.join(ART, "..", "..")).replace("\\", "/")


def err(path, line, msg):
    errors.append(f"{rel(path)}:{line}: {msg}")


def warn(path, line, msg):
    warnings.append(f"{rel(path)}:{line}: {msg}")


# --- Палитра ---------------------------------------------------------------

def read_palette():
    path = os.path.join(SRC, "palette.txt")
    chars, colors, names = [], [], []
    ramps, shadow = {}, {}
    hex_re = re.compile(r"^#[0-9A-Fa-f]{6}$")
    with open(path, encoding="utf-8") as f:
        for n, line in enumerate(f, 1):
            s = line.strip()
            if not s or s.startswith("#"):
                continue
            parts = s.split()
            if parts[0] == "ramp":
                if len(parts) != 5 or not all(hex_re.match(p) for p in parts[2:]):
                    err(path, n, "рампа: ожидается «ramp имя #F #f #G»")
                    continue
                ramps[parts[1]] = [p.upper() for p in parts[2:]]
            elif parts[0] == "shadow":
                if len(parts) != 3 or not hex_re.match(parts[2]):
                    err(path, n, "тень: ожидается «shadow фон #RRGGBB»")
                    continue
                shadow[parts[1]] = parts[2].upper()
            else:
                ch = parts[0]
                if len(ch) != 1 or not ch.isalnum() or not ch.isascii():
                    err(path, n, f"символ цвета «{ch}» должен быть одной латинской буквой или цифрой")
                    continue
                if len(parts) < 2 or not hex_re.match(parts[1]):
                    err(path, n, f"цвет «{ch}»: ожидается #RRGGBB")
                    continue
                if ch in chars:
                    err(path, n, f"символ «{ch}» задан повторно")
                    continue
                chars.append(ch)
                colors.append(parts[1].upper())
                names.append(" ".join(parts[2:]))
    for ch in ("K", "F", "f", "G", "z"):
        if ch not in chars:
            err(path, 0, f"нет обязательного символа «{ch}»")
    if "K" in chars and colors[chars.index("K")] != OUTLINE:
        err(path, 0, f"контур K должен быть {OUTLINE}")
    distinct = set(colors) | {c for r in ramps.values() for c in r} | set(shadow.values())
    if len(distinct) > MAX_COLORS:
        err(path, 0, f"цветов {len(distinct)}, допускается не больше {MAX_COLORS}")
    # Рампы сверяются с окрасами из pet_parts.json, если файл доступен.
    if os.path.exists(PET_PARTS):
        parts_json = json.load(open(PET_PARTS, encoding="utf-8"))
        for c in parts_json.get("colors", []):
            r = ramps.get(c["id"])
            if r is None:
                err(path, 0, f"нет рампы для окраса «{c['id']}»")
            elif r[0] != c["hex"].upper():
                err(path, 0, f"рампа «{c['id']}»: основной {r[0]}, а окрас {c['hex']}")
    else:
        warn(path, 0, "pet_parts.json не найден, рампы не сверены с окрасами")
    for key in ("card", "scene"):
        if key not in shadow:
            err(path, 0, f"нет значения тени под фигурой для фона «{key}»")
    return dict(path=path, chars=chars, colors=colors, names=names, ramps=ramps,
                shadow=shadow, distinct=len(distinct))


# --- Спрайты ---------------------------------------------------------------

def read_sprite(path, pal_chars):
    """Возвращает dict(w, h, pivot, rows, first_row_line) или None."""
    with open(path, encoding="utf-8") as f:
        lines = f.read().split("\n")
    if lines and lines[-1] == "":
        lines.pop()
    if not lines or not re.match(r"^# \d+ \d+$", lines[0]):
        err(path, 1, "первая строка должна быть «# ширина высота»")
        return None
    w, h = map(int, lines[0][2:].split())
    pivot = None
    i = 1
    if i < len(lines) and lines[i].startswith("#"):
        m = re.match(r"^# pivot (-?\d+) (-?\d+)$", lines[i])
        if not m:
            err(path, i + 1, "ожидается «# pivot x y»")
            return None
        pivot = [int(m.group(1)), int(m.group(2))]
        i += 1
    first = i + 1
    rows = lines[i:]
    ok = True
    if len(rows) != h:
        err(path, first, f"строк {len(rows)}, в заголовке высота {h}")
        ok = False
    allowed = set(pal_chars) | {"."}
    for k, r in enumerate(rows):
        if len(r) != w:
            err(path, first + k, f"длина строки {len(r)}, в заголовке ширина {w}")
            ok = False
        for x, ch in enumerate(r):
            if ch not in allowed:
                err(path, first + k, f"символ «{ch}» в столбце {x} не входит в палитру")
                ok = False
    if not ok:
        return None
    return dict(w=w, h=h, pivot=pivot or [0, 0], rows=rows, first=first)


def check_pet(path, sp):
    rows = sp["rows"]
    w, h = sp["w"], sp["h"]
    if (w, h) != (PET_W, PET_H):
        err(path, 1, f"холст питомца {w} × {h}, требуется {PET_W} × {PET_H}")
        return
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch == ".":
                continue
            if x in (0, w - 1) or y in (0, h - 1):
                err(path, sp["first"] + y, f"фигура выходит за холст: клетка ({x}, {y}) на краю")
            if ch in FUR:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    X, Y = x + dx, y + dy
                    if not (0 <= X < w and 0 <= Y < h) or rows[Y][X] == ".":
                        err(path, sp["first"] + y,
                            f"нет контура: клетка шерсти ({x}, {y}) граничит с прозрачной")
                        break
    body_rows = [y for y, r in enumerate(rows) if any(ch not in ".z" for ch in r)]
    if body_rows and body_rows[-1] != BASELINE - 1:
        err(path, sp["first"] + body_rows[-1],
            f"нижняя строка фигуры {body_rows[-1]}, фигура должна стоять на базовой линии y = {BASELINE}")
    for y, r in enumerate(rows):
        if "z" in r and y not in (BASELINE, BASELINE + 1):
            err(path, sp["first"] + y, f"тень под фигурой допускается только на строках {BASELINE}–{BASELINE + 1}")


def encode_rows(rows, chars):
    """Строка символов или серии [индекс, длина] (−1 — прозрачная), что короче."""
    index = {c: i for i, c in enumerate(chars)}
    out = []
    for r in rows:
        runs = []
        for ch in r:
            i = index.get(ch, -1)
            if runs and runs[-1][0] == i:
                runs[-1][1] += 1
            else:
                runs.append([i, 1])
        as_runs = json.dumps(runs, separators=(",", ":"))
        as_text = json.dumps(r)
        out.append(runs if len(as_runs) < len(as_text) else r)
    return out


def main():
    # Консоль Windows по умолчанию не в UTF-8: сообщения на русском печатаются явно в UTF-8.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    pal = read_palette()
    chars = pal["chars"]

    sprites = {}
    broken = set()  # файлы есть, но не разобраны из-за ошибок выше
    kinds = ("pet", "face", "accessory", "scene", "goal", "fx")
    for kind in kinds:
        d = os.path.join(SRC, kind)
        if not os.path.isdir(d):
            continue
        for name in sorted(os.listdir(d)):
            if not name.endswith(".txt"):
                continue
            path = os.path.join(d, name)
            sp = read_sprite(path, chars)
            if sp is None:
                broken.add(name[:-4])
                continue
            sid = name[:-4]
            if sid in sprites:
                err(path, 1, f"спрайт «{sid}» уже есть в {sprites[sid]['kind']}")
                continue
            sp.update(kind=kind, path=path)
            sprites[sid] = sp
            if kind == "pet":
                check_pet(path, sp)
            elif kind == "goal" and (sp["w"], sp["h"]) != (GOAL_W, GOAL_H):
                err(path, 1, f"иллюстрация цели {sp['w']} × {sp['h']}, требуется {GOAL_W} × {GOAL_H}")
            elif kind == "fx" and (sp["w"] > FX_MAX or sp["h"] > FX_MAX):
                err(path, 1, f"частица {sp['w']} × {sp['h']}, допускается до {FX_MAX} × {FX_MAX}")
            elif kind == "scene" and (sp["w"], sp["h"]) != (SCENE_W, SCENE_H) and not sid.startswith(("tent", "edge_")):
                err(path, 1, f"слой сцены {sp['w']} × {sp['h']}, требуется {SCENE_W} × {SCENE_H}")

    def need(sid, where, line=0):
        if sid in broken:
            return False
        if sid not in sprites:
            err(where, line, f"нет спрайта «{sid}»")
            return False
        return True

    # Законченные виды: у которых есть хотя бы один спрайт фигуры.
    species = sorted({sid.split("_")[0] for sid, s in sprites.items() if s["kind"] == "pet"})
    pet_dir = os.path.join(SRC, "pet")
    for spc in species:
        for st in STAGES:
            for fr in (0, 1):
                need(f"{spc}_{st}_{fr}", pet_dir)
        for e in EYES:
            need(f"{spc}_eyes_{e}", os.path.join(SRC, "face"))
        for m in MOUTHS + ("chew_0", "chew_1"):
            need(f"{spc}_mouth_{m}", os.path.join(SRC, "face"))
        for a in ("bow", "scarf"):
            if f"{a}_{spc}" not in sprites:
                for st in STAGES:
                    need(f"{a}_{spc}_{st}", os.path.join(SRC, "accessory"))

    # --- Якоря -------------------------------------------------------------
    apath = os.path.join(SRC, "anchors.json")
    anchors = json.load(open(apath, encoding="utf-8"))
    with open(apath, encoding="utf-8") as f:
        alines = f.read().split("\n")

    def aline(key):
        for n, l in enumerate(alines, 1):
            if f'"{key}":' in l:
                return n
        return 0

    def part_id(kind, spc, st, key):
        if kind == "eyes":
            return f"{spc}_eyes_normal"
        if kind == "mouth":
            return f"{spc}_mouth_neutral"
        if f"{key}_{spc}_{st}" in sprites:
            return f"{key}_{spc}_{st}"
        return f"{key}_{spc}"

    for key, val in anchors.items():
        if key == "scene":
            continue
        n = aline(key)
        m = re.match(r"^([a-z]+)_([a-z]+)$", key)
        if not m or m.group(2) not in STAGES:
            err(apath, n, f"ключ «{key}»: ожидается вид_стадия")
            continue
        spc, st = m.groups()
        for fr in ("0", "1"):
            if not need(f"{spc}_{st}_{fr}", apath, n):
                continue
            fa = val.get(fr)
            if not isinstance(fa, dict):
                err(apath, n, f"«{key}»: нет якорей кадра {fr}")
                continue
            for ak in ANCHOR_KEYS:
                p = fa.get(ak)
                if not (isinstance(p, dict) and isinstance(p.get("x"), int) and isinstance(p.get("y"), int)):
                    err(apath, n, f"«{key}» кадр {fr}: якорь «{ak}» должен быть целыми x, y")
                    continue
                if not (0 <= p["x"] < PET_W and 0 <= p["y"] < PET_H):
                    err(apath, n, f"«{key}» кадр {fr}: якорь «{ak}» вне холста")
                if ak == "fx_head":
                    continue
                pid = part_id(ak, spc, st, ak)
                if not need(pid, apath, n):
                    continue
                s = sprites[pid]
                variants = [pid]
                if ak == "eyes":
                    variants = [f"{spc}_eyes_{e}" for e in EYES]
                if ak == "mouth":
                    variants = [f"{spc}_mouth_{mm}" for mm in MOUTHS + ("chew_0", "chew_1")]
                for vid in variants:
                    if vid not in sprites:
                        continue
                    s = sprites[vid]
                    x0 = p["x"] - s["pivot"][0]
                    y0 = p["y"] - s["pivot"][1]
                    if x0 < 0 or y0 < 0 or x0 + s["w"] > PET_W or y0 + s["h"] > PET_H:
                        err(apath, n, f"«{key}» кадр {fr}: «{vid}» по якорю «{ak}» выходит за холст")

    scene_a = anchors.get("scene")
    if scene_a is None:
        err(apath, 0, "нет якорей сцены «scene»")
    else:
        n = aline("scene")
        for ak, sid, w, h in (("pet", None, PET_W, PET_H), ("goal", None, GOAL_W, GOAL_H), ("tent", "tent", 0, 0)):
            p = scene_a.get(ak)
            if not (isinstance(p, dict) and isinstance(p.get("x"), int) and isinstance(p.get("y"), int)):
                err(apath, n, f"сцена: якорь «{ak}» должен быть целыми x, y")
                continue
            if ak == "pet":
                px, py = 24, BASELINE
                probe = next((s for s in sprites.values() if s["kind"] == "pet"), None)
                if probe:
                    px, py = probe["pivot"]
            elif ak == "goal":
                probe = next((sprites[g] for g in GOALS if g in sprites), None)
                px, py = probe["pivot"] if probe else (0, 0)
            else:
                if not need("tent", apath, n):
                    continue
                w, h = sprites["tent"]["w"], sprites["tent"]["h"]
                px, py = sprites["tent"]["pivot"]
            x0, y0 = p["x"] - px, p["y"] - py
            if x0 < 0 or y0 < 0 or x0 + w > SCENE_W or y0 + h > SCENE_H:
                err(apath, n, f"сцена: «{ak}» по якорю выходит за сцену")
        for sid in ("sky", "sky_1", "ground", "tent"):
            need(sid, os.path.join(SRC, "scene"))
        for gid in GOALS:
            need(gid, os.path.join(SRC, "goal"))

    # --- Анимация ----------------------------------------------------------
    npath = os.path.join(SRC, "animation.json")
    anim = json.load(open(npath, encoding="utf-8"))
    with open(npath, encoding="utf-8") as f:
        nlines = f.read().split("\n")

    def nline(key):
        for k, l in enumerate(nlines, 1):
            if f'"{key}":' in l:
                return k
        return 0

    if anim.get("tick_ms") != 125:
        err(npath, nline("tick_ms"), "такт должен быть 125 мс")
    if anim.get("max_particles", 0) > 24:
        err(npath, nline("max_particles"), "пул частиц не больше 24")
    fx_map = anim.get("fx", {})
    for name in FX_NAMES:
        if name not in fx_map:
            err(npath, nline("fx"), f"нет частицы «{name}»")
    for name, frames in fx_map.items():
        for fid in frames:
            need(fid, npath, nline(name))
    for fid in anim.get("sky", {}).get("frames", []):
        need(fid, npath, nline("sky"))
    max_off = anim.get("max_offset", 3)
    for sname, st in anim.get("states", {}).items():
        jump = st.get("jump")
        if jump and max(abs(v) for v in jump["offsets"]) > max_off:
            err(npath, nline(sname), f"состояние «{sname}»: смещение больше {max_off} клеток")
        bl = st.get("blink")
        if bl and bl["interval"][0] < 3:
            err(npath, nline(sname), f"состояние «{sname}»: моргание чаще трёх раз в секунду")
        for spc in species:
            if bl:
                need(f"{spc}_eyes_{bl['eyes']}", npath, nline(sname))
    for rname, r in anim.get("reactions", {}).items():
        ln = nline(rname)
        if r.get("ticks", 0) > MAX_REACTION_TICKS:
            err(npath, ln, f"реакция «{rname}» длиннее 2 с")
        for e in r.get("emit", []):
            if e.get("fx") not in fx_map:
                err(npath, ln, f"реакция «{rname}» ссылается на неизвестную частицу «{e.get('fx')}»")
            if e.get("from") not in ANCHOR_KEYS + ("outline", "base"):
                err(npath, ln, f"реакция «{rname}»: неизвестная точка «{e.get('from')}»")
            if e.get("at", 0) + e.get("life", 0) > r.get("ticks", 0):
                err(npath, ln, f"реакция «{rname}»: частица живёт дольше реакции")
        for j in r.get("jump", []):
            if max(abs(v) for v in j["offsets"]) > max_off:
                err(npath, ln, f"реакция «{rname}»: смещение больше {max_off} клеток")
        fl = r.get("flash")
        if fl and fl.get("ticks", 1) > 1:
            err(npath, ln, f"реакция «{rname}»: больше одного светлого кадра подряд")
        mouth = r.get("mouth")
        if mouth:
            for spc in species:
                for fid in mouth["frames"]:
                    need(f"{spc}_mouth_{fid}", npath, ln)
        if r.get("eyes"):
            for spc in species:
                need(f"{spc}_eyes_{r['eyes']}", npath, ln)
        # Одновременно живущих частиц не больше пула.
        alive = [0] * (r.get("ticks", 0) + 1)
        for e in r.get("emit", []):
            for t in range(e["at"], min(len(alive), e["at"] + e["life"])):
                alive[t] += 1
        if alive and max(alive) > anim.get("max_particles", 24):
            err(npath, ln, f"реакция «{rname}»: частиц одновременно больше пула")

    # --- Вывод -------------------------------------------------------------
    for w in warnings:
        print("предупреждение:", w)
    if errors:
        for e in errors:
            print("ошибка:", e)
        print(f"ошибок: {len(errors)}")
        return 1

    out_sprites = []
    for sid in sorted(sprites, key=lambda k: (kinds.index(sprites[k]["kind"]), k)):
        s = sprites[sid]
        out_sprites.append({"id": sid, "kind": s["kind"], "w": s["w"], "h": s["h"],
                            "pivot": s["pivot"], "rows": encode_rows(s["rows"], chars)})
    data = {
        "format": 1,
        "grid": {"pet": [PET_W, PET_H], "scene": [SCENE_W, SCENE_H], "goal": [GOAL_W, GOAL_H],
                 "baseline": BASELINE},
        "palette": {
            "chars": "".join(chars),
            "colors": pal["colors"],
            "transparent": -1,
            "fur": {c: chars.index(c) for c in FUR},
            "ramps": pal["ramps"],
            "shadow": {"index": chars.index("z"), **pal["shadow"]},
        },
        "species": species,
        "sprites": out_sprites,
        "anchors": anchors,
        "animation": anim,
    }
    text = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    size = len(text.encode("utf-8"))
    if size > MAX_JSON_BYTES:
        print(f"ошибка: {rel(OUT_JSON)}:0: размер {size} байт больше {MAX_JSON_BYTES}")
        return 1
    os.makedirs(os.path.dirname(OUT_JSON), exist_ok=True)
    with open(OUT_JSON, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)

    with open(TEMPLATE, encoding="utf-8") as f:
        page = f.read()
    marker = "/*@PIXEL_ART@*/null"
    if marker not in page:
        print(f"ошибка: {rel(TEMPLATE)}:0: нет метки {marker}")
        return 1
    page = page.replace(marker, text.replace("</", "<\\/"))
    os.makedirs(os.path.dirname(OUT_HTML), exist_ok=True)
    with open(OUT_HTML, "w", encoding="utf-8", newline="\n") as f:
        f.write(page)

    by_kind = {}
    for s in sprites.values():
        by_kind[s["kind"]] = by_kind.get(s["kind"], 0) + 1
    print(f"палитра: {len(chars)} символов, цветов с рампами и тенью: {pal['distinct']}")
    print("спрайтов: " + ", ".join(f"{k} {by_kind.get(k, 0)}" for k in kinds) + f", всего {len(sprites)}")
    for spc in species:
        n = sum(1 for sid, s in sprites.items()
                if s["kind"] in ("pet", "face", "accessory") and (sid.startswith(spc + "_") or sid.endswith("_" + spc)
                                                                  or f"_{spc}_" in sid))
        print(f"вид {spc}: спрайтов {n}")
    print(f"{rel(OUT_JSON)}: {size} байт")
    print(f"{rel(OUT_HTML)}: записан")
    return 0


if __name__ == "__main__":
    sys.exit(main())
