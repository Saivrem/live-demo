#!/bin/bash
set -e

UPLOADS_DIR="/var/www/html/uploads"

if [ -d "$UPLOADS_DIR" ]; then
    find "$UPLOADS_DIR" -type f ! -name ".htaccess" -exec rm -f {} +
fi