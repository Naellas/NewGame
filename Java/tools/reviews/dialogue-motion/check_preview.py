"""Local Chrome smoke check for the dialogue motion review; no packages needed."""
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

ROOT = Path(__file__).resolve().parents[4]
SCREENSHOTS = ROOT / 'Java/temp/review-checks/dialogue'
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
        call('Emulation.setDeviceMetricsOverride', {'width': 1200, 'height': 1000, 'deviceScaleFactor': 1, 'mobile': False})
        call('Page.navigate', {'url': (ROOT / 'Java/tools/reviews/dialogue-motion/index.html').as_uri()})
        time.sleep(1)
        assert evaluate("dialogueRoster.length===26"), 'Missing actors'
        evaluate("pause()")
        for index in range(26):
            evaluate(f"actor.selectedIndex={index};load();pause()")
            for attempt in range(40):
                if evaluate("Object.keys(images).length===3"): break
                time.sleep(.1)
            assert evaluate("Object.keys(images).length===3"), f'Missing images: {index}'
            evaluate("slider.value=20;slider.dispatchEvent(new Event('input'));draw()")
            assert evaluate("document.querySelector('#number').value==='21 / 96'")
            assert evaluate("images.body.naturalWidth===3840 && images.face.naturalHeight===2640")
        evaluate("actor.value='npc_aria_dialogue_sprite';load();pause()")
        time.sleep(.4)
        evaluate("slider.value=20;slider.dispatchEvent(new Event('input'));document.querySelector('#guides').click()")
        evaluate("document.querySelector('#next').click()")
        assert evaluate("Number(slider.value)===21")
        evaluate("document.querySelector('#back').click()")
        assert evaluate("Number(slider.value)===20")
        assert evaluate("dialogueRoster.filter(r=>r.parts.length>0).length===3 && dialogueRoster.find(r=>r.id===actor.value).lips.length===6")
        evaluate("document.querySelector('#physics').click()")
        for attempt in range(40):
            if evaluate("Object.keys(images).length===3"): break
            time.sleep(.1)
        assert evaluate("Number(slider.value)===20 && images.body.src.includes('-rig-body.png')"), 'Physics off changed frame or rig'
        evaluate("document.querySelector('#physics').click()")
        for attempt in range(40):
            if evaluate("Object.keys(images).length===3"): break
            time.sleep(.1)
        assert evaluate("Number(slider.value)===20 && !images.body.src.includes('-rig-body.png')"), 'Physics round trip failed'
        screenshot = call('Page.captureScreenshot', {'format': 'png'})
        (SCREENSHOTS / 'preview-screenshot.png').write_bytes(base64.b64decode(screenshot['data']))
        evaluate("actor.value='npc_vesper_dialogue_sprite';load();pause();view.value='body'")
        for attempt in range(40):
            if evaluate("Object.keys(images).length===3"): break
            time.sleep(.1)
        assert evaluate("Object.keys(images).length===3"), 'Vesper native-size preview missing'
        evaluate("slider.value=40;slider.dispatchEvent(new Event('input'));draw()")
        call('Emulation.setDeviceMetricsOverride', {'width': 1200, 'height': 1400, 'deviceScaleFactor': 1, 'mobile': False})
        screenshot = call('Page.captureScreenshot', {'format': 'png'})
        (SCREENSHOTS / 'sharpness-screenshot.png').write_bytes(base64.b64decode(screenshot['data']))
        evaluate("view.value='body';view.dispatchEvent(new Event('change'))")
        assert evaluate("document.querySelector('#motion').height===800")
        call('Emulation.setDeviceMetricsOverride', {'width': 390, 'height': 844, 'deviceScaleFactor': 1, 'mobile': True})
        time.sleep(.2)
        assert evaluate("document.documentElement.scrollWidth<=390"), 'Mobile overflow'
        evaluate("document.querySelector('#play').click()")
        before=evaluate("Number(slider.value)")
        time.sleep(.25)
        assert evaluate("Number(slider.value)")!=before, 'Playback did not advance'
        call('Emulation.setDeviceMetricsOverride', {'width': 1200, 'height': 1100, 'deviceScaleFactor': 1, 'mobile': False})
        main = (ROOT / 'Java/tools/reviews/characters/index.html').as_uri()
        call('Page.navigate', {'url': main + '#characters'})
        time.sleep(.8)
        assert evaluate("!document.querySelector('#dialogue-study-frame').hasAttribute('src')"), 'Preview not lazy loaded'
        evaluate("document.querySelector('#tab-dialogue').click()")
        time.sleep(.8)
        assert evaluate("location.hash==='#dialogue-motion' && !document.querySelector('#dialogue-panel').hidden")
        tree = call('Page.getFrameTree')['frameTree']
        child_id = next(f['frame']['id'] for f in tree['childFrames'] if 'dialogue-motion' in f['frame']['url'])
        context = next(c['id'] for c in reversed(contexts) if c.get('auxData', {}).get('frameId') == child_id and c['auxData'].get('isDefault'))
        def child(expression):
            result = call('Runtime.evaluate', {'contextId': context, 'expression': expression, 'returnByValue': True})
            assert 'exceptionDetails' not in result, result
            return result['result'].get('value')
        assert child("embedded && Object.keys(images).length===3"), 'Embedded preview failed to load'
        child("pause();slider.value=18;slider.dispatchEvent(new Event('input'))")
        child("view.value='face';view.dispatchEvent(new Event('change'))")
        time.sleep(.2)
        height = evaluate("document.querySelector('#dialogue-study-frame').offsetHeight")
        child("view.value='body';view.dispatchEvent(new Event('change'))")
        time.sleep(.2)
        assert evaluate("document.querySelector('#dialogue-study-frame').offsetHeight") > height, 'Frame did not grow'
        child("view.value='face';view.dispatchEvent(new Event('change'))")
        time.sleep(.2)
        assert evaluate("document.querySelector('#dialogue-study-frame').offsetHeight") == height, 'Frame did not shrink'
        child("document.querySelector('#play').click()")
        evaluate("document.querySelector('#tab-characters').click()")
        time.sleep(.15)
        before = child("position")
        time.sleep(.2)
        assert child("position") == before, 'Hidden preview kept playing'
        evaluate("document.querySelector('#tab-dialogue').click()")
        time.sleep(.2)
        assert child("position") != before, 'Preview failed to resume'
        child("pause()")
        evaluate("document.querySelector('#tab-dialogue').dispatchEvent(new KeyboardEvent('keydown',{key:'Home',bubbles:true}))")
        assert evaluate("document.activeElement.id==='tab-characters'")
        evaluate("document.querySelector('#tab-characters').dispatchEvent(new KeyboardEvent('keydown',{key:'End',bubbles:true}))")
        assert evaluate("document.activeElement.id==='tab-dialogue'")
        screenshot = call('Page.captureScreenshot', {'format': 'png'})
        (SCREENSHOTS / 'embedded-screenshot.png').write_bytes(base64.b64decode(screenshot['data']))
        call('Emulation.setDeviceMetricsOverride', {'width': 390, 'height': 844, 'deviceScaleFactor': 1, 'mobile': True})
        time.sleep(.2)
        assert evaluate("document.documentElement.scrollWidth<=390"), 'Parent mobile overflow'
        assert child("document.documentElement.scrollWidth<=innerWidth"), 'Embedded mobile overflow'
        call('Page.navigate', {'url': main + '#dialogue-motion'})
        time.sleep(.6)
        assert evaluate("document.querySelector('#tab-dialogue').getAttribute('aria-selected')==='true' && document.querySelector('#dialogue-study-frame').hasAttribute('src')"), 'Deep link failed'
        assert not errors, errors
        print('PASS: standalone 26-actor review; integrated local-file tab, lazy loading, resizing, hidden pause/resume, keyboard navigation, mobile layout and deep link; no JS exceptions.')
        ws.close()
    finally:
        browser.terminate()
        browser.wait(timeout=10)
        server.shutdown()
