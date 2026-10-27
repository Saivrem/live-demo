"""Container smoke test. All firewall changes stay in a private network namespace.
Build codex-live-demo-apache-check and codex-live-demo-fail2ban-check first.
"""
import json
import subprocess
import time
import uuid

suffix = uuid.uuid4().hex[:10]
volume = 'icedata-security-test-' + suffix
containers = []

def docker(*args):
    return subprocess.check_output(['docker', *args], text=True, stderr=subprocess.STDOUT).strip()

def start(*args):
    cid = docker('run', '--rm', '-d', *args)
    containers.append(cid)
    return cid

def wait_for(check, message, attempts=30):
    for _ in range(attempts):
        try:
            if check(): return
        except (subprocess.CalledProcessError, AssertionError): pass
        time.sleep(.5)
    raise AssertionError(message)

try:
    docker('volume', 'create', volume)
    f2b = start('--cap-drop', 'ALL', '--cap-add', 'NET_ADMIN',
                '--mount', f'type=volume,src={volume},dst=/var/log/apache2,readonly',
                'codex-live-demo-fail2ban-check')
    web = start('--network', 'container:' + f2b,
                '--mount', f'type=volume,src={volume},dst=/var/log/apache2',
                'codex-live-demo-apache-check')
    admin_client = start('--entrypoint', 'sleep', 'codex-live-demo-fail2ban-check', '300')
    client = start('--entrypoint', 'sleep', 'codex-live-demo-fail2ban-check', '300')
    f2b_ip = next(iter(json.loads(docker('inspect', f2b))[0]['NetworkSettings']['Networks'].values()))['IPAddress']
    client_ip = next(iter(json.loads(docker('inspect', client))[0]['NetworkSettings']['Networks'].values()))['IPAddress']
    def request(path='/', expected=200, timeout=3):
        code = f'''import urllib.request,urllib.error
try:
 r=urllib.request.urlopen("http://{f2b_ip}{path}",timeout={timeout})
 assert r.status=={expected},r.status
except urllib.error.HTTPError as e:
 assert e.code=={expected},e.code
'''
        docker('exec', client, 'python3', '-c', code)
        return True
    wait_for(lambda: docker('exec', f2b, 'fail2ban-client', 'ping') == 'Server replied: pong', 'Fail2ban did not start')
    wait_for(lambda: request(), 'Apache did not start')
    for method in ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']:
        code = f'''import urllib.request,urllib.error
try:
 urllib.request.urlopen(urllib.request.Request("http://{f2b_ip}/icedata/api/v2/admin",method="{method}"))
 raise AssertionError("admin is exposed")
except urllib.error.HTTPError as e:
 assert e.code==404,e.code
'''
        docker('exec', admin_client, 'python3', '-c', code)
    # Use a fresh source so the filter must see real Apache log records.
    for _ in range(5): request('/phpmyadmin/', 404)
    wait_for(lambda: client_ip in docker('exec', f2b, 'fail2ban-client', 'get', 'launchpad-scanners', 'banip'), 'Scanner was not banned')
    for table, hook in [('icedata_f2b_forward', 'forward'), ('icedata_f2b_input', 'input')]:
        rules = docker('exec', f2b, 'nft', 'list', 'table', 'inet', table)
        assert client_ip in rules and f'hook {hook}' in rules and 'drop' in rules, rules
    try:
        request(timeout=1)
    except subprocess.CalledProcessError:
        pass
    else:
        raise AssertionError('Banned client can still reach Apache')
    docker('exec', f2b, 'fail2ban-client', 'set', 'launchpad-scanners', 'unbanip', client_ip)
    request()
    # IPv6 uses separate sets in both hooks even on a test bridge with no IPv6 route.
    docker('exec', f2b, 'fail2ban-client', 'set', 'launchpad-exploits', 'banip', '2001:db8::23')
    for table in ['icedata_f2b_forward', 'icedata_f2b_input']:
        assert '2001:db8::23' in docker('exec', f2b, 'nft', 'list', 'table', 'inet', table)
    docker('exec', f2b, 'fail2ban-client', 'set', 'launchpad-exploits', 'unbanip', '2001:db8::23')
    print('PASS: closed admin, real Apache scanner logs, IPv4 ban blocks HTTP, unban restores HTTP, forward/input hooks, IPv6 bans.')
except Exception:
    for container in containers[:2]:
        print(docker('logs', '--tail', '50', container))
    raise
finally:
    for container in reversed(containers):
        subprocess.run(['docker', 'stop', '-t', '5', container], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    subprocess.run(['docker', 'volume', 'rm', volume], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
