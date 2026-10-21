#!/bin/bash
set -euo pipefail

########################################
#        Generate .env File            #
########################################

ENV_FILE=".env"

FORCE=false
for arg in "$@"; do
  if [[ "$arg" == "-f" || "$arg" == "--force" ]]; then
    FORCE=true
  fi
done

if [ -f "$ENV_FILE" ] && [ "$FORCE" = false ]; then
  echo "$ENV_FILE already exists. Use -f or --force to overwrite."
  exit 1
fi

command -v openssl >/dev/null || { echo "openssl is required to generate database passwords" >&2; exit 1; }
generated_db_password=$(openssl rand -hex 24)
generated_flyway_password=$(openssl rand -hex 24)
generated_root_password=$(openssl rand -hex 24)
umask 077
cat << EOF > "$ENV_FILE"
# ---------- Database Configuration ----------
DB_USER="db_user"                            # Username for the database
DB_PASS="$generated_db_password"                            # Password for the database user
DB_SCHEMA="db_name"                          # MySQL database name
DB_PORT="3306"                               # MySQL port
DB_HOST="database"                          # Host address of the database
DB_PARAMS="serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8"    # Connection parameters (e.g., SSL mode, timezone)
FLYWAY_USER="flyway"                         # User for Flyway migration scripts
FLYWAY_PASSWORD="$generated_flyway_password"                     # Password for Flyway user

# ---------- MySQL Configuration -----------
MYSQL_ROOT_PASSWORD="$generated_root_password"              # Root password for MySQL, needed during initial setup

# ---------- Icecat Configuration ----------
ICECAT_USER="shop_user"                      # Username for the Icecat shop service
ICECAT_PASS="shop_user_pass"                 # Password for the Icecat shop service
ICECAT_BASE="https://data.icecat.biz/export/" # Base path to data repo
ICECAT_REPO="freexml/"                       # Or level4 or whatever
LANG_LIST="refs/LanguageList.xml.gz"         # Language list file
SUPPLIERS_LIST="refs/SuppliersList.xml.gz"   # Suppliers list file
SUPPLIER_MAPPING="EN/supplier_mapping.xml"   # EN is hardcoded, think about it

# ---------- Java Configuration ------------
JAVA_PORT="8086"                             # Port used by the Java Icedata service
JAVA_HOST="http://java"                        # Host address where the Icedata service is running
EOF

chmod 600 "$ENV_FILE"
echo ".env file has been successfully generated at $ENV_FILE"
