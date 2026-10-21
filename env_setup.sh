#!/bin/bash
set -euo pipefail

project_directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
environment_file="$project_directory/.env"
example_file="$project_directory/.env.example"

usage() {
    cat <<'USAGE'
Usage: bash env_setup.sh

Creates .env next to this script using .env.example.
MySQL and Flyway passwords are generated automatically.
After creation, fill in ICECAT_USER and ICECAT_PASS in .env.
An existing .env is never overwritten.

  -h, --help  Show this help.
USAGE
}

if [[ $# -gt 0 ]]; then
    case "$1" in
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown argument: $1. Overwriting .env with --force is disabled." >&2; usage >&2; exit 2 ;;
    esac
fi

if [[ -e "$environment_file" || -L "$environment_file" ]]; then
    echo "File $environment_file already exists and has been left unchanged."
    echo "Open it in an editor and check ICECAT_USER and ICECAT_PASS."
    echo "Do not regenerate passwords for an existing database."
    exit 0
fi

if [[ ! -f "$example_file" ]]; then
    echo "File $example_file not found. Copy the complete project, including hidden files." >&2
    exit 1
fi
command -v openssl >/dev/null || { echo "openssl is required. On Ubuntu: sudo apt install openssl" >&2; exit 1; }

generated_db_password=$(openssl rand -hex 24)
generated_flyway_password=$(openssl rand -hex 24)
generated_root_password=$(openssl rand -hex 24)

umask 077
temporary_file=$(mktemp "$project_directory/.env.tmp.XXXXXX")
trap 'rm -f -- "$temporary_file"' EXIT
sed \
    -e "s/__GENERATE_DB_PASSWORD__/$generated_db_password/g" \
    -e "s/__GENERATE_FLYWAY_PASSWORD__/$generated_flyway_password/g" \
    -e "s/__GENERATE_ROOT_PASSWORD__/$generated_root_password/g" \
    "$example_file" > "$temporary_file"

# Publish the complete file without overwriting a concurrently created .env.
ln -- "$temporary_file" "$environment_file"
echo "Created $environment_file (owner-only permissions: 600)."
echo "Next steps:"
printf '  1. Open the file: nano "%s"\n' "$environment_file"
echo "  2. Replace YOUR_ICECAT_LOGIN and YOUR_ICECAT_PASSWORD with your credentials."
echo "  3. Save: Ctrl+O, Enter. Exit: Ctrl+X."
printf '  4. Run: bash "%s/assemble.sh"\n' "$project_directory"
