#!/usr/bin/env python3
"""Run startup checks for the plugin on official Paper-family and BuildTools servers.

Requires network access and Java 8, 16, 17, 21, and 25 runtimes. Temporary server
files are written to /tmp/mytheria-matrix; the incremental report stays here.
"""
import json
import os
import shutil
import signal
import subprocess
import time
import urllib.request
import zlib
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
ROOT = Path('/tmp/mytheria-matrix')
PLUGIN = REPO / 'target/MytheriaHoppers.jar'
RESULTS = REPO / 'compatibility/results.json'
REPORT = REPO / 'compatibility/results.md'
UA = 'MytheriaHoppers disposable compatibility test'

def get_json(url):
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)

def download(url, dest):
    dest.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=300) as r, dest.open('wb') as f:
        shutil.copyfileobj(r, f)

def key(v):
    return tuple(map(int, v.split('.')))

def save():
    RESULTS.write_text(json.dumps(results, indent=2) + '\n')
    lines = ['# Compatibility matrix', '',
             'Startup check: plugin enabled and logged its successful-enable message. '
             'Each row is a stable Minecraft release listed by Paper; the exact server '
             'build used is recorded in `results.json`. `N/P` means that server type '
             'does not publish that release; `Unavailable` means no usable build was obtained.', '',
             '| Minecraft | Paper | Purpur | CraftBukkit | Spigot |',
             '|---|---|---|---|---|']
    for v, row in results.items():
        def fmt(s):
            if s == 'NOT RUN': return 'Pending'
            if s == 'NOT PUBLISHED': return 'N/P'
            if s.startswith('UNAVAILABLE:'): return 'N/A†'
            if s == 'PASS (alpha build)': return 'PASS*'
            if s.startswith('PASS'): return 'PASS'
            return s
        lines.append('| ' + v + ' | ' + ' | '.join(fmt(row[k]) for k in ('Paper','Purpur','CraftBukkit','Spigot')) + ' |')
    lines += ['', '* Paper alpha build; † BuildTools did not produce a runnable server jar (details in `results.json`).',
              f'', f'Versions covered: {len(results)}. Results update after each server check.']
    REPORT.write_text('\n'.join(lines) + '\n')

def java_for(v):
    n = key(v)
    if n < (1, 17): return ROOT / 'java8/bin/java'
    if n < (1, 18): return ROOT / 'java16/bin/java'
    if n < (1, 20, 5): return ROOT / 'java17/bin/java'
    if n < (26,): return Path('/usr/local/sdkman/candidates/java/21.0.12+1-ms/bin/java')
    return Path('/usr/local/sdkman/candidates/java/25.0.4+1-ms/bin/java')

def buildtools_java_for(v):
    n = key(v)
    if n < (1, 17): return ROOT / 'java8/bin/java'
    if n < (1, 18): return ROOT / 'java16/bin/java'
    if n < (1, 20, 5): return ROOT / 'java17/bin/java'
    if n < (26,): return Path('/usr/local/sdkman/candidates/java/21.0.12+1-ms/bin/java')
    return Path('/usr/local/sdkman/candidates/java/25.0.4+1-ms/bin/java')

def install_runtime(major):
    dest = ROOT / f'java{major}'
    if (dest / 'bin/java').exists(): return
    archive = ROOT / f'jdk{major}.tar.gz'
    urls = {
        8: 'https://api.adoptium.net/v3/binary/latest/8/ga/linux/x64/jdk/hotspot/normal/eclipse',
        16: 'https://api.adoptium.net/v3/binary/latest/16/ga/linux/x64/jdk/hotspot/normal/eclipse',
        17: 'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse',
    }
    download(urls[major], archive)
    dest.mkdir(parents=True, exist_ok=True)
    subprocess.run(['tar','-xzf',str(archive),'-C',str(dest),'--strip-components=1'],check=True)

def run_server(kind, version, jar):
    d = ROOT / 'runs' / f'{kind}-{version}'
    (d / 'plugins').mkdir(parents=True, exist_ok=True)
    shutil.copy2(PLUGIN, d/'plugins/MytheriaHoppers.jar')
    shutil.copy2(jar, d/'server.jar')
    (d/'eula.txt').write_text('eula=true\n')
    port = 35000 + zlib.crc32(f'{kind}-{version}'.encode()) % 15000
    (d/'server.properties').write_text('\n'.join([
        'online-mode=false',f'server-port={port}','max-players=1','view-distance=2',
        'simulation-distance=2','level-type=flat','generate-structures=false',
        'spawn-protection=0','allow-nether=false'])+'\n')
    bukkit = d/'bukkit.yml'
    bukkit.write_text('settings:\n  allow-end: false\n')
    log = d/'server.log'
    with log.open('w') as out:
        proc = subprocess.Popen([str(java_for(version)),'-Xms384M','-Xmx1024M','-jar','server.jar','--noconsole'],cwd=d,stdout=out,stderr=subprocess.STDOUT,start_new_session=True)
    result = 'FAIL: startup timeout'
    deadline = time.time()+150
    while time.time() < deadline:
        time.sleep(1)
        txt = log.read_text(errors='replace') if log.exists() else ''
        if 'MytheriaHoppers enabled successfully!' in txt:
            result = 'PASS'; break
        if proc.poll() is not None:
            result = f'FAIL: server exited {proc.returncode}'
            break
    if proc.poll() is None:
        try: os.killpg(proc.pid, signal.SIGTERM)
        except ProcessLookupError: pass
        try: proc.wait(timeout=20)
        except subprocess.TimeoutExpired:
            try: os.killpg(proc.pid, signal.SIGKILL)
            except ProcessLookupError: pass
            proc.wait()
    return result

