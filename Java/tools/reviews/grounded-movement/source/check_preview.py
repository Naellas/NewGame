"""Local Chrome smoke check for the embedded movement study; no packages needed."""
import base64
import http.server
import json
import os
from pathlib import Path
import socket
import struct
import subprocess
import tempfile
import threading
import time
import urllib.request
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[5]
SCREENSHOTS = ROOT / 'Java/temp/review-checks/movement'
SCREENSHOTS.mkdir(parents=True, exist_ok=True)
class QuietHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT), **kwargs)
    def log_message(self, *args):
        pass

server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), QuietHandler)
threading.Thread(target=server.serve_forever, daemon=True).start()
errors = []
contexts = []
with tempfile.TemporaryDirectory(prefix='alderfall-preview-') as profile:
    browser = subprocess.Popen([
        r'C:\Program Files\Google\Chrome\Application\chrome.exe', '--headless=new',
        '--disable-gpu', '--in-process-gpu', '--no-sandbox', '--no-first-run',
        '--remote-allow-origins=*', '--remote-debugging-port=0',
        '--user-data-dir=' + profile, 'about:blank'
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
       creationflags=subprocess.CREATE_NO_WINDOW)
    try:
        port_file = Path(profile) / 'DevToolsActivePort'
        for _ in range(100):
            if port_file.exists():
                break
            time.sleep(.1)
        port = port_file.read_text().splitlines()[0]
        pages = json.load(urllib.request.urlopen('http://127.0.0.1:' + port + '/json'))
        url = urlparse(next(p['webSocketDebuggerUrl'] for p in pages if p['type'] == 'page'))
        ws = socket.create_connection((url.hostname, url.port), timeout=15)
        key = base64.b64encode(os.urandom(16)).decode()
        ws.sendall((f'GET {url.path} HTTP/1.1\r\nHost: {url.netloc}\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: {key}\r\nSec-WebSocket-Version: 13\r\n\r\n').encode())
        handshake = b''
        while not handshake.endswith(b'\r\n\r\n'):
            handshake += ws.recv(1)
        assert b' 101 ' in handshake.split(b'\r\n')[0], handshake
        seq = 0
        def read(n):
            data = b''
            while len(data) < n:
                block = ws.recv(n - len(data))
                if not block:
                    raise EOFError()
                data += block
            return data
        def call(method, params=None):
            global seq
            seq += 1
            body = json.dumps({'id': seq, 'method': method, 'params': params or {}}).encode()
            mask = os.urandom(4)
            header = bytes([129, 128 | len(body)]) if len(body) < 126 else bytes([129, 254]) + struct.pack('!H', len(body))
            ws.sendall(header + mask + bytes(b ^ mask[i % 4] for i, b in enumerate(body)))
            while True:
                head = read(2); size = head[1] & 127
                if size == 126: size = struct.unpack('!H', read(2))[0]
                elif size == 127: size = struct.unpack('!Q', read(8))[0]
                response = json.loads(read(size))
                if response.get('method') == 'Runtime.exceptionThrown': errors.append(response)
                if response.get('method') == 'Runtime.executionContextCreated': contexts.append(response['params']['context'])
                if response.get('id') == seq:
                    assert 'error' not in response, response
                    return response.get('result', {})
        def evaluate(expression):
            result = call('Runtime.evaluate', {'expression': expression, 'returnByValue': True})
            assert 'exceptionDetails' not in result, result
            return result['result'].get('value')
        call('Runtime.enable')
        call('Emulation.setDeviceMetricsOverride', {'width': 1400, 'height': 1100, 'deviceScaleFactor': 1, 'mobile': False})
        address = f'http://127.0.0.1:{server.server_port}/Java/tools/reviews/characters/index.html#movement-physics'
        call('Page.navigate', {'url': address})
        time.sleep(1)
        child = "document.querySelector('#movement-study-frame').contentWindow"
        assert evaluate("document.querySelector('#tab-movement').getAttribute('aria-selected')==='true'")
        assert evaluate("document.querySelectorAll('[role=tab]').length===3 && !!document.querySelector('#tab-dialogue')")
        for actor in range(3):
            for cadence in (5,10,15):
                evaluate(f"(()=>{{const w={child};w.document.querySelector('#actor').value='{actor}';w.document.querySelector('#speed').value='{cadence}';w.reset();}})()")
                for _ in range(80):
                    time.sleep(.1)
                    if evaluate(f"!{child}.document.querySelector('#status').textContent.startsWith('Loading')"): break
                assert evaluate(f"!{child}.document.querySelector('#status').textContent.startsWith('Loading')"), (actor,cadence,errors)
        evaluate(f"(()=>{{const w={child};for(const id of ['bones','nodes','contacts'])w.document.getElementById(id).checked=true;const f=w.document.getElementById('frame');f.value='7';f.dispatchEvent(new Event('input'));}})()")
        time.sleep(.1)
        assert evaluate(f"{child}.document.getElementById('number').value==='8 / 32'")
        before=evaluate("document.querySelector('#movement-study-frame').offsetHeight")
        time.sleep(.2)
        assert evaluate("document.querySelector('#movement-study-frame').offsetHeight") == before
        evaluate(f"(()=>{{const w={child};for(const id of ['physics','colliders','nodes'])w.document.getElementById(id).checked=false;}})()")
        time.sleep(.1)
        assert evaluate(f"{child}.document.getElementById('base').toDataURL()!=={child}.document.getElementById('cloth').toDataURL()"), 'Disabling physics restored the original rig'
        held_frame=evaluate(f"{child}.document.getElementById('number').value")
        rig_only=evaluate(f"{child}.document.getElementById('cloth').toDataURL()")
        for toggle in ('bones','contacts','nodes','colliders','physics'):
            evaluate(f"{child}.document.getElementById('{toggle}').click()")
            time.sleep(.1)
            assert evaluate(f"{child}.document.getElementById('number').value")==held_frame, 'Toggle advanced paused frame'
            evaluate(f"{child}.document.getElementById('{toggle}').click()")
            time.sleep(.1)
            assert evaluate(f"{child}.document.getElementById('cloth').toDataURL()")==rig_only, 'Toggle changed restored pose'

        for route in ('cloth-physics','movement-study','movement-physics'):
            evaluate(f"location.hash='{route}'")
            time.sleep(.1)
            assert evaluate("!document.getElementById('movement-panel').hidden")
        evaluate("document.getElementById('tab-movement').dispatchEvent(new KeyboardEvent('keydown',{key:'ArrowRight',bubbles:true}))")
        assert evaluate("document.activeElement.id==='tab-dialogue' && !document.getElementById('dialogue-panel').hidden")
        evaluate("document.getElementById('tab-movement').click()")
        call('Emulation.setDeviceMetricsOverride', {'width':390,'height':844,'deviceScaleFactor':1,'mobile':False})
        time.sleep(.3)
        assert evaluate(f"{child}.document.documentElement.scrollWidth<={child}.innerWidth"), 'Mobile overflow'
        assert evaluate("document.documentElement.scrollWidth<=innerWidth"), 'Parent mobile overflow'
        call('Emulation.setDeviceMetricsOverride', {'width':1400,'height':1100,'deviceScaleFactor':1,'mobile':False})
        evaluate(f"(()=>{{const w={child};w.document.getElementById('physics').checked=true;w.document.getElementById('actor').value='0';w.document.getElementById('speed').value='10';w.reset();}})()")
        time.sleep(.2)
        screenshot=call('Page.captureScreenshot', {'format':'png'})
        (SCREENSHOTS/'preview-screenshot.png').write_bytes(base64.b64decode(screenshot['data']))
        call('Page.navigate', {'url':(ROOT/'Java/tools/reviews/characters/index.html').as_uri()+'#movement-physics'})
        time.sleep(1)
        child_id=call('Page.getFrameTree')['frameTree']['childFrames'][0]['frame']['id']
        context=next(c['id'] for c in reversed(contexts) if c.get('auxData',{}).get('frameId')==child_id and c['auxData'].get('isDefault'))
        local=call('Runtime.evaluate',{'contextId':context,'expression':"!document.querySelector('#status').textContent.startsWith('Loading')",'returnByValue':True})
        for _ in range(50):
            if local['result'].get('value'):break
            time.sleep(.1)
            local=call('Runtime.evaluate',{'contextId':context,'expression':"!document.querySelector('#status').textContent.startsWith('Loading')",'returnByValue':True})
        assert local['result'].get('value'), ('Local preview failed',local,call('Page.getFrameTree'),errors)
        # The old bookmark must preserve its tab while all content lives in tools.
        call('Page.navigate', {'url': (ROOT/'asset-review/characters/index.html').as_uri()+'#movement-physics'})
        time.sleep(.8)
        assert evaluate("location.pathname.includes('/Java/tools/reviews/characters/index.html') && location.hash==='#movement-physics'"), 'Old bookmark did not redirect'
        evaluate("document.querySelector('#tab-characters').click();group.value='party';mode.value='combat';refresh()")
        def loaded_gallery():
            for _ in range(80):
                if evaluate("items.filter(i=>!i.figure.hidden).every(i=>i.image.complete&&i.image.naturalWidth>0)"):
                    return
                time.sleep(.1)
            raise AssertionError(evaluate("items.filter(i=>!i.figure.hidden&&!i.image.naturalWidth).map(i=>i.image.src)"))
        loaded_gallery()
        assert evaluate("items.filter(i=>!i.figure.hidden).length===14"), 'Party combat coverage changed'
        for style in ('walk','walk_alternate','walk_articulated','walk_grounded','idle'):
            evaluate(f"mode.value='{style}';refresh()")
            loaded_gallery()
        evaluate("group.value='monsters';mode.value='combat';refresh()")
        loaded_gallery()
        assert evaluate("items.filter(i=>!i.figure.hidden).length===58"), 'Monster coverage changed'
        call('Page.navigate', {'url': f'http://127.0.0.1:{server.server_port}/Java/tools/reviews/movement-physics/calibration/index.html'})
        time.sleep(.7)
        assert evaluate("calibrationResults.length===1016 && calibrationSummary.actors===127"), 'Incomplete calibration inventory'
        assert evaluate("document.getElementById('actor').options.length===127 && document.querySelectorAll('#rows tr').length===8")
        evaluate("document.getElementById('actor').value='npc_calder';refresh()")
        time.sleep(.2)
        assert evaluate("document.getElementById('sheet').complete && document.getElementById('sheet').naturalWidth===1000"), 'Calibration sheet did not load'
        evaluate("document.getElementById('filter').value='fallback';filter()")
        assert evaluate("document.getElementById('actor').options.length===113"), 'Fallback classification changed'
        evaluate("document.getElementById('filter').value='fix';filter()")
        assert evaluate("document.getElementById('actor').options.length===new Set(calibrationResults.filter(r=>r.status!=='checks-pass'||r.physics!=='checks-pass').map(r=>r.actor)).size"), 'Failure filter lost flagged cases'
        call('Emulation.setDeviceMetricsOverride', {'width':390,'height':844,'deviceScaleFactor':1,'mobile':False})
        time.sleep(.1)
        assert evaluate("document.documentElement.scrollWidth<=innerWidth"), 'Calibration mobile overflow'
        assert not errors, errors
        print('PASS: unified simulator, 9 actor/cadence combinations, overlays, scrub, independent physics and guide toggles at a fixed frame, legacy routes, dialogue tab, mobile, stable height, local files, full-roster calibration inventory and filters, no JS exceptions.')
        ws.close()
    finally:
        browser.terminate()
        browser.wait(timeout=10)
        server.shutdown()
