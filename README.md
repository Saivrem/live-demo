# Live Demo

## Icedata V2

### Introduction
This project if a full representation of live-demo project. Codebase is split between several modules, including launchpad for web, icedata-v2 for java application. All projects could be assembled and run with docker compose file provided.

### Prerequisites

#### Common Setup:
- All components of this project share the same `.env` file, which is **not included** for security reasons. The project expects specific environment variables to be set. 
  - see `env_setup.sh` for reference, it should be also used to set up variables in local env, however, I'd suggest to stick with `.env` file and compose 

#### Database Requirements:
- The target machine should have a **MySQL server** installed and properly configured.

#### Web Server Requirements:
- The target machine should be capable of running a web server with the Launchpad project preinstalled. The web-related files can be found in the `launchpad` package.

#### Backend Setup:
- The backend is located in the `icedata-v2` package and relies on the environment variables specified in the common setup section above.
- App configuration is split in several parts in `application.yml`: 
  - general spring and other dependencies configs like `spring`, `management`, `server`
  - app-specific config in same file `icedata-v2` part
  - files to work with in `files` segment

### Local Development

For local development, a `docker-compose` file is provided. The application can be started as a full system or by individual services.

#### Running only selected services
To run only selected services via docker compose you need to use service name, e.g.
```bash

docker compose up -d database java
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

Also `assemple.sh` recognize several cmd arguments:
  - `-c` is going to clean up all previous docker containers and volumes (be aware that database and users will be wiped as well)
  - `-t` runs `./gradlew clean test` before application rollout