ROOT.mkdir(parents=True, exist_ok=True)
(REPO/'compatibility').mkdir(exist_ok=True)
if not PLUGIN.exists(): raise SystemExit(f'Missing built plugin: {PLUGIN}')
for j in (8,16,17): install_runtime(j)
paper_meta = get_json('https://fill.papermc.io/v3/projects/paper')
versions = sorted({v for group in paper_meta['versions'].values() for v in group
                   if '-' not in v and (1,14,4) <= key(v) <= (26,2)}, key=key)
purpur_versions = set(v for v in get_json('https://api.purpurmc.org/v2/purpur')['versions']
                       if '-' not in v and (1,14,4) <= key(v) <= (26,2))
results = {v:{'Paper':'NOT RUN','Purpur':'NOT PUBLISHED' if v not in purpur_versions else 'NOT RUN',
              'CraftBukkit':'NOT RUN','Spigot':'NOT RUN'} for v in versions}
if RESULTS.exists():
    old=json.loads(RESULTS.read_text())
    for v,row in old.items():
        if v in results:
            for k,s in row.items():
                if s not in ('NOT RUN','NOT PUBLISHED'): results[v][k]=s
save()
print(f'{len(versions)} Paper releases, {len(purpur_versions)} Purpur releases',flush=True)

for v in versions:
    if results[v]['Paper']=='NOT RUN':
        try:
            builds=get_json(f'https://fill.papermc.io/v3/projects/paper/versions/{v}/builds')
            usable=[b for b in builds if 'server:default' in b.get('downloads',{})]
            stable=[b for b in usable if b.get('channel')=='STABLE']
            chosen=max(stable or usable,key=lambda b:b['id']) if usable else None
            if not chosen: results[v]['Paper']='UNAVAILABLE: no build'
            else:
                item=chosen['downloads']['server:default']; jar=ROOT/'jars'/f'paper-{v}.jar'
                if not jar.exists(): download(item['url'],jar)
                results[v]['Paper']=run_server('paper',v,jar)
                if not stable and results[v]['Paper']=='PASS': results[v]['Paper'] += ' (alpha build)'
        except Exception as e: results[v]['Paper']=f'UNAVAILABLE: {type(e).__name__}: {e}'
        print('Paper',v,results[v]['Paper'],flush=True); save()
    if v in purpur_versions and results[v]['Purpur']=='NOT RUN':
        try:
            jar=ROOT/'jars'/f'purpur-{v}.jar'
            if not jar.exists(): download(f'https://api.purpurmc.org/v2/purpur/{v}/latest/download',jar)
            results[v]['Purpur']=run_server('purpur',v,jar)
        except Exception as e: results[v]['Purpur']=f'UNAVAILABLE: {type(e).__name__}: {e}'
        print('Purpur',v,results[v]['Purpur'],flush=True); save()

bt=ROOT/'buildtools'; bt.mkdir(exist_ok=True)
btjar=ROOT/'BuildTools.jar'
if not btjar.exists(): download('https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar',btjar)
for v in versions:
    if results[v]['CraftBukkit'].startswith('PASS') and results[v]['Spigot'].startswith('PASS'): continue
    try:
        log=bt/f'buildtools-{v}.log'
        java=str(buildtools_java_for(v))
        if not results[v]['Spigot'].startswith('PASS'):
            with log.open('w') as out:
                cp=subprocess.run([java,'-Xmx3G','-jar',str(btjar),'--rev',v],
                                  cwd=bt,stdout=out,stderr=subprocess.STDOUT,timeout=2400)
            jar=bt/f'spigot-{v}.jar'
            if cp.returncode==0 and jar.exists(): results[v]['Spigot']=run_server('spigot',v,jar)
            else:
                tail=' | '.join(log.read_text(errors='replace').splitlines()[-6:])[-800:]
                results[v]['Spigot']=f'UNAVAILABLE: BuildTools could not create {jar.name}; {tail}'
            save()
        if not results[v]['CraftBukkit'].startswith('PASS'):
            with log.open('w') as out:
                cp=subprocess.run([java,'-Xmx3G','-jar',str(btjar),'--rev',v,'--compile','CRAFTBUKKIT'],
                                  cwd=bt,stdout=out,stderr=subprocess.STDOUT,timeout=2400)
            jar=bt/f'craftbukkit-{v}.jar'
            if cp.returncode==0 and jar.exists(): results[v]['CraftBukkit']=run_server('craftbukkit',v,jar)
            else:
                tail=' | '.join(log.read_text(errors='replace').splitlines()[-6:])[-800:]
                results[v]['CraftBukkit']=f'UNAVAILABLE: BuildTools could not create {jar.name}; {tail}'
    except subprocess.TimeoutExpired:
        results[v]['CraftBukkit']='UNAVAILABLE: BuildTools timeout'; results[v]['Spigot']='UNAVAILABLE: BuildTools timeout'
    except Exception as e:
        results[v]['CraftBukkit']=f'UNAVAILABLE: {type(e).__name__}: {e}'
        results[v]['Spigot']=f'UNAVAILABLE: {type(e).__name__}: {e}'
    print('CraftBukkit',v,results[v]['CraftBukkit'],'| Spigot',results[v]['Spigot'],flush=True); save()
print('MATRIX COMPLETE',flush=True)
