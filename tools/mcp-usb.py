#!/usr/bin/env python3
"""USB/MCP host bridge for Nothing Modes.

This script sends control commands to the debug build of Nothing Modes over
`adb` and reads JSON responses from the device's logcat. It lets an AI agent
list, get, save, run and delete modes while the phone is USB-connected.

Usage:
    python3 tools/mcp-usb.py list
    python3 tools/mcp-usb.py get <mode-id>
    python3 tools/mcp-usb.py run <mode-id>
    python3 tools/mcp-usb.py delete <mode-id>
    python3 tools/mcp-usb.py save < mode.json
"""

import argparse
import json
import re
import subprocess
import sys
import time
from pathlib import Path

PACKAGE = "com.tdvorak.nothingmodes.debug"
RECEIVER = "com.tdvorak.nothingmodes.agent.ModeControlReceiver"
TAG = "NothingMcp"


def adb(args: list[str]) -> str:
    cmd = ["adb", *args]
    return subprocess.check_output(cmd, text=True, stderr=subprocess.STDOUT)


def send_command(command: str, id_: str = "", json_payload: str = "") -> None:
    extras = ["--es", "command", command]
    if id_:
        extras += ["--es", "id", id_]
    if json_payload:
        extras += ["--es", "json", json_payload]
    adb(["shell", "am", "broadcast", "-n", f"{PACKAGE}/{RECEIVER}", *extras])


def read_response(timeout: float = 5.0) -> dict | None:
    start = time.time()
    deadline = start + timeout
    while time.time() < deadline:
        out = adb(["logcat", "-d", "-s", f"{TAG}:I"])
        for line in reversed(out.splitlines()):
            # Log format: MM-DD HH:MM:SS.mmm  PID  TID I TAG: <json>
            if TAG in line and ("ok" in line or "detail" in line):
                match = re.search(rf"{re.escape(TAG)}:\s*(.+)$", line)
                if match:
                    try:
                        return json.loads(match.group(1))
                    except json.JSONDecodeError:
                        continue
        time.sleep(0.3)
    return None


def load_json_file(path: str) -> str:
    return Path(path).read_text()


def main() -> int:
    parser = argparse.ArgumentParser(description="USB/MCP bridge for Nothing Modes")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("list", help="List all modes")
    get = sub.add_parser("get", help="Get one mode")
    get.add_argument("id")
    run = sub.add_parser("run", help="Run a manual mode")
    run.add_argument("id")
    delete = sub.add_parser("delete", help="Delete a mode")
    delete.add_argument("id")
    save = sub.add_parser("save", help="Save a mode from JSON on stdin")

    args = parser.parse_args()

    try:
        payload = ""
        if args.command == "save":
            payload = sys.stdin.read()

        send_command(args.command, getattr(args, "id", ""), payload)
        response = read_response()
        if response is None:
            print("No response from device. Is the debug build installed and USB debugging enabled?", file=sys.stderr)
            return 1
        print(json.dumps(response, indent=2))
        return 0 if response.get("ok") else 1
    except subprocess.CalledProcessError as e:
        print(f"adb failed: {e.output}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
