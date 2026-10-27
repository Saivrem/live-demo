# Live Demo / Icedata V2

A web interface for working with Icecat. Apache serves the pages, Java/Spring handles the API and file uploads, and MySQL stores reference data. Fail2ban blocks common scanners using Apache access logs. PHP is no longer required.

## Using Launchpad

Choose **Find by** (Brand + MPN, EAN / GTIN, or Icecat product ID), then choose a
response type and language. Only the relevant identifier fields are shown.
**Account settings** contains the Icecat username and optional JSON app key.

- **JSON**: show the response on the page or open JSON in a new tab. Field selection
  is optional; leave it empty for the complete response.
- **XML / CSV**: choose Product XML, Meta XML or Product CSV and open it in a new tab.
- **Icecat Live · full**: show the complete product datasheet.
- **Icecat Live · selected blocks**: choose at least one block, such as gallery,
  essential information or specifications.
- **Icecat product page**: opens the product page when searching by product ID.

Live modes also provide **Get embed code**, with a copy button. **Copy request link**
saves the current identifier, response type, language, selected fields and resource
repository in a shared link. Filled account settings, including the app key, are
included in shared links. Existing links using `radio` and `requestType` remain
supported. During loading, the previous result stays visible; errors provide a
retry action without discarding that result.

The **Resources** menu groups index files, reference files and documentation into
expandable branches. Its repository selector and the form language determine file
URLs; changing these does not change server-side import or monitoring credentials.
Header controls provide descriptions on hover and keyboard focus.

Live content uses separate scoped styles (`launchpad/styles/live.css`), including
compact gallery controls and theme-aware text, specification row backgrounds,
group headings, borders and tooltips. Switching themes updates an already loaded
Live datasheet without another request. The application theme is restricted
to Launchpad's own controls so it cannot resize embedded Icecat Live buttons.

## Quick start on a virtual machine

These instructions target a Linux server. Run all commands **in a terminal on the server**. You need Docker Engine with Compose v2, internet access, and Icecat credentials.

### 1. Prepare the server

