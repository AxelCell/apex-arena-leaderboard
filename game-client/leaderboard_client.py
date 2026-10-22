"""
Talks to the Apex Arena leaderboard service (a Spring Boot app).

Everything here runs on background daemon threads and swallows network errors,
so the game never freezes or crashes when the server is slow or down.
Only the standard library is used (urllib), no extra pip install.

Server address: set APEX_API_URL (default http://localhost:8080).
"""
import json
import os
import threading
import time
import urllib.error
import urllib.parse
import urllib.request

API_URL = os.environ.get("APEX_API_URL", "http://localhost:8080").rstrip("/")
TIMEOUT = 3.0
NAME_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "player_name.txt")

BOARDS = [("OVERALL", {}), ("EASY", {"difficulty": "easy"}),
          ("HARD", {"difficulty": "hard"}), ("BOSS", {"difficulty": "boss"})]


def load_name():
    try:
        with open(NAME_FILE, encoding="utf-8") as f:
            return f.read().strip()[:16]
    except OSError:
        return ""


def save_name(name):
    try:
        with open(NAME_FILE, "w", encoding="utf-8") as f:
            f.write(name)
    except OSError:
        pass


def valid_name(name):
    return 3 <= len(name) <= 16 and all(c.isascii() and (c.isalnum() or c == "_") for c in name)


def _request(method, path, body=None):
    """One HTTP call. Returns (status, parsed_json_or_None). Raises on network failure."""
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(API_URL + path, data=data, method=method)
    req.add_header("Accept", "application/json")
    if data is not None:
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
            raw = resp.read()
            return resp.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:   # server answered with 4xx/5xx
        try:
            return e.code, json.loads(e.read() or b"null")
        except ValueError:
            return e.code, None


def _background(fn, *args):
    threading.Thread(target=fn, args=args, daemon=True).start()


class Client:
    """Shared state the game reads every frame; worker threads only assign whole values."""

    def __init__(self):
        self.last_submit = None   # dict(state="sending"|"ok"|"error", points=.., rank=.., msg=..)
        self.boards = {}          # board label -> dict(state=.., entries=[..], me=.., at=time)

    # ---- POST /api/scores
    def submit(self, player, monster, difficulty, mode, won, hp_pct, fight_seconds):
        body = dict(playerName=player, monster=monster, difficulty=difficulty, mode=mode,
                    won=bool(won), hpLeftPercent=int(hp_pct), fightSeconds=round(float(fight_seconds), 2))
        status = dict(state="sending")
        self.last_submit = status
        _background(self._submit, body, status)

    def _submit(self, body, status):
        try:
            code, data = _request("POST", "/api/scores", body)
            if code == 201 and data:
                status.update(state="ok", points=data.get("points"), rank=data.get("overallRank"))
            else:
                msg = (data or {}).get("detail") or ("server error %d" % code)
                status.update(state="error", msg=msg)
        except Exception:   # offline, timeout, bad JSON... never let it reach the game loop
            status.update(state="error", msg="leaderboard offline - score not saved")
        for b in list(self.boards.values()):   # board screens should refetch after a new score
            b["at"] = 0

    # ---- GET /api/leaderboard/top + GET /api/players/{name}/rank
    def fetch_board(self, label, player, force=False):
        cur = self.boards.get(label)
        if cur and not force and (cur["state"] == "loading" or time.time() - cur["at"] < 10):
            return
        entry = dict(state="loading", entries=(cur or {}).get("entries", []), me=None, at=time.time())
        self.boards[label] = entry
        _background(self._fetch, label, player, entry)

    def _fetch(self, label, player, entry):
        params = dict(BOARDS)[label]
        try:
            q = urllib.parse.urlencode(dict(params, n=10))
            code, data = _request("GET", "/api/leaderboard/top?" + q)
            if code != 200 or not data:
                raise ValueError(code)
            me = None
            if player:
                q = ("?" + urllib.parse.urlencode(params)) if params else ""
                c2, d2 = _request("GET", "/api/players/%s/rank%s" % (urllib.parse.quote(player), q))
                me = d2 if c2 == 200 else None
            entry.update(state="ok", entries=data.get("entries", []), me=me, at=time.time())
        except Exception:
            entry.update(state="error", at=time.time())
