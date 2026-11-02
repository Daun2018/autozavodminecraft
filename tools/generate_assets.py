#!/usr/bin/env python3
"""Генерирует текстуры машин, иконки предметов и звук двигателя для avtovazmod.

Запуск из корня проекта:  python3 tools/generate_assets.py
Нужны: Pillow, numpy, scipy, ffmpeg (для .ogg).
Текстуры можно заменить на нарисованные вручную — раскладка полос описана в VazCarModel.java.
"""
import os
import random
import subprocess
import sys
import wave

import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "avtovazmod")
random.seed(2107)


def noisy(color, amount=4):
    return tuple(max(0, min(255, c + random.randint(-amount, amount))) if i < 3 else c
                 for i, c in enumerate(color))


def fill_band(img, y0, y1, color, amount=4):
    px = img.load()
    for y in range(y0, y1):
        for x in range(img.width):
            px[x, y] = noisy(color, amount)


def car_texture(body_rgb, name):
    """256x256: полосы цвета по вертикали (см. V_* в VazCarModel)."""
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    fill_band(img, 0, 80, body_rgb + (255,), 3)          # кузов
    fill_band(img, 80, 140, (140, 185, 215, 105), 0)     # стекло, полупрозрачное
    fill_band(img, 140, 190, (34, 34, 38, 255), 3)       # шины, бамперы, кресла, руль
    fill_band(img, 190, 210, (176, 178, 184, 255), 4)    # колпаки
    fill_band(img, 210, 230, (255, 240, 170, 255), 3)    # фары
    fill_band(img, 230, 256, (190, 20, 26, 255), 3)      # стоп-сигналы
    path = os.path.join(ASSETS, "textures", "entity", name)
    img.save(path)
    print("wrote", path)


def icon(name, body, cabin_top, cabin_h, wheel_r):
    """Иконка 16x16: машина сбоку."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    dark = (28, 28, 32, 255)
    glass = (150, 200, 225, 255)
    shade = tuple(int(c * 0.72) for c in body) + (255,)
    body = body + (255,)
    # нижняя часть кузова
    d.rectangle((0, 8, 15, 11), fill=body)
    d.rectangle((0, 11, 15, 11), fill=shade)
    # салон
    d.rectangle((3, cabin_top, 12, 7), fill=body)
    d.rectangle((4, cabin_top + 1, 11, cabin_top + cabin_h), fill=glass)
    d.line((7, cabin_top + 1, 7, cabin_top + cabin_h), fill=body)
    # фары
    d.point((0, 9), fill=(255, 240, 170, 255))
    d.point((15, 9), fill=(190, 20, 26, 255))
    # колёса
    for cx in (4, 11):
        d.ellipse((cx - wheel_r, 12 - wheel_r + 2, cx + wheel_r, 12 + wheel_r + 1), fill=dark)
        d.point((cx, 13), fill=(176, 178, 184, 255))
    path = os.path.join(ASSETS, "textures", "item", name)
    img.save(path)
    print("wrote", path)


def write_wav(path, samples, rate=44100):
    pcm = (np.clip(samples, -1, 1) * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(pcm.tobytes())


def engine_ogg():
    """Бесшовный 2-секундный цикл: низкие гармоники с амплитудной «пульсацией» цилиндров."""
    rate, dur = 44100, 2.0
    t = np.arange(int(rate * dur)) / rate
    base = 38.0                                            # кратно 0.5 Гц -> цикл сходится
    sig = np.zeros_like(t)
    for k, amp in enumerate([1.0, 0.7, 0.5, 0.35, 0.22, 0.14, 0.09], start=1):
        sig += amp * np.sin(2 * np.pi * base * k * t + k * 0.6)
    sig *= 0.72 + 0.28 * np.sin(2 * np.pi * 19.0 * t)      # 19 Гц -> 38 импульсов в секунду
    sig *= 0.9 + 0.1 * np.sin(2 * np.pi * 1.5 * t)
    sig = np.tanh(sig * 1.4) * 0.55                        # лёгкое насыщение — «звенит» как мотор
    wav_path = os.path.join(ASSETS, "sounds", "car", "engine.wav")
    ogg_path = os.path.join(ASSETS, "sounds", "car", "engine.ogg")
    write_wav(wav_path, sig, rate)
    for codec in (["-c:a", "libvorbis", "-q:a", "4"], ["-c:a", "vorbis", "-strict", "-2", "-b:a", "96k"]):
        r = subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, *codec, ogg_path])
        if r.returncode == 0:
            os.remove(wav_path)
            print("wrote", ogg_path)
            return
    print("WARNING: ffmpeg не смог закодировать ogg; лежит", wav_path, file=sys.stderr)


if __name__ == "__main__":
    os.makedirs(os.path.join(ASSETS, "textures", "entity"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "textures", "item"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "sounds", "car"), exist_ok=True)
    car_texture((142, 27, 42), "vaz2107.png")              # «вишнёвая» семёрка
    car_texture((79, 107, 58), "vaz_niva.png")             # зелёная «Нива»
    icon("vaz2107_spawn.png", (142, 27, 42), 5, 2, 2)
    icon("vaz_niva_spawn.png", (79, 107, 58), 4, 3, 2)
    engine_ogg()
