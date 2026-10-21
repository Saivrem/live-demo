"""Disposable full Compose smoke test; uses test credentials, random loopback ports
and private fail2ban networking. Build the three codex-live-demo-*-check images first.
Production containers, data and host firewall are never modified.
"""
import json, os, pathlib, subprocess, tempfile, time, urllib.request, urllib.error, uuid
root=pathlib.Path(__file__).resolve().parents[1]
project='icedata-smoke-'+uuid.uuid4().hex[:8]
values=dict(DB_USER='smoke_app',DB_PASS='smoke_app_password',DB_SCHEMA='smoke_db',DB_PORT='3306',DB_HOST='database',DB_PARAMS='serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8',FLYWAY_USER='smoke_flyway',FLYWAY_PASSWORD="smoke'back\\slash",MYSQL_ROOT_PASSWORD='smoke_root_password',ICECAT_USER='smoke_user',ICECAT_PASS='smoke_password',ICECAT_BASE='https://data.icecat.biz/export/',ICECAT_REPO='freexml/',JAVA_HOST='http://java',JAVA_PORT='8086')
with tempfile.TemporaryDirectory(prefix='icedata-stack-') as temporary:
    config=json.loads(subprocess.check_output(['docker','compose','config','--format','json'],cwd=root,text=True))
    config.pop('name',None)
    for network in config.get('networks',{}).values(): network.pop('name',None)
    for volume in config['volumes'].values(): volume.pop('name',None)
    for name,service in config['services'].items():
        service.pop('container_name',None); service.pop('build',None)
        service['restart']='no'
        if name in ['java','database']:
            service['environment']=dict(values)
        if name=='database':
            service['environment'].update(MYSQL_DATABASE=values['DB_SCHEMA'],MYSQL_USER=values['DB_USER'],MYSQL_PASSWORD=values['DB_PASS'])
        if name=='java':
            service['environment'].update(UPLOADS_DIRECTORY='/var/lib/icedata/uploads',ICEDATA_V2_CRON='-',UPLOADS_CLEANUP_CRON='-',ICEDATA_V2_UPDATE_ZONE='UTC')
        if name in ['java','launchpad','fail2ban']:
            service['image']={'java':'codex-live-demo-java-check','launchpad':'codex-live-demo-apache-check','fail2ban':'codex-live-demo-fail2ban-check'}[name]
        for port in service.get('ports',[]):
            port['host_ip']='127.0.0.1'; port['published']='0'
        if name=='fail2ban':
            service.pop('network_mode',None) # Private namespace; never touch host firewall.
    file=pathlib.Path(temporary)/'compose.json'; file.write_text(json.dumps(config)); file.chmod(0o600)
    compose=['docker','compose','-p',project,'-f',str(file)]
    def call(*args): return subprocess.check_output([*compose,*args],text=True,stderr=subprocess.STDOUT).strip()
    def get(base,path):
        try:return urllib.request.urlopen(base+path,timeout=10)
        except urllib.error.HTTPError as error:return error
    try:
        call('up','-d','--wait','--wait-timeout','180')
        web='http://'+call('port','launchpad','80'); backend='http://'+call('port','java','8086')
        assert get(web,'/').status==200
        assert get(web,'/icedata/api/v2/admin').status==404
        assert get(backend,'/icedata/api/v2/admin').status==403
        assert json.loads(get(web,'/icedata/api/v2/languages').read())==[]
        boundary='icedata-smoke-boundary'
        payload=(f'--{boundary}\r\nContent-Disposition: form-data; name="files[]"; filename="products.csv"\r\nContent-Type: text/csv\r\n\r\nid,name\n1,test\r\n--{boundary}--\r\n').encode()
        request=urllib.request.Request(web+'/icedata/api/v2/uploads',data=payload,headers={'Content-Type':'multipart/form-data; boundary='+boundary},method='POST')
        response=urllib.request.urlopen(request,timeout=10); assert response.status==201
        uploaded=json.loads(response.read()); assert uploaded[0]['name']=='products.csv'
        assert get(web,uploaded[0]['url']).read()==b'id,name\n1,test'
        assert len(json.loads(get(web,'/icedata/api/v2/uploads').read()))==1
        assert 'launchpad-scanners' in call('exec','-T','fail2ban','fail2ban-client','status')
        print('PASS: fresh MySQL volume + Flyway user (quoted password), Java startup/health, Apache API proxy, closed admin, upload/list/download, fail2ban health. All services healthy.')
    except Exception as error:
        if isinstance(error,subprocess.CalledProcessError): print(error.output[-5000:])
        print(call('logs','--tail','50','java','database','fail2ban'))
        raise
    finally:
        subprocess.run([*compose,'down','--volumes','--remove-orphans'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
