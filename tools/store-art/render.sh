#!/bin/sh
# Renders store art with headless Chrome: sh tools/store-art/render.sh <in.html|svg> <out.png> <w> <h>
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
"$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 --window-size="$3,$4" --default-background-color=00000000 --screenshot="$2" "file://$(cd "$(dirname "$1")" && pwd)/$(basename "$1")" 2>/dev/null
