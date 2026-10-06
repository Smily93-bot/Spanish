"""Builds Lía's widget portraits from the first frame of her walk sheet.

lia_happy.webp  - her normal smiling face (goal met / streak alive)
lia_sad.webp    - the same portrait repainted sad: frown, raised inner brows,
                  droopy eyelids, a tear and a slightly faded palette (streak broken)

Usage: python3 tools/make_lia_moods.py   (needs Pillow, numpy, opencv-python-headless)
"""
import os

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app/src/main/res/drawable-nodpi")
FRAME = (164, 33, 387, 582)      # first walk frame in explorer_walk.webp
HEAD = (55, 0, 365, 310)         # head-and-shoulders crop inside the frame
K = 4                            # work at 4x for smooth strokes
OUT = 256

INK = (60, 22, 10, 255)
BROW = (28, 14, 10, 255)


def P(x, y):
    """Frame coordinates -> working-canvas coordinates."""
    return ((x - HEAD[0]) * K, (y - HEAD[1]) * K)


def inpaint(img, polys):
    """Fills polygons (frame coords) with surrounding skin using OpenCV inpainting."""
    rgba = np.array(img)
    mask = Image.new("L", img.size, 0)
    d = ImageDraw.Draw(mask)
    for poly in polys:
        d.polygon([P(*p) for p in poly], fill=255)
    m = np.array(mask.filter(ImageFilter.MaxFilter(9)))
    rgb = cv2.inpaint(np.ascontiguousarray(rgba[:, :, :3]), m, 12, cv2.INPAINT_TELEA)
    # Soften the patch edge so it blends with the painted shading.
    soft = np.array(mask.filter(ImageFilter.MaxFilter(9)).filter(ImageFilter.GaussianBlur(6))) / 255.0
    out = rgba.copy()
    for c in range(3):
        out[:, :, c] = (rgb[:, :, c] * soft + rgba[:, :, c] * (1 - soft)).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def stroke(d, pts, width, color):
    pts = [P(*p) for p in pts]
    d.line(pts, fill=color, width=width * K // 2, joint="curve")
    r = width * K // 4
    for x, y in (pts[0], pts[-1]):
        d.ellipse((x - r, y - r, x + r, y + r), fill=color)


def make():
    sheet = Image.open(os.path.join(RES, "explorer_walk.webp")).convert("RGBA")
    x, y, w, h = FRAME
    frame = sheet.crop((x, y, x + w, y + h))
    head = frame.crop(HEAD)
    big = head.resize((head.width * K, head.height * K), Image.LANCZOS)

    happy = head.resize((OUT, OUT), Image.LANCZOS)
    happy.save(os.path.join(RES, "lia_happy.webp"), "WEBP", quality=92)

    # --- sad face ---------------------------------------------------------------
    sad = inpaint(big, [
        [(212, 160), (254, 158), (256, 186), (214, 186)],        # smile
        [(198, 102), (238, 100), (240, 119), (198, 121)],        # left brow
        [(254, 104), (278, 104), (278, 119), (254, 119)],        # right brow
    ])
    d = ImageDraw.Draw(sad)
    # Frown: a small arch with the corners pulled down.
    stroke(d, [(222, 179), (228, 174), (236, 172), (244, 174), (249, 179)], 3, INK)
    # Worried brows: inner ends raised towards the middle of the face.
    stroke(d, [(202, 118), (214, 115), (228, 110), (237, 105)], 4, BROW)
    stroke(d, [(256, 106), (264, 110), (275, 114)], 4, BROW)
    # Heavy upper eyelids over the top of each eye.
    skin = (226, 118, 56, 255)
    d.chord([*P(193, 113), *P(233, 141)], 180, 360, fill=skin)
    stroke(d, [(195, 128), (205, 127), (215, 127), (225, 127), (232, 129)], 3, BROW)
    d.chord([*P(248, 118), *P(272, 144)], 180, 360, fill=skin)
    stroke(d, [(250, 131), (258, 130), (266, 130), (271, 132)], 3, BROW)
    # Tear rolling down from the left eye.
    tx, ty = P(205, 160)
    r = 5 * K
    d.polygon([(tx, ty - 3 * r), (tx - r, ty), (tx + r, ty)], fill=(120, 200, 255, 235))
    d.ellipse((tx - r, ty - r, tx + r, ty + r), fill=(120, 200, 255, 235))
    d.ellipse((tx - r // 2, ty - r // 2 - r // 2, tx, ty), fill=(235, 250, 255, 255))

    sad = sad.resize((OUT, OUT), Image.LANCZOS)
    alpha = sad.getchannel("A")
    sad = ImageEnhance.Color(sad.convert("RGB")).enhance(0.8)
    sad = ImageEnhance.Brightness(sad).enhance(0.95).convert("RGBA")
    sad.putalpha(alpha)
    sad.save(os.path.join(RES, "lia_sad.webp"), "WEBP", quality=92)


if __name__ == "__main__":
    make()
