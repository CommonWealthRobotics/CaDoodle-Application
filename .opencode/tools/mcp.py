#!/usr/bin/env python3
"""
MCP Client for CaDoodle Application - Tool implementation
Usage: python3 mcp_tool.py <command> [args...]
Commands: initialize, get_current_state, get_selected, get_csgs, select_csgs, add_operation, remove_operation, regenerate, get_parameters, set_bounds, get_shapes_palette
"""

import socket
import json
import uuid
import sys
import os

SERVER_HOST = "127.0.0.1"
SERVER_PORT = 8080
TIMEOUT = 30

class CaDoodleMCPClient:
    def __init__(self, host=SERVER_HOST, port=SERVER_PORT):
        self.host = host
        self.port = port
        self.socket = None
        
    def connect(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.settimeout(TIMEOUT)
        self.socket.connect((self.host, self.port))
        
    def disconnect(self):
        if self.socket:
            self.socket.close()
            self.socket = None
            
    def send_request(self, method, params=None):
        if not self.socket:
            raise Exception("Not connected to server")
            
        request = {
            "jsonrpc": "2.0",
            "method": method,
            "params": params or {},
            "id": str(uuid.uuid4())
        }
        
        message = json.dumps(request) + "\n"
        self.socket.sendall(message.encode('utf-8'))
        
        response_data = b""
        try:
            while True:
                chunk = self.socket.recv(4096)
                if not chunk:
                    break
                response_data += chunk
                try:
                    json.loads(response_data)
                    break
                except json.JSONDecodeError:
                    continue
        except socket.timeout:
            pass
            
        response = response_data.decode('utf-8')
        return json.loads(response)


def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "No command provided"}))
        sys.exit(1)
        
    command = sys.argv[1]
    args = sys.argv[2:]
    client = None
    
    try:
        client = CaDoodleMCPClient()
        client.connect()
        
        if command == "initialize":
            result = client.send_request("initialize")
            print(json.dumps(result))
            
        elif command == "get_current_state":
            result = client.send_request("state.getCurrent")
            print(json.dumps(result))
            
        elif command == "get_selected":
            result = client.send_request("state.getSelected")
            print(json.dumps(result))
            
        elif command == "get_csgs":
            result = client.send_request("state.getCSGs")
            print(json.dumps(result))
            
        elif command == "select_csgs":
            if len(args) < 1:
                raise Exception("Missing names argument")
            names = json.loads(args[0])
            result = client.send_request("state.select", {"names": names})
            print(json.dumps(result))
            
        elif command == "add_operation":
            if len(args) < 2:
                raise Exception("Missing operation_type and params arguments")
            operation_type = args[0]
            params = json.loads(args[1])
            params["operationType"] = operation_type
            result = client.send_request("operations.add", params)
            print(json.dumps(result))
            
        elif command == "remove_operation":
            if len(args) < 1:
                raise Exception("Missing operation_id argument")
            operation_id = args[0]
            result = client.send_request("operations.remove", {"operationId": operation_id})
            print(json.dumps(result))
            
        elif command == "regenerate":
            source_operation_id = args[0] if len(args) >= 1 else None
            params = {}
            if source_operation_id:
                params["sourceOperationId"] = source_operation_id
            result = client.send_request("operations.regenerate", params)
            print(json.dumps(result))
            
        elif command == "get_parameters":
            if len(args) < 1:
                raise Exception("Missing csg_name argument")
            csg_name = args[0]
            result = client.send_request("csg.getParameters", {"csgName": csg_name})
            print(json.dumps(result))
            
        elif command == "set_bounds":
            if len(args) < 7:
                raise Exception("Missing bounds arguments")
            csg_name = args[0]
            try:
                minX = float(args[1])
                minY = float(args[2])
                minZ = float(args[3])
                maxX = float(args[4])
                maxY = float(args[5])
                maxZ = float(args[6])
            except ValueError:
                raise Exception("Invalid numeric value in bounds parameters")
            bounds = {
                "csgName": csg_name,
                "minX": minX,
                "minY": minY,
                "minZ": minZ,
                "maxX": maxX,
                "maxY": maxY,
                "maxZ": maxZ
            }
            result = client.send_request("csg.setBounds", bounds)
            print(json.dumps(result))
            
        elif command == "get_shapes_palette":
            result = client.send_request("shapes.getPalette")
            print(json.dumps(result))
            
        elif command == "add_shape_by_name":
            if len(args) < 1:
                raise Exception("Missing name argument")
            name = args[0]
            result = client.send_request("shapes.addByName", {"name": name})
            print(json.dumps(result))
            
        else:
            print(json.dumps({"error": f"Unknown command: {command}"}))
            
    except Exception as e:
        print(json.dumps({"error": str(e)}))
        sys.exit(1)
    finally:
        if client:
            client.disconnect()


if __name__ == "__main__":
    main()
