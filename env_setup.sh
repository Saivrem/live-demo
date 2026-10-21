#!/bin/bash
set -euo pipefail

project_directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
environment_file="$project_directory/.env"
example_file="$project_directory/.env.example"

usage() {
    cat <<'USAGE'
Использование: bash env_setup.sh

Создаёт .env рядом со скриптом на основе .env.example.
Пароли MySQL и Flyway генерируются автоматически.
После создания заполни ICECAT_USER и ICECAT_PASS в .env.
Существующий .env никогда не перезаписывается.

  -h, --help  Показать эту справку.
USAGE
}

if [[ $# -gt 0 ]]; then
    case "$1" in
        -h|--help) usage; exit 0 ;;
        *) echo "Неизвестный аргумент: $1. Перезапись .env через --force отключена." >&2; usage >&2; exit 2 ;;
    esac
fi

if [[ -e "$environment_file" || -L "$environment_file" ]]; then
    echo "Файл $environment_file уже существует. Он оставлен без изменений."
    echo "Открой его в редакторе и проверь ICECAT_USER и ICECAT_PASS."
    echo "Не генерируй новые пароли для уже созданной базы данных."
    exit 0
fi

if [[ ! -f "$example_file" ]]; then
    echo "Не найден $example_file. Скопируй проект целиком, включая скрытые файлы." >&2
    exit 1
fi
command -v openssl >/dev/null || { echo "Нужен openssl. На Ubuntu: sudo apt install openssl" >&2; exit 1; }

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
echo "Создан $environment_file (доступ только владельцу: 600)."
echo "Дальше:"
printf '  1. Открой файл: nano "%s"\n' "$environment_file"
echo "  2. Замени YOUR_ICECAT_LOGIN и YOUR_ICECAT_PASSWORD своими данными."
echo "  3. Сохрани: Ctrl+O, Enter. Выйди: Ctrl+X."
printf '  4. Запусти: bash "%s/assemble.sh"\n' "$project_directory"
