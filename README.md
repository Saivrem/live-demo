# Live Demo

## Icedata V2

### Introduction
This project if a full representation of live-demo project. Codebase is split between several modules, including launchpad for web, icedata-v2 for java application. All projects could be assembled and run with docker compose file provided.

### Prerequisites

#### Common Setup:
- All components of this project share the same `.env` file, which is **not included** for security reasons. The project expects specific environment variables to be set. Below is a reference script that can be used to set up these values:

```bash

#!/bin/bash

########################################
#          Environment Setup           #
########################################

# ---------- Database Configuration ----------
export DB_USER="db_user"                            # Username for the database
export DB_PASS="superSecretPass123"                 # Password for the database user
export DB_SCHEMA="shop_db"                          # Database schema (PostgreSQL) or database name (MySQL)
export DB_PORT="5432"                               # Port number for the database connection (default for PostgreSQL is 5432)
export DB_HOST="localhost"                          # Host address of the database
export DB_PARAMS="?sslmode=disable&timezone=UTC"    # Connection parameters (e.g., SSL mode, timezone)
export FLYWAY_USER='flyway'                         # User for Flyway migration scripts
export FLYWAY_PASSWORD='flyway'                     # Password for Flyway user

# ---------- MySQL Configuration ----------
export MYSQL_ROOT_PASSWORD="rootPass456"            # Root password for MySQL, needed during initial setup

# ---------- Icecat Shop User Configuration ----------
export ICECAT_USER="shop_user"                      # Username for the Icecat shop service
export ICECAT_PASS="shopUserPass789"                # Password for the Icecat shop service

# ---------- Java Application Configuration ----------
export JAVA_PORT="8080"                             # Port used by the Java Icedata service
export JAVA_HOST="127.0.0.1"                        # Host address where the Icedata service is running

########################################
#         End of Configuration         #
########################################      
```

#### Database Requirements:
- The target machine should have a **MySQL server** installed and properly configured.

#### Web Server Requirements:
- The target machine should be capable of running a web server with the Launchpad project preinstalled. The web-related files can be found in the `launchpad` package.

#### Backend Setup:
- The backend is located in the `icedata-v2` package and relies on the environment variables specified in the common setup section above.

### Local Development

For local development, a `docker-compose` file is provided. The application can be started as a full system or by individual services.

#### Running MySQL Server Only:
To start only the MySQL service:
```bash

docker compose up -d database
```

#### Full System Run:
To build and run the entire system, use the following script:
```bash

bash assemble.sh
```
This script will:
1. Remove old artifacts.
2. Build the new one.
3. Execute the `docker-compose` file.

#### Full Cleanup:
To clean up all Docker containers and networks, use:
```bash

bash docker-cleanup.sh
```
