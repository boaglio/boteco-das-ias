#!/usr/bin/env bash
#
# Boteco das IAs — single entry point for every operation, so you never run
# java (or remember stage names) by hand.
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

usage() { sed -n '2,/^set -euo/p' "$0" | sed 's/^# \{0,1\}//; /^set -euo/d'; }

cmd="${1:-help}"
[ $# -gt 0 ] && shift || true

case "$cmd" in
    build)                                   mvn package ;;
    gather|translate|collect|illustrate|render)  stages "$cmd" "$@" ;;
    all)                                     stages gather translate collect illustrate render ;;
    opine)                                   scripts/input-opinion.sh "$@" ;;
    images)                                  scripts/linkedin-images.sh "$@" ;;
    publish)                                 scripts/publish.sh ;;
    weekly)
        stages gather translate collect illustrate render
        scripts/input-opinion.sh "$@"
        scripts/publish.sh
        ;;
    help|-h|--help)                          usage ;;
    *) echo "Unknown command: $cmd" >&2; usage; exit 1 ;;
esac
