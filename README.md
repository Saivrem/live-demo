# Live Demo

## Icedata V2

### Introduction
This project if a full representation of live-demo project. Codebase is split between several modules, including launchpad for web, icedata-v2 for java application. All projects could be assembled and run with docker compose file provided.

### Prerequisites

#### Common Setup:
- All components of this project share the same `.env` file, which is **not included** for security reasons. The project expects specific environment variables to be set. 
  - see `env_setup.sh` for reference, it should be also used to set up variables in local env, however, I'd suggest to stick with `.env` file and compose 

#### Database Requirements:
- Compose starts MySQL and initializes an empty database volume, including the Flyway user. No host MySQL installation is needed. Existing database volumes keep their users and passwords.

#### Web Server Requirements:
- Launchpad is static HTML/CSS/JavaScript served by Apache HTTP Server (`httpd:2.4-alpine`) without PHP.
- `DockerfileWeb` packages `launchpad/` and `apache.conf`. Static files are packaged into the image; rebuild the web image after edits.
- Apache proxies `/icedata/api/v2/` to the Java service. The browser loads languages from `/icedata/api/v2/languages` before restoring shared-link parameters.
- `/index.php` redirects to `/` while preserving query parameters for old shared links. The obsolete statistics page and Status button have been removed.

#### Shared Uploads:
- `POST /icedata/api/v2/uploads` accepts multipart `files[]`; `GET` on the same endpoint lists uploads. Each entry contains `name`, `url`, and `size`; its URL downloads the file as an attachment.
- Allowed extensions: `csv`, `xls`, `xlsx`, `docx`, `doc`. Files must be nonempty and at most 20 MiB each; the complete multipart request is limited to 100 MiB.
- Spring stores uploads with generated unique names in the `icedata-v2-uploads-volume` Docker volume, outside the static web directory. `/uploads/` is the file-list page.
- Files remain publicly shared, as before. Spring deletes them daily at 00:00 UTC, including files uploaded shortly before midnight.
- Configure `UPLOADS_DIRECTORY` to change the storage directory (`uploads` relative to the backend working directory outside Docker). `uploads.cleanup-cron` and `uploads.cleanup-zone` control cleanup.
- Existing files in `launchpad/uploads/` are not migrated into the new storage. Move any files that must be preserved out of that old directory before deploying, then upload them through the new form.

#### Backend Setup:
- The backend is located in the `icedata-v2` package and relies on the environment variables specified in the common setup section above.
- App configuration is split in several parts in `application.yml`: 
  - general spring and other dependencies configs like `spring`, `management`, `server`
  - app-specific config in same file `icedata-v2` part
  - files to work with in `files` segment

### Deployment on a Linux VM

Install Docker Engine with Compose and nftables kernel support. The fail2ban service uses the Linux host network namespace and `NET_ADMIN` to manage its own firewall tables. It needs neither privileged mode nor the Docker socket. Docker Desktop is not a substitute for validating protection on the target VM.

```bash
bash env_setup.sh
# Edit .env: set ICECAT_USER and ICECAT_PASS; check the other settings.
docker compose up -d --build
```

`env_setup.sh` generates independent random database passwords and leaves an existing `.env` untouched. Java builds inside Docker; a local JDK or prebuilt JAR is not required. Only Apache port 80 is public. MySQL (`127.0.0.1:3306`) and Java (`127.0.0.1:8086`) are available on the VM loopback interface. Containers connect through the Compose network using `database:3306` and `java:8086`; localhost is only used for host access and each service's own healthcheck.

`/icedata/api/v2/admin` and its subpaths return 404 for every method, including direct requests to Spring. Database updates run only on the scheduler, daily at 10:00 UTC. An empty database will remain empty until the first scheduled update; no HTTP initialization route exists.

For local frontend edits, rebuild `launchpad` with `docker compose up -d --build launchpad`. To start only the database and backend, use `docker compose up -d --build database java`.

### Fail2ban

The custom image in `fail2ban/` runs two jails over Apache's shared access log:

