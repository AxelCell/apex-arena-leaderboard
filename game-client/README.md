# Game client (Apex Arena, Python + pygame)

The game lives in its own folder. These files document its integration with this service:

- `leaderboard_client.py` - copy it next to the game's `main.py`. Uses only the Python
  standard library (`urllib`, `threading`), so no new dependency.
- `main_py_integration.diff` - the changes made to the game's `main.py`.

What the integration does:

- Asks for a player name at launch (saved to `player_name.txt`).
- When a human finishes a **Quick Battle** or **League** fight against the AI, it POSTs the
  raw result (monster, difficulty, win/loss, HP left %, fight seconds) on a daemon thread.
  The server computes the points. 2P Versus and Watch mode are never submitted.
- Network errors and timeouts (3 s) are caught on the worker thread, so the game never
  freezes or crashes when the server is down; the result screen just says "offline".
- Adds a **LEADERBOARD** item to the title menu: Overall / Easy / Hard / Boss boards, plus
  the player's own rank.

Server address: environment variable `APEX_API_URL` (default `http://localhost:8080`).
