"""Serves a canned Open-Meteo 15-minute forecast for scripts/e2e/rain_alerts.sh.

Usage: python3 nowcast_server.py <port> <state-dir>

Each request reads the scenario name from <state-dir>/scenario and answers with 8 slots of 15
minutes, starting with the current one, in UTC (utc_offset_seconds 0). Times are computed per
request, so "snow in 60 minutes" stays true however long the check takes. Every request is
appended to <state-dir>/requests.log with its query and Cache-Control header.

Scenarios:
  dry           overcast throughout
  raining-now   moderate rain in the current slot
  snow-in-60    overcast, then moderate snow from the slot 60 minutes after the current one
"""
import json
import sys
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

PORT = int(sys.argv[1])
STATE = Path(sys.argv[2])
SLOTS = 8
OVERCAST, MODERATE_RAIN, MODERATE_SNOW = 3, 63, 73


def forecast(scenario):
    now = datetime.now(timezone.utc)
    start = now.replace(minute=now.minute - now.minute % 15, second=0, microsecond=0)
    times = [(start + timedelta(minutes=15 * i)).strftime("%Y-%m-%dT%H:%M") for i in range(SLOTS)]
    codes = [OVERCAST] * SLOTS
    snowfall = [0.0] * SLOTS
    if scenario == "raining-now":
        codes[0] = MODERATE_RAIN
    elif scenario == "snow-in-60":
        for i in range(4, SLOTS):
            codes[i] = MODERATE_SNOW
            snowfall[i] = 0.4
    elif scenario != "dry":
        raise ValueError(f"unknown scenario {scenario!r}")
    return {
        "latitude": 51.5,
        "longitude": -0.12,
        "utc_offset_seconds": 0,
        "timezone": "GMT",
        "minutely_15_units": {"time": "iso8601", "weather_code": "wmo code", "snowfall": "cm"},
        "minutely_15": {"time": times, "weather_code": codes, "snowfall": snowfall},
    }


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        scenario = (STATE / "scenario").read_text().strip()
        with open(STATE / "requests.log", "a") as log:
            log.write(f"{scenario} {self.path} cache-control={self.headers.get('Cache-Control')}\n")
        if "minutely_15" not in self.path:
            self.send_error(404, "only the 15-minute forecast is served here")
            return
        body = json.dumps(forecast(scenario)).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args):
        pass


HTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
