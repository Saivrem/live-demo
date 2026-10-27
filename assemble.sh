#!/bin/bash
set -euo pipefail

project_directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
recreate_containers=false
run_tests=false
initial_start=false

usage() {
    cat <<'USAGE'
Usage: assemble.sh [-c] [-t] [--initial-start]

Build images and start the Compose stack.
  -c, --recreate  Stop existing containers before starting; preserve all volumes.
  -t, --test      Run Java tests in Docker before starting the stack.
  --initial-start Import reference data from Icecat at application startup.
  -h, --help      Show this help.
USAGE
}

for argument in "$@"; do
    case "$argument" in
        -c|--recreate) recreate_containers=true ;;
        -t|--test) run_tests=true ;;
        --initial-start) initial_start=true ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown argument: $argument" >&2; usage >&2; exit 2 ;;
    esac
done

cd -- "$project_directory"
command -v docker >/dev/null || { echo "Docker is required." >&2; exit 1; }
docker compose version >/dev/null
docker info >/dev/null

if [[ ! -f .env ]]; then
    echo "Missing .env. Run bash env_setup.sh and set your Icecat credentials first." >&2
    exit 1
fi

docker compose config --quiet

if [[ "$run_tests" == true ]]; then
    test_image="icedata-v2-java-tests:local"
    docker build --target test -f DockerfileJava -t "$test_image" .
    docker run --rm \
        --add-host host.docker.internal:host-gateway \
        -e DOCKER_HOST=unix:///var/run/docker.sock \
        -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
        -e TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
        -v /var/run/docker.sock:/var/run/docker.sock \
        "$test_image"
fi

# Build before stopping the running stack so a build failure leaves it available.
docker compose build
if [[ "$recreate_containers" == true ]]; then
    docker compose down
fi

INITIAL_START="$initial_start" docker compose up -d --wait --wait-timeout 180
