#!/usr/bin/env bash
#
# Boteco das IAs — single entry point for every operation, so you never run
# java (or remember stage names, or hunt for another script) by hand.
#
#   scripts/boteco.sh <command> [args]
#
# Commands:
#   build                 compile the jar (mvn package, runs tests)
#   gather                stage 1 — pick the best news per subject
#   translate             stage 2 — translate to Brazilian Portuguese
#   collect               stage 3 — collect the AI opinions
#   illustrate            stage 4 — generate the anime images
#   render                stage 5 — write magazine.html + LinkedIn cards
#   all                   run stages 1..5 in order
#   opine [date]          type your own opinion (offers to re-render + images)
#   images [date]         generate the LinkedIn images for an edition
#   publish               refresh the Pages landing page + README list
#   weekly                all → opine → publish  (the full edition flow)
#   help                  show this help
#
# Env:
#   BOTECO_ARGS   extra Spring flags, e.g. BOTECO_ARGS="--boteco.comfyui.steps=14"
#
set -euo pipefail
cd "$(dirname "$0")/.."

# ---------------------------------------------------------------------------
# Shared helpers
# ---------------------------------------------------------------------------

# Files registered here are removed on exit, success or failure alike.
TMP_FILES=()
cleanup() {
    local f
    for f in "${TMP_FILES[@]+"${TMP_FILES[@]}"}"; do
        rm -f "$f"
    done
}
trap cleanup EXIT
register_tmp() { TMP_FILES+=("$1"); }

# Path to the runnable jar, building it first if needed.
jar() {
    local j
    j=$(ls target/boteco-das-ias-*.jar 2>/dev/null | head -1 || true)
    if [ -z "$j" ]; then
        echo "No jar found — building…" >&2
        mvn -q package -DskipTests >&2
        j=$(ls target/boteco-das-ias-*.jar | head -1)
    fi
    printf '%s' "$j"
}

# Run one or more pipeline stages. stdin is /dev/null so the build never blocks
# waiting for the human reviewer — your opinion is added via the 'opine' command.
stages() {
    java -jar "$(jar)" ${BOTECO_ARGS:-} "$@" </dev/null
}

# Run the 'gather' stage specifically. Unlike the other stages, gather prompts
# you interactively to pick one of the top-3 candidates per subject — so it
# needs a real terminal on stdin instead of /dev/null, or that prompt would
# hit EOF instantly and silently default to the top-ranked candidate every
# time, with no chance to actually choose.
stages_gather() {
    : < /dev/tty 2>/dev/null || { echo "'gather' needs an interactive terminal (to prompt for your top-3 pick) — run it directly in a shell, not via cron/pipe/subagent." >&2; exit 1; }
    java -jar "$(jar)" ${BOTECO_ARGS:-} gather "$@" </dev/tty
}

usage() { sed -n '2,/^set -euo/p' "$0" | sed 's/^# \{0,1\}//; /^set -euo/d'; }

# ---------------------------------------------------------------------------
# opine — capture *your* opinion for each news item, interactively, and write
# it into the edition's magazine.json — first in each conversation, exactly
# as the layout expects. Run it after 'collect' and before 'render'.
# ---------------------------------------------------------------------------
opine() {
    local date="${1:-$(date +%F)}"
    local json="releases/$date/magazine.json"
    local tmp="$json.tmp"

    command -v jq >/dev/null || { echo "'opine' needs 'jq' installed." >&2; exit 1; }
    [ -f "$json" ] || { echo "No edition found at $json — run 'gather' first." >&2; exit 1; }
    register_tmp "$tmp"

    local count
    count=$(jq '.news | length' "$json")
    echo "Boteco das IAs — sua opinião para $date ($count notícia(s))"
    echo "Digite sua opinião e Enter. Deixe em branco para pular."
    echo

    local i subject title url summary opinion answer
    for i in $(seq 0 $((count - 1))); do
        subject=$(jq -r ".news[$i].subject" "$json")
        title=$(jq -r ".news[$i].titlePt // .news[$i].title" "$json")
        url=$(jq -r ".news[$i].url" "$json")
        summary=$(jq -r ".news[$i].summaryPt // .news[$i].summary // \"\"" "$json")

        echo "────────────────────────────────────────────────────────"
        echo "[$subject] $title"
        echo "$url"
        [ -n "$summary" ] && echo "$summary" | fold -s -w 72
        printf "Sua opinião> "
        IFS= read -r opinion </dev/tty || opinion=""

        if [ -n "$opinion" ]; then
            # Written straight to the edition file (atomically, via the tmp +
            # rename) after every item, so an interrupted run never loses an
            # opinion already typed — unlike a single commit at the very end.
            jq --arg t "$opinion" \
               ".news[$i].opinions |= ([{reviewer: \"HUMAN\", text: \$t}] + map(select(.reviewer != \"HUMAN\")))" \
               "$json" >"$tmp" && mv "$tmp" "$json"
            echo "  ✓ salva"
        else
            echo "  – pulada"
        fi
        echo
    done

    echo "Pronto. Opiniões gravadas em $json"
    echo

    printf "Re-gerar o HTML e as imagens do LinkedIn agora? [s/N] "
    IFS= read -r answer </dev/tty || answer=""
    case "$answer" in
        [sSyY]*)
            echo "Renderizando…"
            stages render
            images "$date"
            ;;
        *)
            echo "Ok. Quando quiser:  scripts/boteco.sh render"
            echo "                    scripts/boteco.sh images $date"
            ;;
    esac
}

