#!/usr/bin/env bash
#
# Turn each per-news card of an edition (card-N-subject.html, produced by the
# 'render' stage) into a PNG sized for LinkedIn — one image per news + its
# conversation, ready to upload manually. Output goes to releases/<date>/linkedin/.
#
# Usage:
#   scripts/linkedin-images.sh [YYYY-MM-DD]      # defaults to today's edition
#
set -euo pipefail

cd "$(dirname "$0")/.."
DATE="${1:-$(date +%F)}"
DIR="releases/$DATE"
[ -d "$DIR" ] || { echo "No edition at $DIR — run the pipeline first."; exit 1; }

shopt -s nullglob
cards=("$DIR"/card-*.html)
[ ${#cards[@]} -gt 0 ] || { echo "No card-*.html in $DIR — run the 'render' stage first."; exit 1; }

chrome="$(command -v google-chrome || command -v chromium || command -v chromium-browser || true)"
[ -n "$chrome" ] || { echo "Need google-chrome or chromium installed."; exit 1; }
command -v python3 >/dev/null || { echo "Need python3 (with Pillow) to trim images."; exit 1; }

OUT="$DIR/linkedin"
mkdir -p "$OUT"

for card in "${cards[@]}"; do
    name="$(basename "${card%.html}")"
    png="$OUT/$name.png"
    "$chrome" --headless=new --no-sandbox --disable-gpu --hide-scrollbars \
        --force-device-scale-factor=1 --window-size=1080,3000 \
        --screenshot="$png" "file://$(pwd)/$card" >/dev/null 2>&1
    # Crop the trailing background so each image hugs its content height.
    python3 - "$png" <<'PY'
import sys
from PIL import Image, ImageChops
p = sys.argv[1]
im = Image.open(p).convert("RGB")
bg = Image.new("RGB", im.size, im.getpixel((0, 0)))
box = ImageChops.difference(im, bg).getbbox()
if box:
    im.crop((0, 0, im.width, min(im.height, box[3] + 40))).save(p)
PY
    echo "  ✓ $png"
done

echo "Done — ${#cards[@]} LinkedIn image(s) in $OUT"
