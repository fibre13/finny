# -*- coding: utf-8 -*-
"""Преобразование SVG выгрузки в Android Vector Drawable.

Поддерживается ровно то подмножество, которым пользуется дизайн-система:
path, circle, ellipse и g с translate. Окружности и эллипсы переводятся
в дуги: у Vector Drawable своих элементов для них нет.

Свойства заливки и обводки с группы переносятся на потомков: группа
Vector Drawable их не наследует.
"""
import io
import re
import sys
import xml.etree.ElementTree as ET

SVG = "{http://www.w3.org/2000/svg}"
INHERITED = ("fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "opacity")


def circle_path(cx, cy, r):
    return (f"M{cx - r},{cy} a{r},{r} 0 1,0 {2 * r},0 "
            f"a{r},{r} 0 1,0 {-2 * r},0 Z")


def ellipse_path(cx, cy, rx, ry):
    return (f"M{cx - rx},{cy} a{rx},{ry} 0 1,0 {2 * rx},0 "
            f"a{rx},{ry} 0 1,0 {-2 * rx},0 Z")


def num(v, default=0.0):
    try:
        return float(v)
    except (TypeError, ValueError):
        return default


def emit_path(d, attrs, indent):
    fill = attrs.get("fill", "#000000")
    stroke = attrs.get("stroke")
    # SVG opacity на элементе без обводки равна прозрачности заливки.
    alpha = attrs.get("opacity") or attrs.get("fill-opacity")

    out = [f'{indent}<path']
    out.append(f'{indent}    android:pathData="{d}"')
    if fill and fill != "none":
        out.append(f'{indent}    android:fillColor="{fill}"')
    if alpha:
        out.append(f'{indent}    android:fillAlpha="{float(alpha)}"')
    if stroke and stroke != "none":
        out.append(f'{indent}    android:strokeColor="{stroke}"')
        out.append(f'{indent}    android:strokeWidth="{attrs.get("stroke-width", "1")}"')
        cap = attrs.get("stroke-linecap")
        join = attrs.get("stroke-linejoin")
        if cap:
            out.append(f'{indent}    android:strokeLineCap="{cap}"')
        if join:
            out.append(f'{indent}    android:strokeLineJoin="{join}"')
    out.append(f'{indent}    />')
    return "\n".join(out)


def walk(node, inherited, indent):
    lines = []
    for child in node:
        tag = child.tag.replace(SVG, "")
        attrs = dict(inherited)
        attrs.update({k: v for k, v in child.attrib.items() if v is not None})

        if tag == "g":
            transform = child.attrib.get("transform", "")
            m = re.match(r"translate\(\s*([-\d.]+)[ ,]+([-\d.]+)\s*\)", transform)
            passed = {k: attrs[k] for k in INHERITED if k in attrs}
            if m:
                lines.append(f'{indent}<group')
                lines.append(f'{indent}    android:translateX="{m.group(1)}"')
                lines.append(f'{indent}    android:translateY="{m.group(2)}">')
                lines.append(walk(child, passed, indent + "    "))
                lines.append(f'{indent}</group>')
            else:
                if transform:
                    raise SystemExit(f"не поддержано преобразование: {transform}")
                lines.append(walk(child, passed, indent))

        elif tag == "path":
            lines.append(emit_path(child.attrib["d"], attrs, indent))

        elif tag == "circle":
            d = circle_path(num(child.attrib.get("cx")), num(child.attrib.get("cy")),
                            num(child.attrib.get("r")))
            lines.append(emit_path(d, attrs, indent))

        elif tag == "ellipse":
            d = ellipse_path(num(child.attrib.get("cx")), num(child.attrib.get("cy")),
                             num(child.attrib.get("rx")), num(child.attrib.get("ry")))
            lines.append(emit_path(d, attrs, indent))

        elif tag in ("metadata", "title", "desc"):
            continue

        else:
            raise SystemExit(f"не поддержан элемент: {tag}")

    return "\n".join(x for x in lines if x)


def convert(src, dst, comment):
    text = io.open(src, encoding="utf-8").read()
    text = re.sub(r"<metadata.*?</metadata>", "", text, flags=re.S)
    root = ET.fromstring(text)

    vb = root.attrib["viewBox"].split()
    w, h = vb[2], vb[3]

    body = walk(root, {}, "    ")
    xml = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        f"<!--\n    {comment}\n-->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{w}dp"\n'
        f'    android:height="{h}dp"\n'
        f'    android:viewportWidth="{w}"\n'
        f'    android:viewportHeight="{h}">\n\n'
        f"{body}\n\n"
        "</vector>\n"
    )
    io.open(dst, "w", encoding="utf-8", newline="\n").write(xml)
    print("записан:", dst)


if __name__ == "__main__":
    convert(sys.argv[1], sys.argv[2], sys.argv[3])