# ---------------------------------------------------------------------------
# images — turn each per-news card of an edition (card-N-subject.html,
# produced by 'render') into a PNG sized for LinkedIn — one image per news +
# its conversation, ready to upload manually. Written to releases/<date>/linkedin/.
# ---------------------------------------------------------------------------

# Crops the trailing background off a screenshot so it hugs its content
# height. Run as a separate function (not inline) so its failure can be
# checked with `if`, instead of tripping `set -e` and aborting the whole run.
trim_image() {
    python3 - "$1" <<'PY'
import sys
from PIL import Image, ImageChops
p = sys.argv[1]
im = Image.open(p).convert("RGB")
bg = Image.new("RGB", im.size, im.getpixel((0, 0)))
box = ImageChops.difference(im, bg).getbbox()
if box:
    im.crop((0, 0, im.width, min(im.height, box[3] + 40))).save(p)
PY
}

images() {
    local date="${1:-$(date +%F)}"
    local dir="releases/$date"
    [ -d "$dir" ] || { echo "No edition at $dir — run the pipeline first." >&2; exit 1; }

    shopt -s nullglob
    local cards=("$dir"/card-*.html)
    shopt -u nullglob
    [ ${#cards[@]} -gt 0 ] || { echo "No card-*.html in $dir — run the 'render' stage first." >&2; exit 1; }

    local chrome
    chrome="$(command -v google-chrome || command -v chromium || command -v chromium-browser || true)"
    [ -n "$chrome" ] || { echo "Need google-chrome or chromium installed." >&2; exit 1; }
    command -v python3 >/dev/null || { echo "Need python3 (with Pillow) to trim images." >&2; exit 1; }
    python3 -c "import PIL" >/dev/null 2>&1 || { echo "Need Pillow: pip install Pillow" >&2; exit 1; }

    local out="$dir/linkedin"
    mkdir -p "$out"

    local card name png shot_err failures=0
    for card in "${cards[@]}"; do
        name="$(basename "${card%.html}")"
        png="$out/$name.png"

        if ! shot_err=$("$chrome" --headless=new --no-sandbox --disable-gpu --hide-scrollbars \
                --force-device-scale-factor=1 --window-size=1080,3000 \
                --screenshot="$png" "file://$(pwd)/$card" 2>&1); then
            echo "  ✗ $png — chrome failed:" >&2
            echo "$shot_err" | sed 's/^/      /' >&2
            failures=$((failures + 1))
            continue
        fi
        if [ ! -s "$png" ]; then
            echo "  ✗ $png — chrome produced an empty file" >&2
            failures=$((failures + 1))
            continue
        fi
        if ! trim_image "$png"; then
            echo "  ✗ $png — failed to trim the screenshot" >&2
            failures=$((failures + 1))
            continue
        fi
        echo "  ✓ $png"
    done

    echo "Done — $((${#cards[@]} - failures))/${#cards[@]} LinkedIn image(s) in $out"
    [ "$failures" -eq 0 ] || exit 1
}

# ---------------------------------------------------------------------------
# publish — build the GitHub Pages landing page (index.html) and refresh the
# README's "Edições" list from whatever editions exist under releases/.
# ---------------------------------------------------------------------------
publish() {
    command -v jq >/dev/null || { echo "'publish' needs 'jq' installed." >&2; exit 1; }
    command -v python3 >/dev/null || { echo "'publish' needs 'python3' installed." >&2; exit 1; }

    local base_url="https://boaglio.github.io/boteco-das-ias"

    # Collect editions (dirs named YYYY-MM-DD that have a magazine.html), newest first.
    local editions=() d
    for d in $(ls -1 releases 2>/dev/null | sort -r); do
        [ -f "releases/$d/magazine.html" ] && editions+=("$d")
    done

    # A single edition's magazine.json failing to parse must not take the
    # rest of the (perfectly fine) editions down with it under set -e.
    edition_title() {
        jq -r '.title // "Boteco das IAs"' "releases/$1/magazine.json" 2>/dev/null \
            || { echo "  ! releases/$1/magazine.json is unreadable — using a placeholder title" >&2
                 echo "Boteco das IAs"; }
    }

    # --- index.html (Pages landing) ------------------------------------------------
    {
    cat <<'HTML'
<!DOCTYPE html>
<html lang="pt-br">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Boteco das IAs</title>
<style>
:root{--bg:#faf7f2;--ink:#2b2b2b;--accent:#c0392b}
body{margin:0;background:var(--bg);color:var(--ink);font-family:system-ui,sans-serif;line-height:1.5}
header{text-align:center;padding:2.5rem 1rem 1.5rem;border-bottom:3px solid var(--accent)}
header img{width:160px;border-radius:16px}
header h1{margin:.6rem 0 .2rem;font-size:2rem}
header p{margin:0;color:#777}
main{max-width:640px;margin:0 auto;padding:1.5rem 1rem}
ul{list-style:none;margin:0;padding:0}
li{padding:.9rem 1rem;border:1px solid #e2dccf;border-radius:12px;background:#fff;margin-bottom:.7rem}
li a.edition{font-weight:700;color:var(--ink);text-decoration:none;font-size:1.1rem}
li a.edition:hover{color:var(--accent)}
.links{margin-top:.3rem;font-size:.85rem}
.links a{color:var(--accent);text-decoration:none;margin-right:.8rem}
footer{text-align:center;color:#999;font-size:.8rem;padding:2rem 1rem}
</style>
</head>
<body>
<header>
<img src="assets/logo.png" alt="Boteco das IAs">
<h1>Boteco das IAs</h1>
<p>Boletim semanal de notícias de tecnologia, comentadas por IAs.</p>
</header>
<main>
<ul>
HTML

    if [ ${#editions[@]} -eq 0 ]; then
        echo '<li>Nenhuma edição ainda.</li>'
    else
        local title pdf li
        for d in "${editions[@]}"; do
            title=$(edition_title "$d")
            pdf=""; [ -f "releases/$d/magazine.pdf" ] && pdf="<a href=\"releases/$d/magazine.pdf\">PDF</a>"
            li=""; [ -d "releases/$d/linkedin" ] && li="<a href=\"releases/$d/linkedin/\">LinkedIn</a>"
            printf '<li><a class="edition" href="releases/%s/magazine.html">%s</a><div class="links">%s %s</div></li>\n' \
                "$d" "$title" "$pdf" "$li"
        done
    fi

    cat <<'HTML'
</ul>
</main>
<footer>github.com/boaglio/boteco-das-ias</footer>
</body>
</html>
HTML
    } > index.html
    echo "Wrote index.html (${#editions[@]} edition(s))"

    # --- README "Edições" section --------------------------------------------------
    local md
    md=$(mktemp)
    register_tmp "$md"
    {
        echo "<!-- EDITIONS:START (generated by scripts/boteco.sh publish) -->"
        if [ ${#editions[@]} -eq 0 ]; then
            echo "_Nenhuma edição publicada ainda._"
        else
            local title
            for d in "${editions[@]}"; do
                title=$(edition_title "$d")
                echo "- **$d** — [$title]($base_url/releases/$d/magazine.html)"
            done
        fi
        echo "<!-- EDITIONS:END -->"
    } > "$md"

    # Replace the marked block in README.md. Fails loudly (instead of silently
    # leaving README.md untouched) if the markers aren't found there anymore.
    python3 - "$md" <<'PY'
import re, sys, pathlib
block = pathlib.Path(sys.argv[1]).read_text()
readme = pathlib.Path("README.md")
text = readme.read_text()
new, count = re.subn(r"<!-- EDITIONS:START.*?EDITIONS:END -->", block.strip(), text, flags=re.S)
if count != 1:
    print("EDITIONS:START/END markers not found in README.md — nothing updated.", file=sys.stderr)
    sys.exit(1)
readme.write_text(new)
PY
    echo "Updated README.md editions list"
}

# ---------------------------------------------------------------------------
# Dispatch
# ---------------------------------------------------------------------------
cmd="${1:-help}"
[ $# -gt 0 ] && shift || true

case "$cmd" in
    build)                                   mvn package ;;
    gather)                                  stages_gather "$@" ;;
    translate|collect|illustrate|render)     stages "$cmd" "$@" ;;
    all)
        stages_gather
        stages translate collect illustrate render
        ;;
    opine)                                   opine "$@" ;;
    images)                                  images "$@" ;;
    publish)                                 publish ;;
    weekly)
        stages_gather
        stages translate collect illustrate render
        opine "$@"
        publish
        ;;
    help|-h|--help)                          usage ;;
    *) echo "Unknown command: $cmd" >&2; usage; exit 1 ;;
esac
