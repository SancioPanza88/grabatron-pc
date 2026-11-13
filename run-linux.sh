#!/usr/bin/env bash
# Avvia il gioco su Linux (sviluppo/prove) usando le librerie native Linux.
set -euo pipefail
cd "$(dirname "$0")"
[ -f out/Grabatron/game.jar ] || ./build.sh
java -Dgrab.data=data -cp "out/Grabatron/game.jar:$(ls lib/*.jar | tr '\n' ':')" desktop.Main "$@"
