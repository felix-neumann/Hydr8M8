"""Generates Hydr8M8's vector drawables and sound effects.

Run from the repo root:  python tools/generate_assets.py
Outputs go to app/src/main/res/{drawable,raw}. Re-run after editing the shapes below.
"""

import math
import os
import struct
import wave

RES = os.path.join("app", "src", "main", "res")
DRAWABLE = os.path.join(RES, "drawable")
RAW = os.path.join(RES, "raw")

# --------------------------------------------------------------------------- shapes


def circle(cx, cy, r):
    return f"M{cx - r},{cy} a{r},{r} 0 1,0 {2 * r},0 a{r},{r} 0 1,0 {-2 * r},0 Z"


def ellipse(cx, cy, rx, ry):
    return f"M{cx - rx},{cy} a{rx},{ry} 0 1,0 {2 * rx},0 a{rx},{ry} 0 1,0 {-2 * rx},0 Z"


# Line icons on a 24x24 grid (stroke only). Mirrors mockups/v2.html.
DRINK_ICONS = {
    "glass_small": ["M6.5 9h11l-1.2 11h-8.6z", "M7.3 13.5h9.4"],
    "glass": ["M6 3h12l-1.6 18H7.6z", "M6.8 11h10.4"],
    "glass_large": ["M5 2.5h14l-1.8 19H6.8z", "M5.7 9.5h12.6", "M9 14v4"],
    "mug": ["M4 6h12v11a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3z", "M16 9h2a2.5 2.5 0 0 1 0 5h-2", "M4 10h12"],
    "bottle_small": ["M10 2.5h4v3l1.8 2.5V20a1.5 1.5 0 0 1-1.5 1.5H9.7A1.5 1.5 0 0 1 8.2 20V8L10 5.5z", "M8.2 12h7.6"],
    "bottle": ["M10 2h4v3l2.5 3.5V20a2 2 0 0 1-2 2h-5a2 2 0 0 1-2-2V8.5L10 5z", "M7.5 11h9", "M7.5 16h9"],
    "bottle_large": [
        "M10.2 1.5h3.6v2.5L17 7.5v13a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2v-13L10.2 4z",
        "M7 10h10",
        "M7 13.5h10",
        "M7 17h10",
    ],
    "carafe": ["M9 2.5h6l-0.8 4L18 12v7.5a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V12l3.8-5.5z", "M6 14h12"],
    "sports_bottle": [
        "M10 1.5h4v2.5h-4z",
        "M8.5 4h7a1.5 1.5 0 0 1 1.5 1.5V20a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2V5.5A1.5 1.5 0 0 1 8.5 4z",
        "M7 9h10",
        "M7 17h10",
    ],
    "sparkling": ["M9.5 2h5l-0.6 9a1.9 1.9 0 0 1-3.8 0z", "M12 13v8", "M9 21.5h6", "M9.8 6h4.4"],
    "wine": ["M7 2.5h10l-0.4 4.5a4.6 4.6 0 0 1-9.2 0z", "M12 11.6V21", "M8.5 21.5h7", "M7.3 6h9.4"],
    "beer": [
        "M5 7h10v12.5a1.5 1.5 0 0 1-1.5 1.5h-7A1.5 1.5 0 0 1 5 19.5z",
        "M15 9.5h2.5a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H15",
        "M5 7a2.2 2.2 0 0 1 2.5-3.5 2.6 2.6 0 0 1 4.6 0A2.2 2.2 0 0 1 15 7",
        "M8.5 11v6.5",
        "M11.5 11v6.5",
    ],
    "wheat_beer": ["M8 2.5h8l-1 6.5 1.3 7.5-1 5H8.7l-1-5L9 9z", "M8.6 6.5h6.8"],
    "long_drink": ["M4 4h16l-8 8z", "M12 12v9", "M8.5 21.5h7", "M7 7h10", circle(17.5, 3, 1.5)],
    "shot": ["M7 8h10l-1.5 12h-7z", "M7.5 12h9"],
}

# Other UI line icons (same style, not drink presets).
UI_ICONS = {
    "undo": ["M9 14 4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 0 11H11"],
}

MATE_BODY = "M54 10 C54 10 22 46 22 68 a32 32 0 0 0 64 0 C86 46 54 10 54 10Z"
INK = "#FF0B2C48"
WHITE = "#FFFFFFFF"


def fill(path, color):
    return ("fill", path, color)


def stroke(path, color, width):
    return ("stroke", path, color, width)


STAR = "l2.2,4.6 5,0.7 -3.6,3.5 0.9,5 -4.5,-2.4 -4.5,2.4 0.9,-5 -3.6,-3.5 5,-0.7z"

