#!/bin/bash

# TODO think about doing `cat` instead of exports, or do both, probably this script should generate .env dummy file

########################################
#          Environment Setup           #
########################################

# ---------- Database Configuration ----------
export DB_USER="db_user"                            # Username for the database
export DB_PASS="db_pass"                            # Password for the database user
export DB_SCHEMA="db_name"                          # Database schema (PostgreSQL) or database name (MySQL)
export DB_PORT="5432"                               # Port number for the database connection (default for PostgreSQL is 5432)
export DB_HOST="localhost"                          # Host address of the database
export DB_PARAMS="?sslmode=disable&timezone=UTC"    # Connection parameters (e.g., SSL mode, timezone)
export FLYWAY_USER='flyway'                         # User for Flyway migration scripts
export FLYWAY_PASSWORD='flyway'                     # Password for Flyway user

# ---------- MySQL Configuration -----------
export MYSQL_ROOT_PASSWORD="root_pass"              # Root password for MySQL, needed during initial setup

# ---------- Icecat Configuration ----------
export ICECAT_USER="shop_user"                      # Username for the Icecat shop service
export ICECAT_PASS="shop_user_pass"                 # Password for the Icecat shop service
export ICECAT_BASE="https://data.icecat.biz/export/"# Base path to data repo
export ICECAT_REPO="freexml/"                       # Or level4 or whatever
export LANG_LIST="refs/LanguageList.xml.gz"         # Language list file
export SUPPLIERS_LIST="refs/SuppliersList.xml.gz"   # Suppliers list file
export SUPPLIER_MAPPING="EN/supplier_mapping.xml"   # EN is hardcoded, think about it

# ---------- Java Configuration ------------
export JAVA_PORT="8080"                             # Port used by the Java Icedata service
export JAVA_HOST="127.0.0.1"                        # Host address where the Icedata service is running

########################################
#         End of Configuration         #
########################################