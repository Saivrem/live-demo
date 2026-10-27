"""Run in the fail2ban image; fail the build on missed probes or false positives."""
from datetime import datetime
from pathlib import Path
import re
import subprocess
import tempfile

now = datetime.now().astimezone().strftime('%d/%b/%Y:%H:%M:%S %z')

def log(target, status=404, method='GET', ip='198.51.100.23', referer='-', agent='test'):
    return f'{ip} - - [{now}] "{method} {target} HTTP/1.1" {status} 123 "{referer}" "{agent}"\n'

safe = [
    log('/'), log('/missing-image.png'), log('/favicon.ico'),
    log('/index.php?lang=DE&radio=ean', 301),
    log('/icedata/api/v2/languages', 200), log('/icedata/api/v2/actuator/health', 200),
    log('/icedata/api/v2/uploads', 201, 'POST'), log('/uploads/', 200),
    log('/icedata/api/v2/uploads/00000000-0000-0000-0000-000000000000-backup.sql.csv', 404),
    log('/?mpn=../../etc/passwd', 200),
    log('/missing', referer='https://example.com/phpmyadmin/', agent='GET /../../etc/passwd HTTP/1.1'),
    log('/missing?x=198.51.100.99 - - [date] GET /phpmyadmin HTTP/1.1'),
]
probes = [
    log('/phpmyadmin/'), log('/PHPMyAdmin/index.php?route=/'), log('/pma/'),
    log('/adminer.php'), log('/wp-login.php', method='POST'), log('/wp-admin/'),
    log('/.env'), log('/.env.production'), log('/.git/config'), log('/.svn/entries'),
    log('/vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php', method='POST'),
    log('/cgi-bin/test.cgi'), log('/dump.sql.gz'), log('/actuator/env'),
    log('/icedata/api/v2/actuator/heapdump'), log('/icedata/api/v2/admin', 404),
    log('/wp-login.php', ip='2001:db8::23'),
]
exploits = [
    log('/etc/passwd'), log('/proc/self/environ'),
    log('/../../etc/passwd', 400), log('/foo/../bar', 404),
    log('/%2e%2e/%2e%2e/etc/passwd', 400), log('/.%2e/%2e./etc/passwd', 400),
    log('/%252e%252e%2fetc%2fpasswd', 404),
    log('/download?file=/etc/passwd', 403), log('/download?file=%2fetc%2fshadow', 404),
    log('/?file=/proc/self/environ', 404, ip='2001:db8::23'),
]

def check(name, lines, expected):
    with tempfile.NamedTemporaryFile(mode='w', suffix='.log') as fixture:
        fixture.writelines(lines)
        fixture.flush()
        result = subprocess.run(['fail2ban-regex', '--usedns=no', fixture.name,
                                 f'/etc/fail2ban/filter.d/{name}.conf'], capture_output=True, text=True)
    match = re.search(r'Lines: \d+ lines, \d+ ignored, (\d+) matched, (\d+) missed', result.stdout)
    if result.returncode or not match or int(match[1]) != expected:
        raise AssertionError(f'{name}: expected {expected} matches\n{result.stdout}\n{result.stderr}')

for name, attacks in [('launchpad-scanners', probes), ('launchpad-exploits', exploits)]:
    check(name, attacks, len(attacks))
    check(name, safe, 0)
print(f'PASS: {len(probes) + len(exploits)} attack examples and {len(safe)} benign examples per filter.')