Install Docker Engine and the Compose plugin using the [official Ubuntu instructions](https://docs.docker.com/engine/install/ubuntu/). For another Linux distribution, select its installation guide in the Docker documentation. You do not need to install Java, Gradle, MySQL, or Apache separately: they are built or run inside containers.

The commands below also require `bash`, `openssl`, `curl`, and the `nano` editor. On Ubuntu, install them with `sudo apt install bash openssl curl nano`.

Check your installation:

```bash
docker info
docker compose version
```

Both commands should finish without errors. If you get `permission denied`, configure user access using the [Docker post-installation instructions](https://docs.docker.com/engine/install/linux-postinstall/) and reconnect over SSH. Docker access grants administrative privileges on the server.

The full stack requires a regular, rootful Docker Engine and nftables support in the Linux kernel: fail2ban uses host networking and `NET_ADMIN`. Rootless Docker is not supported by this configuration. You can develop and run tests on macOS/Windows, but validate public-facing protection on the target Linux VM.

Allow inbound **TCP port 80** in your hosting provider's network settings. Keep SSH access available. **Do not expose ports 3306 or 8086 publicly.**

### 2. Copy the project

Clone the repository or copy the complete project to the server. For example, if the project directory is named `live-demo`:

```bash
cd live-demo
ls assemble.sh docker-compose.yml env_setup.sh
```

All three files should exist. Run the remaining commands from this directory, next to `docker-compose.yml`.

### 3. Create and edit the configuration

```bash
bash env_setup.sh
nano .env
```

See [`.env.example`](.env.example) for an annotated template. It separates settings into four groups: Icecat credentials, export paths, database passwords, and the internal network.

The script creates `.env` next to itself using this template, generates random database passwords, and sets file permissions to `600`. It works from any directory. Do not copy the example directly to `.env`: the script must replace the `__GENERATE_*__` placeholders. For help, run `bash env_setup.sh --help`.

Replace these two values in the editor:

| Variable | What to enter |
| --- | --- |
| `ICECAT_USER` | Your Icecat account login instead of `YOUR_ICECAT_LOGIN` |
| `ICECAT_PASS` | Your Icecat password instead of `YOUR_ICECAT_PASSWORD` |

Keep the other defaults for a standard deployment. If your account uses a different export directory, adjust `ICECAT_BASE` and `ICECAT_REPO`. Use single quotes around a password containing `$` so Compose does not interpret part of it as a variable.

In `nano`, press **Ctrl+O**, then **Enter** to save, and **Ctrl+X** to exit.

If `.env` already exists, `env_setup.sh` reports this and leaves it unchanged. Open the existing file instead. **Do not regenerate passwords for an existing database:** changing `.env` does not update users or passwords in the saved MySQL database.

`.env` contains secrets. Do not commit it or publish its contents in logs or messages.

### 4. Start the service

```bash
bash assemble.sh --initial-start
```

The first run downloads images and dependencies, builds Java, creates the database, and applies migrations. This can take several minutes. The script succeeds when the services are ready; after starting containers, it waits up to 180 seconds for readiness.

Available options:

| Command | What it does |
| --- | --- |
| `bash assemble.sh` | Builds images and starts the stack |
| `bash assemble.sh --initial-start` | Builds and starts the stack, importing reference data from Icecat at startup |
| `bash assemble.sh -t` | Runs Java tests in Docker, then builds and starts the stack |
| `bash assemble.sh -c` | Builds images, stops existing containers, and recreates them while preserving data |
| `bash assemble.sh -t -c` | Runs tests, builds images, and recreates containers |
| `bash assemble.sh --help` | Displays help |

A test or build failure stops the script before any running containers are shut down. **`-t` does not mean “tests only”:** the service starts after successful tests. See the development section below for standalone tests.

To start directly with Compose:

```bash
docker compose up -d --build --wait --wait-timeout 180
```

### 5. Check the result

```bash
docker compose ps
curl -fsS http://127.0.0.1/icedata/api/v2/actuator/health
```

The list should show `database`, `java`, `launchpad`, and `fail2ban` running. The database, Java, and fail2ban should be `healthy`; Apache has no separate healthcheck. The health request should return JSON containing `"status":"UP"`.

Open **`http://SERVER_IP/`** in your browser, replacing `SERVER_IP` with the VM's actual address. Using `localhost` in a browser on your computer refers to your computer, not the server.

**Use `--initial-start` to populate reference data immediately.** It runs the same
language and supplier import services used by the daily **10:00 UTC** scheduler,
after Flyway migrations. It requires valid Icecat credentials and network access.
An import exception or a source producing no records fails application startup;
check the Java logs and configuration before retrying. Existing records are updated
by ID; this flag does not delete or recreate the database.

Without the flag, a new database remains empty until the next scheduled import.
Check import progress and look for `Initial database import completed` in the logs
(the container health response alone does not confirm that import has finished):

```bash
docker compose logs --tail=200 java
curl -fsS http://127.0.0.1/icedata/api/v2/languages
```

For subsequent deployments, run `bash assemble.sh` without `--initial-start`.
The flag stays in the Java container command until it is recreated without it;
restarting that container repeats the import. It is not an automatic first-run detector.
The flag can be combined with `-t` and `-c`.

To request the import directly through Compose:

```bash
INITIAL_START=true docker compose up -d --build --wait --wait-timeout 180
```

For a standalone Java artifact (with the database and environment configured):

```bash
java -jar icedata-v2/build/libs/icedata-v2-0.1.0-SNAPSHOT.jar --initial-start
```

Flyway migrations still run on every normal startup; `--initial-start` controls
reference data import only. `--initial-start=false` explicitly disables it.

Administrative API access requires the `ICEDATA_ADMIN` authority: this covers `/icedata/api/v2/admin` (including subpaths) and `POST /icedata/api/v2/suppliers`. Spring Security returns `403` without that authority. No users or login flow are configured, and no default user is generated. Administrative writes also require a valid CSRF token. Apache continues to return `404` for admin routes; the retired admin controller remains a `404` stub even after authorization. There is no manual import button. Public read endpoints and uploads remain available without authentication.

## Troubleshooting

Start by checking service status and recent logs:

```bash
docker compose ps -a
docker compose logs --tail=100 database java launchpad fail2ban
```

To follow logs as they arrive:

```bash
docker compose logs -f --tail=100 java
```

**Ctrl+C** stops following logs; the service keeps running.

| Symptom | What to check |
| --- | --- |
| `Cannot connect to the Docker daemon` | Is Docker running? On Linux with systemd: `sudo systemctl start docker` |
| `Missing .env` | Complete step 3 and make sure `.env` is next to `assemble.sh` |
| `port is already allocated` / `address already in use` | Check whether another service uses port 80, 3306, or 8086, or another copy of this project is running |
| MySQL or Java does not become `healthy` | Read the `database` and `java` logs; verify that `.env` passwords match those in the existing database |
| Readiness timeout | Containers may still be starting. Check `ps -a` and logs, resolve the cause, then run `bash assemble.sh` again |
| `curl` works on the server, but the site is unreachable externally | Check the IP address, inbound TCP 80 in your provider's network settings, and the `http://` URL |
| Empty language list | Has a scheduled 10:00 UTC import run since deployment? Check Icecat credentials, export paths, and Java import logs |
| Fail2ban reports nftables errors or `Operation not permitted` | Use a Linux VM with regular Docker Engine. Check kernel nftables support and that Compose still sets `network_mode: host` and `NET_ADMIN` |

Do not delete the database volume to fix a startup error: it contains your data.

## Updating and stopping

Back up the database and `.env` before updating. If the project was cloned with Git and your local changes are saved:

```bash
git pull --ff-only
bash assemble.sh -t
```

The build recreates services that have changed. Frontend files are packaged in the Apache image, so changes under `launchpad/` appear after rebuilding. To update only the frontend:

```bash
docker compose up -d --build launchpad
```

Stop the stack while keeping data:

```bash
docker compose down
```

Start the existing images again:

```bash
docker compose up -d --wait --wait-timeout 180
```

**Do not add `-v` / `--volumes` to `docker compose down`: it deletes data volumes.** Avoid changing the project directory name or Compose project name unnecessarily: these affect volume selection, and a different project may create a new, empty database.

## Data storage and ports

| Component | Access |
| --- | --- |
| Apache / website | Public port 80 |
| Spring API | Through Apache; direct access only on the server at `127.0.0.1:8086` |
| MySQL | Only on the server at `127.0.0.1:3306` |
| Fail2ban | No HTTP port; manage it with `docker compose exec` |

Within Compose, containers connect using `database:3306` and `java:8086`. Do not replace these addresses with `localhost`: inside a container, it refers to that container itself.

| Compose volume | Contents |
| --- | --- |
| `icedata-v2-mysql-volume` | MySQL database |
| `icedata-v2-uploads-volume` | Uploaded files |
| `apache-logs` | Apache logs |
| `fail2ban-state` | Fail2ban state |

Docker prefixes volume names with the project name. Normal restarts, rebuilds, and `assemble.sh -c` preserve volumes. Persistent volumes are not a substitute for backups.

## Suppliers CSV

`/share/SuppliersList.csv` is exported from the suppliers database immediately after
supplier import completes, both with `--initial-start` and with the daily scheduler
(10:00 UTC). The supplier import service calls the CSV exporter directly.
No extra XML download is performed.
The UTF-8 CSV columns are `supplier_id`, `supplier_name`, `is_sponsor`, `brand_logo`,
ordered by supplier ID. CSV fields and column order are defined by `@CsvMapping`
on the `@CsvRow` supplier entity; unannotated relationships are excluded. All data fields are enclosed in double quotes, with embedded quotes doubled.
Commas, line breaks and whitespace are preserved; null values become quoted empty fields.
The CSV becomes available after suppliers have been imported.
Java writes to a persistent shared volume, mounted read-only by Apache at `/share/`.
Publication is atomic; export failures leave the previous CSV available.
Outside Docker, the output directory defaults to `share/` and can be set with `SHARE_DIRECTORY`.

## File uploads

The upload listing is available at `/uploads/`. Java accepts nonempty `csv`, `xls`, `xlsx`, `docx`, and `doc` files, up to **20 MiB per file** and **100 MiB per multipart request**. Stored files receive unique names and are downloaded as attachments.

**Files are public and deleted daily at 00:00 UTC**, including files uploaded shortly before midnight. This feature is intended for temporary file sharing.

API:

- `POST /icedata/api/v2/uploads` — multipart field `files[]`.
- `GET /icedata/api/v2/uploads` — a list containing `name`, `url`, and `size`.
- The address in `url` downloads the file.

Old files in `launchpad/uploads/` are not migrated automatically. Save any files you need before upgrading, then upload them through the new form. Old `/index.php?...` links redirect to the home page while preserving query parameters.

## Daily index monitoring

The **Status** button opens `/statistic/`. The page reads saved checks from
`GET /icedata/api/v2/daily-indexes`; opening or reloading it does not query Icecat.
A scheduler sends authenticated `HEAD` requests for `daily.index.xml.gz` and
`daily.index.csv.gz` for each distinct language in the database. It saves one row
per repository, language and format, updating rows without clearing the table.

Set credential scope explicitly in `.env` (existing installations should add this):

```dotenv
ICECAT_ACCESS='free'
DAILY_INDEX_DELAY='PT3H'
```

- `free` (default): check `freexml` only.
- `full`: check both `freexml` and `level4`. Requires credentials with full access.

This setting is separate from `ICECAT_REPO`, which chooses the source for reference
imports. Switching back to `free` also hides previously saved `level4` rows from
the API and page. Monitoring never guesses permission by probing `level4` in free mode.

The delay is measured after each sweep, defaults to **3 hours**, and accepts ISO-8601
durations (`PT2H` for 2 hours). Normal startup schedules the first check after one
minute; `--initial-start` explicitly checks after the language and supplier imports.
An empty language table postpones checks until a later sweep. Failed checks are not
retried immediately. `DAILY_INDEX_INITIAL_DELAY` (default `PT1M`) and
`DAILY_INDEX_TIMEOUT_MILLIS` (default `10000`, per connect/read operation) are optional.

File colors follow the original dashboard's calendar-day rules in **UTC**:

| Color | Last-Modified date |
| --- | --- |
| Green | Today |
| Yellow | Yesterday |
| Red | Earlier than yesterday |

These are calendar days, not elapsed 24-hour windows. Missing, invalid or future
timestamps have unknown freshness. HTTP and network errors are shown separately,
including the last check time and HTTP status where available. Failed checks keep
the previous file timestamp rather than substituting the current time. An overdue
check is shown in grey so an old successful check cannot appear green indefinitely.
On the status page, timestamps are always UTC.

## Fail2ban and HTTPS

The project builds its own fail2ban image. It reads Apache access logs and enables two filters:

| Jail | What it detects | Initial ban |
| --- | --- | --- |
| `launchpad-scanners` | Probes for phpMyAdmin, Adminer, WordPress, PHPUnit/CGI, `.env`, `.git`, backups, and sensitive service endpoints | 5 matches within 60 seconds → 1 hour |
| `launchpad-exploits` | Directory traversal attempts and probes for system files | 2 matches within 60 seconds → 1 day |

Repeated bans increase up to 7 days. An ordinary 404 alone does not trigger a ban. Filters inspect the request target and response status; they target specific scanners, not every form of DDoS, SQL injection, or XSS.

Check the jails:

```bash
docker compose exec fail2ban fail2ban-client status
docker compose exec fail2ban fail2ban-client status launchpad-scanners
docker compose exec fail2ban fail2ban-client status launchpad-exploits
```

Unban an address (replace the example IP and select the jail where it is banned):

```bash
docker compose exec fail2ban fail2ban-client set launchpad-scanners unbanip 198.51.100.23
```

Fail2ban manages separate nftables tables, `icedata_f2b_input` and `icedata_f2b_forward`, for TCP ports 80/443 and both IPv4 and IPv6. It does not need the Docker socket. Apache and container logs have size limits. Do not run a second fail2ban instance managing the same tables.

The current setup assumes **clients connect directly to Apache**. Before placing a CDN or another proxy in front, configure trusted proxy addresses, client IP handling, and a ban action appropriate for that topology. Incoming `X-Forwarded-For` headers are not trusted.

**HTTPS is not configured yet.** The site serves HTTP on port 80. For your domain, configure a certificate and TLS in Apache, publish port 443, and allow it in your provider's network settings. Including port 443 in fail2ban rules does not enable HTTPS.

## Frontend dependencies

The modern and classic pages use Bootstrap **5.3.8** (CSS and bundle with
Popper from jsDelivr, pinned with SRI). The modern request UI uses native browser
APIs and ES modules; no bundler or jQuery is required. The classic page keeps
jQuery **4.0.0** (full build, served locally) and its original JSONViewer.

### Request UI modules

`launchpad/js/logic.js` is the page controller. Modules under `launchpad/js/request/`
separate form state and validation (`form.js`, `state.js`), request and shared URLs
(`links.js`), JSON/language loading (`api.js`), resource navigation (`resources.js`),
the existing Live renderer and embed snippets (`live.js`, `embed.js`), result
layout and expansion (`result-panel.js`), and clipboard access (`clipboard.js`). Serve the frontend over HTTP; native modules do not run
by opening `index.html` directly with a `file:` URL.

The selected response is submitted with the primary button or Enter in a text or
password field. Missing fields have inline descriptions and keyboard focus moves
to the first error. A successful embedded response includes a product, language
and format summary captured when the request was submitted. Editing the form or
a subsequent failed request does not change the context of the displayed result.

## Website appearance

The sun/moon button switches between **Ayu Light** and **Ayu Dark** on Launchpad,
Status and Uploads. The first visit follows the system color preference;
an explicit selection is saved in browser storage and shared between pages and tabs.
If storage is unavailable, the switch still works for the current page.

The separate **Old version** link opens `/classic/`: the original layout, controls
and frontend scripts from `main`, with isolated styles and scripts. Status and
Uploads also have classic pages. **New version** returns to the corresponding
modern page, preserving request query parameters. Both versions use the same
backend and shared files. The classic files are a compatibility snapshot; update
API integrations there explicitly if backend contracts change. The modern Ayu
theme preference does not affect the classic interface.

The footer stays at the bottom of short pages and follows the content on longer
pages without covering it. The copyright year is calculated in the browser.
Ayu palette attribution and its MIT license are included in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and packaged in the web image.

## Working with results

**Expand result** fills the browser viewport with the current JSON or Live result.
The existing content stays mounted, including the Live gallery. **Exit expanded
view** or Escape returns to the page and restores focus and the previous scroll
position. **Back to form** exits expanded view if needed and focuses the primary
request button. The result header remains visible while scrolling; JSON controls
stay directly below it. These controls also remain available on narrow screens.

## JSON response viewer

The modern Launchpad uses a read-only inspector in `launchpad/js/json-inspector.js`
with Ayu-aware styles in `launchpad/styles/json-inspector.css`. It uses browser
APIs and adds no third-party dependencies. The classic page keeps JSONViewer.

- **Tree / Raw** switches between a collapsible tree and formatted JSON. Open
  branches are preserved when switching modes.
- **Find a key or value** searches the complete displayed payload, including
  collapsed branches, highlights matches and shows matching branches. Clear the
  search to browse the full tree again.
- Each row has **Value** (copies JSON, including string quotes) and **Path**
  (copies a bracket-notation JSONPath such as `$["Gallery"][0]["Pic"]`). Actions
  appear on hover or keyboard focus and remain visible on touch devices.
- **Payload** switches between **Product data (data)** and **Full API response**,
  including service fields such as `msg` and any response metadata. The default
  is `data` when present, including `null`; responses without that member show the
  full response. Switching makes no new request and preserves each tree's open
  branches when the search is unchanged. Tree/Raw mode and search are shared
  between payload views. Field paths are relative to the selected payload.
- **Copy JSON** and **Download** export the selected payload in full, even while
  search is active. Downloads are named `icecat-data.json` for data and
  `icecat-response.json` for the full response.
- Branch contents render on demand, with up to 100 children per batch. **Show
  next** reveals further items. The Raw view and exported JSON include all items.

Response strings are inserted as text. Clipboard access falls back to the
browser's copy command on HTTP; if copying is blocked, select text in Raw.

## Development tests

### Frontend tests without Docker

With Node.js 24, run from the repository root:

```bash
node --test scripts/test-frontend.mjs
```

These tests cover active-field validation, reserved characters in request URLs,
shared-link restoration, content aliases, request summaries, API errors, JSON
payload selection and executable Live embed snippets. They do not call Icecat or require the backend.


### Java tests without starting the application stack

You need a local JDK 17 and a running Docker Engine:

```bash
cd icedata-v2
./gradlew test jacocoTestReport
cd ..
```

Tests create a disposable MySQL container and use local XML/gzip fixtures and isolated directories. They do not need the production database or Icecat credentials.

Local test reports:

- `icedata-v2/build/reports/tests/test/index.html` — test results.
- `icedata-v2/build/reports/jacoco/test/html/index.html` — code coverage.

### Java tests entirely inside Docker

No local JDK is required. Run from the project root:

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

Only the test container uses the Docker socket to create the disposable database. Results appear in the terminal; HTML reports from this run are deleted with the container. Use the local test command above to keep reports on disk.

### Full stack and firewall smoke tests

You need Docker and Python 3. Run from the project root with `.env` prepared:

```bash
docker build -f DockerfileJava -t codex-live-demo-java-check .
docker build -f DockerfileWeb -t codex-live-demo-apache-check .
docker build -t codex-live-demo-fail2ban-check fail2ban
python3 scripts/test-stack.py
python3 scripts/test-security.py
```

The first script creates a separate Compose project with a fresh database and random loopback ports, then checks readiness and uploads/downloads through Apache. The second checks bans triggered by real HTTP requests, unbanning, and IPv6 rules. Both use isolated test resources and remove them afterwards. Firewall tests run in a private network namespace without changing the host firewall. Validate external access and bans separately on the target VM after deployment.

## GitHub Actions

The [Java and Docker CI](.github/workflows/gradle.yml) workflow runs on pushes to `main`, pull requests targeting `main`, and manual runs through **Actions → Java and Docker CI → Run workflow**.

It runs two independent checks:

- **Java tests and build** — Java 17, Gradle wrapper validation, tests with disposable MySQL, coverage reports, and JAR packaging.
- **Docker stack and fail2ban** — temporary `.env` generation, Compose validation, three image builds, an isolated test stack, and HTTP ban/unban checks.

These checks require no GitHub Secrets. Import tests use local fixtures, not a real Icecat account. The workflow validates the build; it does not publish images or deploy to your VM.

The completed run's **Artifacts** section contains `java-test-reports` and `docker-test-logs`, retained for 14 days. Upload steps also run after failures; a report may be absent if the build failed before creating it. In the Java report archive, open `reports/tests/test/index.html` or `reports/jacoco/test/html/index.html`.
