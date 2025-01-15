#!/bin/bash

########################################
#        Generate .env File            #
########################################

ENV_FILE=".env"

cat << EOF > $ENV_FILE
# ---------- Database Configuration ----------
DB_USER="db_user"                            # Username for the database
DB_PASS="db_pass"                            # Password for the database user
DB_SCHEMA="db_name"                          # Database schema (PostgreSQL) or database name (MySQL)
DB_PORT="5432"                               # Port number for the database connection (default for PostgreSQL is 5432)
DB_HOST="localhost"                          # Host address of the database
DB_PARAMS="?sslmode=disable&timezone=UTC"    # Connection parameters (e.g., SSL mode, timezone)
FLYWAY_USER="flyway"                         # User for Flyway migration scripts
FLYWAY_PASSWORD="flyway"                     # Password for Flyway user

# ---------- MySQL Configuration -----------
MYSQL_ROOT_PASSWORD="root_pass"              # Root password for MySQL, needed during initial setup

# ---------- Icecat Configuration ----------
ICECAT_USER="shop_user"                      # Username for the Icecat shop service
ICECAT_PASS="shop_user_pass"                 # Password for the Icecat shop service
ICECAT_BASE="https://data.icecat.biz/export/"# Base path to data repo
ICECAT_REPO="freexml/"                       # Or level4 or whatever
LANG_LIST="refs/LanguageList.xml.gz"         # Language list file
SUPPLIERS_LIST="refs/SuppliersList.xml.gz"   # Suppliers list file
SUPPLIER_MAPPING="EN/supplier_mapping.xml"   # EN is hardcoded, think about it

# ---------- Java Configuration ------------
JAVA_PORT="8080"                             # Port used by the Java Icedata service
JAVA_HOST="127.0.0.1"                        # Host address where the Icedata service is running
EOF

echo ".env file has been successfully generated at $ENV_FILE"