MATE_FACES = {
    "champion": [
        stroke("M27 60 L10 34", WHITE, 7),
        fill(circle(9, 32, 5.5), WHITE),
        stroke("M81 60 L98 34", WHITE, 7),
        fill(circle(99, 32, 5.5), WHITE),
        fill("M45 58 " + STAR, "#FFFFC83D"),
        fill("M63 58 " + STAR, "#FFFFC83D"),
        fill("M40 77 Q54 98 68 77 Z", INK),
        fill("M47 86 Q54 92 61 86 Q54 83 47 86Z", "#FFFF8FA3"),
        fill(ellipse(34, 78, 4.5, 2.8), "#FFFFB3C1"),
        fill(ellipse(74, 78, 4.5, 2.8), "#FFFFB3C1"),
        fill("M20 12 l5 1.5 -1.5 5 -5 -1.5z", "#FFFFC83D"),
        fill("M82 8 l5 -1.5 1.5 5 -5 1.5z", "#FFFF8FA3"),
        fill(circle(70, 18, 2.5), "#FF9FE0FF"),
        fill(circle(34, 22, 2.5), "#FFF2B6FF"),
        fill("M54 0 l2 5 5 2 -5 2 -2 5 -2 -5 -5 -2 5 -2z", "#FFFFC83D"),
    ],
    "happy": [
        fill(circle(45, 66, 5), INK),
        fill(circle(63, 66, 5), INK),
        stroke("M44 78 Q54 87 64 78", INK, 5),
        fill(ellipse(37, 74, 4, 2.5), "#FFFFB3C1"),
        fill(ellipse(71, 74, 4, 2.5), "#FFFFB3C1"),
    ],
    "thirsty": [
        fill(circle(45, 66, 5), INK),
        fill(circle(63, 66, 5), INK),
        stroke("M45 82 Q54 76 63 82", INK, 5),
        fill("M76 50 q4 8 0 12 q-4 -4 0 -12z", "#FF9FD2FF"),
    ],
    "very_thirsty": [
        stroke("M37 58 L49 54 M71 58 L59 54", INK, 3.5),
        fill("M39 65 h12 a6 6 0 0 1 -12 0z", INK),
        fill("M57 65 h12 a6 6 0 0 1 -12 0z", INK),
        stroke("M45 82 q4.5 -4 9 0 q4.5 4 9 0", INK, 4),
        fill("M78 46 q5 9 0 13 q-5 -4 0 -13z", "#FF9FD2FF"),
        stroke("M84 68 h12 l-1.5 16 h-9z", "#FF9FD2FF", 2.5),
    ],
    "tipsy": [
        stroke("M39 66 q6 -5 12 0 M57 66 q6 -5 12 0", INK, 5),
        stroke("M45 79 Q50 84 56 79 Q60 76 64 80", INK, 4.5),
        fill(ellipse(37, 74, 5, 3), "#FFFF8FA3"),
        fill(ellipse(71, 74, 5, 3), "#FFFF8FA3"),
    ],
    "sleepy": [
        stroke("M39 67 h12 M57 67 h12", INK, 5),
        fill(circle(54, 80, 4), INK),
        stroke("M70 24 h8 l-8 10 h8", "#FF9FD2FF", 2.5),
        stroke("M82 12 h6 l-6 7 h6", "#FF9FD2FF", 2),
    ],
}

# --------------------------------------------------------------------------- writers


def vector(size_dp, viewport, items, extra=""):
    body = []
    for item in items:
        if item[0] == "fill":
            _, d, color = item
            body.append(f'    <path android:fillColor="{color}" android:pathData="{d}" />')
        else:
            _, d, color, width = item
            body.append(
                f'    <path android:strokeColor="{color}" android:strokeWidth="{width}" '
                f'android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{d}" />'
            )
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{size_dp}dp" android:height="{size_dp}dp"\n'
        f'    android:viewportWidth="{viewport}" android:viewportHeight="{viewport}"{extra}>\n'
        + "\n".join(body)
        + "\n</vector>\n"
    )


