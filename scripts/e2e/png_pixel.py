#!/usr/bin/env python3
"""Prints the colour of one pixel of a PNG as "R G B", using only the standard library.

Usage: png_pixel.py <file.png> <x> <y>
x and y may be fractions of the width and height, e.g. 0.01 0.6.
Handles what `adb exec-out screencap -p` writes: 8-bit RGB or RGBA, not interlaced.
"""
import struct
import sys
import zlib


def read_png(path):
    with open(path, "rb") as f:
        data = f.read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        sys.exit(f"{path} is not a PNG")
    pos, idat = 8, b""
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        if kind == b"IHDR":
            width, height, depth, colour, _, _, interlace = struct.unpack(">IIBBBBB", body)
        elif kind == b"IDAT":
            idat += body
        pos += 12 + length
    if depth != 8 or colour not in (2, 6) or interlace:
        sys.exit(f"Unsupported PNG: depth {depth}, colour type {colour}, interlace {interlace}")
    return width, height, 3 if colour == 2 else 4, zlib.decompress(idat)


def row(raw, width, channels, y, previous):
    """Undoes the PNG filter of row y, given the already-decoded row above it."""
    stride = width * channels
    start = y * (stride + 1)
    kind, line = raw[start], bytearray(raw[start + 1:start + 1 + stride])
    for i in range(stride):
        left = line[i - channels] if i >= channels else 0
        up = previous[i]
        up_left = previous[i - channels] if i >= channels else 0
        if kind == 1:
            line[i] = (line[i] + left) & 0xFF
        elif kind == 2:
            line[i] = (line[i] + up) & 0xFF
        elif kind == 3:
            line[i] = (line[i] + (left + up) // 2) & 0xFF
        elif kind == 4:
            p = left + up - up_left
            pa, pb, pc = abs(p - left), abs(p - up), abs(p - up_left)
            predictor = left if pa <= pb and pa <= pc else up if pb <= pc else up_left
            line[i] = (line[i] + predictor) & 0xFF
    return line


def main():
    path, x, y = sys.argv[1], float(sys.argv[2]), float(sys.argv[3])
    width, height, channels, raw = read_png(path)
    x = int(x * width) if x < 1 else int(x)
    y = int(y * height) if y < 1 else int(y)
    previous = bytearray(width * channels)
    for r in range(y + 1):
        previous = row(raw, width, channels, r, previous)
    print(*previous[x * channels:x * channels + 3])


if __name__ == "__main__":
    main()