- `launchpad-scanners`: phpMyAdmin, Adminer, WordPress, PHPUnit/CGI probes, exposed repository/environment files, common database backups and sensitive Actuator endpoints. Five matching failures within 60 seconds trigger a one-hour ban.
- `launchpad-exploits`: literal/encoded directory traversal and probes for OS credential/environment files. Two matching failures within 60 seconds trigger a one-day ban.

Repeated bans increase up to seven days; state survives container recreation in `fail2ban-state`. Ordinary 404s, the legitimate `/index.php` redirect, uploads and healthchecks do not trigger bans. Filters inspect the request target, not attacker-controlled referer/user-agent strings. Tests for attack cases, IPv6, and benign traffic run during the image build. These are targeted scanner filters, not a general SQL injection/XSS detector or a DDoS mitigation service.

Fail2ban creates separate `inet` nftables tables (`icedata_f2b_forward`, `icedata_f2b_input`) with early forward/input hooks, covering Docker forwarding and host input for TCP ports 80/443 and both IP families. It does not edit Docker's own chains. Apache access logs rotate in eight files of approximately 10 MiB; `access.log` follows the current file. Docker stdout logs are bounded too.

This default deployment assumes clients connect directly to Apache. Do not put a CDN/load balancer in front without configuring trusted client-IP handling and adapting the ban action to that topology. Fail2ban deliberately does not trust incoming `X-Forwarded-For` headers. HTTPS certificates/TLS termination must be configured for the VM's actual domain; port 443 is covered by the ban action but is not currently published.

```bash
docker compose exec fail2ban fail2ban-client status
docker compose exec fail2ban fail2ban-client status launchpad-scanners
docker compose exec fail2ban fail2ban-client set launchpad-scanners unbanip 198.51.100.23
docker compose logs --tail=100 fail2ban
```

The last unban command is an example; substitute the actual address. Custom jail overrides can be mounted as `/etc/fail2ban/jail.d/99-local.local`. Do not run a second fail2ban instance that manages the same nftables tables.

To rerun the isolated firewall smoke test (Docker Engine required):

```bash
docker build -f DockerfileWeb -t codex-live-demo-apache-check .
docker build -t codex-live-demo-fail2ban-check fail2ban
python3 scripts/test-security.py
```

It checks real Apache logs, HTTP blocking/unblocking and IPv6 rule creation in a private container network namespace. It never uses host networking or changes the host firewall, and removes its test containers/volume afterwards. Validate the actual public path on the target VM after deployment as well.

### Java tests and coverage

With JDK 17 and Docker Engine running:

```bash
cd icedata-v2
./gradlew test jacocoTestReport
```

The full suite uses a disposable MySQL 8.4 Testcontainer with dynamic ports, real Flyway migrations and isolated upload/download directories. Integration tests use local HTTP gzip/XML fixtures, so they need no Icecat credentials or external Icecat requests. The suite covers HTTP APIs, Tomcat multipart limits, atomic upload rollback, Unicode filenames, repeated imports, batching, timestamp offsets, external XML entities and failure isolation between scheduled sources. No production containers or data are reused.

Reports: `icedata-v2/build/reports/tests/test/index.html` and `icedata-v2/build/reports/jacoco/test/html/index.html`.

The Dockerfile also has a `test` target for running the JVM itself inside Docker:

```bash
docker build --target test -f DockerfileJava -t codex-live-demo-java-tests .
docker run --rm \
  --add-host host.docker.internal:host-gateway \
  -e DOCKER_HOST=unix:///var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -e TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  -v /var/run/docker.sock:/var/run/docker.sock \
  codex-live-demo-java-tests
```

The socket mount is needed only by this trusted test runner to create disposable MySQL/Ryuk containers; it is not part of the deployed application or fail2ban service.

For a full stack smoke test after building the three `codex-live-demo-*-check` images:

```bash
docker build -f DockerfileJava -t codex-live-demo-java-check .
docker build -f DockerfileWeb -t codex-live-demo-apache-check .
docker build -t codex-live-demo-fail2ban-check fail2ban
python3 scripts/test-stack.py
```

This creates a separate Compose project, fresh database/log/upload volumes and random loopback ports using only test credentials, checks readiness and upload/list/download through Apache, and deletes its own containers and volumes afterwards. Fail2ban runs in a private namespace during this smoke test; `scripts/test-security.py` tests actual HTTP blocking separately.