def write(path, text):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def generate_drawables():
    os.makedirs(DRAWABLE, exist_ok=True)
    for name, paths in DRINK_ICONS.items():
        items = [stroke(p, WHITE, 1.6) for p in paths]
        write(os.path.join(DRAWABLE, f"ic_drink_{name}.xml"), vector(24, 24, items))
    for name, paths in UI_ICONS.items():
        items = [stroke(p, WHITE, 1.8) for p in paths]
        write(os.path.join(DRAWABLE, f"ic_{name}.xml"), vector(24, 24, items))

    for mood, face in MATE_FACES.items():
        # Blue outline keeps the white mate visible on light backgrounds.
        items = [fill(MATE_BODY, WHITE), stroke(MATE_BODY, "#FF3FA7FF", 3)] + face
        write(os.path.join(DRAWABLE, f"mate_{mood}.xml"), vector(108, 108, items))

    # Launcher foreground: happy mate scaled into the adaptive-icon safe zone.
    items = [fill(MATE_BODY, WHITE)] + MATE_FACES["happy"]
    inner = vector(108, 108, items).split("\n")
    paths = [line for line in inner if line.strip().startswith("<path")]
    fg = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp" android:height="108dp"\n'
        '    android:viewportWidth="108" android:viewportHeight="108">\n'
        '  <group android:scaleX="0.62" android:scaleY="0.62" android:pivotX="54" android:pivotY="56">\n'
        + "\n".join("  " + p for p in paths)
        + "\n  </group>\n</vector>\n"
    )
    write(os.path.join(DRAWABLE, "ic_launcher_foreground.xml"), fg)

    # Monochrome (themed icon) + notification icon: drop with eye holes (even-odd).
    holes = MATE_BODY + " " + circle(45, 66, 5) + " " + circle(63, 66, 5)
    mono = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp" android:height="108dp"\n'
        '    android:viewportWidth="108" android:viewportHeight="108">\n'
        '  <group android:scaleX="0.62" android:scaleY="0.62" android:pivotX="54" android:pivotY="56">\n'
        f'    <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd" android:pathData="{holes}" />\n'
        "  </group>\n</vector>\n"
    )
    write(os.path.join(DRAWABLE, "ic_launcher_monochrome.xml"), mono)
    stat = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="24dp" android:height="24dp"\n'
        '    android:viewportWidth="108" android:viewportHeight="108">\n'
        f'    <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd" android:pathData="{holes}" />\n'
        "</vector>\n"
    )
    write(os.path.join(DRAWABLE, "ic_stat_drop.xml"), stat)


# --------------------------------------------------------------------------- sounds

RATE = 44100


def tone(freq_start, freq_end, dur, amp, decay, harmonics=((1, 1.0),)):
    n = int(RATE * dur)
    out = []
    phase = 0.0
    for i in range(n):
        t = i / RATE
        f = freq_start + (freq_end - freq_start) * (i / n)
        phase += 2 * math.pi * f / RATE
        env = math.exp(-decay * t) * min(1.0, t / 0.004)  # 4 ms attack avoids clicks
        s = sum(a * math.sin(phase * h) for h, a in harmonics)
        out.append(amp * env * s)
    return out


def mix(tracks, total):
    buf = [0.0] * int(RATE * total)
    for start, samples in tracks:
        off = int(RATE * start)
        for i, s in enumerate(samples):
            if off + i < len(buf):
                buf[off + i] += s
    # Fade out the tail to avoid a click.
    fade = int(RATE * 0.02)
    for i in range(fade):
        buf[-1 - i] *= i / fade
    return buf


def save_wav(name, samples):
    os.makedirs(RAW, exist_ok=True)
    with wave.open(os.path.join(RAW, name), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 32767)) for s in samples))


BELL = ((1, 1.0), (2, 0.25), (3, 0.08))


def generate_sounds():
    # Water drop: quick upward pitch sweep with a tiny echo.
    drop = mix([(0.0, tone(500, 1100, 0.12, 0.35, 30)), (0.07, tone(900, 1400, 0.06, 0.12, 45))], 0.22)
    save_wav("sfx_drop.wav", drop)

    # Goal reached: soft two-note bell chime (E6 -> B6).
    goal = mix([(0.0, tone(1318.5, 1318.5, 0.6, 0.22, 6, BELL)), (0.14, tone(1975.5, 1975.5, 0.7, 0.2, 5, BELL))], 0.9)
    save_wav("sfx_goal.wav", goal)

    # Streak / milestone: rising C-major arpeggio with sparkle.
    notes = [1046.5, 1318.5, 1568.0, 2093.0]
    tracks = [(i * 0.09, tone(f, f, 0.7, 0.17, 5, BELL)) for i, f in enumerate(notes)]
    tracks.append((0.36, tone(4186.0, 4186.0, 0.4, 0.05, 9)))
    save_wav("sfx_streak.wav", mix(tracks, 1.2))


if __name__ == "__main__":
    generate_drawables()
    generate_sounds()
    print("assets generated")
