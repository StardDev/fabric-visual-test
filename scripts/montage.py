"""Joins screenshots into one grid image, ordered by the number in the file name.

Usage: python montage.py <folder> "<glob>" <output.png> [columns] [scale]
Example: python montage.py run/screenshots "check-*.png" out.png 4 0.35
Needs Pillow (pip install pillow).
"""
import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw


def number(path: Path) -> int:
    found = re.findall(r"\d+", path.stem)
    return int(found[-1]) if found else 0


def main() -> None:
    if len(sys.argv) < 4:
        print(__doc__)
        sys.exit(1)
    folder, pattern, output = Path(sys.argv[1]), sys.argv[2], Path(sys.argv[3])
    columns = int(sys.argv[4]) if len(sys.argv) > 4 else 4
    scale = float(sys.argv[5]) if len(sys.argv) > 5 else 0.35

    files = sorted(folder.glob(pattern), key=number)
    if not files:
        print(f"No files match {pattern} in {folder}")
        sys.exit(1)

    first = Image.open(files[0])
    width, height = int(first.width * scale), int(first.height * scale)
    rows = (len(files) + columns - 1) // columns
    grid = Image.new("RGB", (width * columns, height * rows))
    draw = ImageDraw.Draw(grid)
    for index, file in enumerate(files):
        x, y = (index % columns) * width, (index // columns) * height
        grid.paste(Image.open(file).convert("RGB").resize((width, height)), (x, y))
        draw.rectangle((x, y, x + 8 * len(file.stem) + 6, y + 14), fill=(0, 0, 0))
        draw.text((x + 3, y + 1), file.stem, fill=(255, 255, 0))
    grid.save(output)
    print(f"{len(files)} screenshots -> {output} ({columns} columns, labels = file names)")


if __name__ == "__main__":
    main()
