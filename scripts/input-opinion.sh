#!/usr/bin/env bash
#
# Capture *your* (the human "Eu") opinion for each news item, interactively,
# and write it into the edition's magazine.json — aligned first in each
# conversation, exactly as the layout expects.
#
# Run it after the AI opinions are collected and before rendering:
#   gather -> translate -> collect (AI)  ->  scripts/input-opinion.sh  ->  render
#
# Usage:
#   scripts/input-opinion.sh [YYYY-MM-DD]      # defaults to today's edition
#
set -euo pipefail

cd "$(dirname "$0")/.."
RELEASES_DIR="releases"
DATE="${1:-$(date +%F)}"
JSON="$RELEASES_DIR/$DATE/magazine.json"

command -v jq >/dev/null || { echo "This script needs 'jq' installed."; exit 1; }
[ -f "$JSON" ] || { echo "No edition found at $JSON — run the 'gather' stage first."; exit 1; }

count=$(jq '.news | length' "$JSON")
echo "Boteco das IAs — sua opinião para $DATE ($count notícia(s))"
echo "Digite sua opinião e Enter. Deixe em branco para pular."
echo

work=$(mktemp)
cp "$JSON" "$work"

for i in $(seq 0 $((count - 1))); do
    subject=$(jq -r ".news[$i].subject" "$work")
    title=$(jq -r ".news[$i].titlePt // .news[$i].title" "$work")
    url=$(jq -r ".news[$i].url" "$work")
    summary=$(jq -r ".news[$i].summaryPt // .news[$i].summary // \"\"" "$work")

    echo "────────────────────────────────────────────────────────"
    echo "[$subject] $title"
    echo "$url"
    [ -n "$summary" ] && echo "$summary" | fold -s -w 72
    printf "Sua opinião> "
    IFS= read -r opinion </dev/tty || opinion=""

    if [ -n "$opinion" ]; then
        # Drop any previous human take, then put the new one first in the list.
        jq --arg t "$opinion" \
           ".news[$i].opinions |= ([{reviewer: \"HUMAN\", text: \$t}] + map(select(.reviewer != \"HUMAN\")))" \
           "$work" >"$work.tmp" && mv "$work.tmp" "$work"
        echo "  ✓ salva"
    else
        echo "  – pulada"
    fi
    echo
done

mv "$work" "$JSON"
echo "Pronto. Opiniões gravadas em $JSON"
echo

printf "Re-gerar o HTML e as imagens do LinkedIn agora? [s/N] "
IFS= read -r answer </dev/tty || answer=""
case "$answer" in
    [sSyY]*)
        jar=$(ls target/boteco-das-ias-*.jar 2>/dev/null | head -1)
        if [ -z "$jar" ]; then
            echo "Jar não encontrado — rode 'mvn package' primeiro."
            exit 1
        fi
        echo "Renderizando…"
        java -jar "$jar" render </dev/null
        "$(dirname "$0")/linkedin-images.sh" "$DATE"
        ;;
    *)
        echo "Ok. Quando quiser:  java -jar target/boteco-das-ias-*.jar render"
        echo "                    scripts/linkedin-images.sh $DATE"
        ;;
esac
