#!/usr/bin/env python3
"""MCP Client for CaDoodle Application - Connects to TCP port 8080"""

import socket
import json
import uuid

SERVER_HOST = "127.0.0.1"
SERVER_PORT = 8080

class CaDoodleMCPClient:
    def __init__(self, host=SERVER_HOST, port=SERVER_PORT):
        self.host = host
        self.port = port
        self.socket = None
        
    def connect(self):
        """Connect to the MCP server."""
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect((self.host, self.port))
        print(f"Connected to MCP server at {self.host}:{self.port}")
        
    def disconnect(self):
        """Close the connection."""
        if self.socket:
            self.socket.close()
            print("Disconnected from MCP server")
            
    def send_request(self, method, params=None):
        """Send a JSON-RPC 2.0 request and wait for response."""
        if not self.socket:
            raise Exception("Not connected to server")
            
        request = {
            "jsonrpc": "2.0",
            "method": method,
            "params": params or {},
            "id": str(uuid.uuid4())
        }
        
        # Send request with newline termination
        message = json.dumps(request) + "\n"
        self.socket.sendall(message.encode('utf-8'))
        
        # Receive response (server doesn't send newline, so read until EOF)
        response_data = b""
        self.socket.settimeout(5.0)
        try:
            while True:
                chunk = self.socket.recv(4096)
                if not chunk:
                    break
                response_data += chunk
                # Try to parse JSON to detect completion
                try:
                    json.loads(response_data)
                    break
                except json.JSONDecodeError:
                    continue
        except socket.timeout:
            pass  # Continue with whatever we have
            
        response = response_data.decode('utf-8')
        print(f"Response: {response}")
        
        return json.loads(response)
    
    def initialize(self):
        """Initialize the connection."""
        return self.send_request("initialize")
    
    def get_current_state(self):
        """Get current state of the CaDoodleFile."""
        return self.send_request("state.getCurrent")
    
    def get_selected(self):
        """Get currently selected CSG names."""
        return self.send_request("state.getSelected")
    
    def get_csgs(self):
        """Get list of CSGs with details."""
        return self.send_request("state.getCSGs")
    
    def add_operation(self, operation_type, params=None):
        """Add an operation to the CaDoodleFile."""
        if params is None:
            params = {}
        params["operationType"] = operation_type
        return self.send_request("operations.add", params)
    
    def select_csgs(self, names):
        """Select CSGs by names."""
        return self.send_request("state.select", {"names": names})
    
    def get_parameters(self, csg_name):
        """Get parameters for a specific CSG."""
        return self.send_request("csg.getParameters", {"csgName": csg_name})
    
    def get_shapes_palette(self):
        """Get shapes palette."""
        return self.send_request("shapes.getPalette")


# Example usage
if __name__ == "__main__":
    client = CaDoodleMCPClient()
    
    try:
        client.connect()
        
        # Initialize
        print("\n=== Initialize ===")
        result = client.initialize()
        print(json.dumps(result, indent=2))
        
        # Get current state
        print("\n=== Get Current State ===")
        result = client.get_current_state()
        print(json.dumps(result, indent=2))
        
        # Get shapes palette
        print("\n=== Get Shapes Palette ===")
        result = client.get_shapes_palette()
        print(json.dumps(result, indent=2))
        
        # Try adding a primitive
        print("\n=== Add a Box ===")
        result = client.add_operation("AddPrimitive", {
            "primitiveType": "box",
            "parameters": {
                "width": 10,
                "height": 10,
                "depth": 10
            }
        })
        print(json.dumps(result, indent=2))
        
        # Get updated state
        print("\n=== Get Updated State ===")
        result = client.get_current_state()
        print(json.dumps(result, indent=2))
        
    except Exception as e:
        print(f"Error: {e}")
    finally:
        client.disconnect()
