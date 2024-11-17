# Live demo

## Icedata V2

### Intro
This project serves as a BE part for launchpad website

### Prerequisites

Database:
- Target machine should have MySQL server installed and configured

Webserver:
- Target machine should have means to run web server with launchpad project preinstalled. Web related files are located in package launchpad

Backend:
- Located in package icedata-v2, relies on a set of env variables, see application.yml

Local development:

- For the convenience of local development docker-compose file is provided, application could be started as whole system or partially via separate services, e.g. `docker compose up -d database` to run only MySQL server
- For full run small bash script `assemble.sh` is provided, it will remove old artifact, build new one and execute docker compose file

