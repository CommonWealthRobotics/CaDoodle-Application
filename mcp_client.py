#!/usr/bin/env python3
"""Minimal Streamable HTTP MCP client for CaDoodle (http://127.0.0.1:8080/mcp)."""

import json
import uuid
import urllib.request

MCP_URL = "http://127.0.0.1:8080/mcp"


def rpc(method, params=None, req_id=None):
    body = {
        "jsonrpc": "2.0",
        "method": method,
        "params": params or {},
    }
    if req_id is not None:
        body["id"] = req_id
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        MCP_URL,
        data=data,
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json, text/event-stream",
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        raw = resp.read().decode("utf-8")
        return json.loads(raw) if raw else None


def main():
    init = rpc(
        "initialize",
        {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {"name": "mcp_client.py", "version": "1.0"},
        },
        req_id=str(uuid.uuid4()),
    )
    print("initialize:", json.dumps(init, indent=2)[:500])
    rpc("notifications/initialized")
    tools = rpc("tools/list", req_id=str(uuid.uuid4()))
    names = [t["name"] for t in tools["result"]["tools"]]
    print("tools:", names)
    state = rpc("tools/call", {"name": "get_current_state", "arguments": {}}, req_id=str(uuid.uuid4()))
    print("state:", json.dumps(state, indent=2)[:500])


if __name__ == "__main__":
    main()
