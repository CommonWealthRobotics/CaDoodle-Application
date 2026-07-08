"""
MCP Server Skill for CaDoodle Application
Provides tools to interact with the CaDoodle MCP server over TCP.
"""

import socket
import json
import uuid
import subprocess
from typing import Any, Dict, List, Optional

SERVER_HOST = "127.0.0.1"
SERVER_PORT = 8080
TIMEOUT = 30

class CaDoodleMCPClient:
    """Simple JSON-RPC 2.0 TCP client for CaDoodle MCP server."""
    
    def __init__(self, host: str = SERVER_HOST, port: int = SERVER_PORT):
        self.host = host
        self.port = port
        self.socket = None
        
    def connect(self):
        """Connect to the MCP server."""
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.settimeout(TIMEOUT)
        self.socket.connect((self.host, self.port))
        
    def disconnect(self):
        """Close the connection."""
        if self.socket:
            self.socket.close()
            self.socket = None
            
    def send_request(self, method: str, params: Optional[Dict] = None) -> Dict:
        """Send a JSON-RPC 2.0 request and wait for response."""
        if not self.socket:
            raise ConnectionError("Not connected to server")
            
        request = {
            "jsonrpc": "2.0",
            "method": method,
            "params": params or {},
            "id": str(uuid.uuid4())
        }
        
        message = json.dumps(request) + "\n"
        self.socket.sendall(message.encode('utf-8'))
        
        response_data = b""
        while True:
            chunk = self.socket.recv(4096)
            if not chunk:
                break
            response_data += chunk
            if b"\n" in response_data:
                break
                
        response = response_data.strip().decode('utf-8')
        return json.loads(response)


def mcp_initialize() -> Dict[str, Any]:
    """Initialize connection to the MCP server.
    
    Returns:
        Dict with server information including protocolVersion, serverName, serverVersion
    """
    client = CaDoodleMCPClient()
    try:
        client.connect()
        return client.send_request("initialize")
    finally:
        client.disconnect()


def mcp_get_current_state() -> Dict[str, Any]:
    """Get current state of the CaDoodleFile.
    
    Returns:
        Dict with current state including operationCount, currentOperationIndex, projectName, csgs
    """
    client = CaDoodleMCPClient()
    try:
        client.connect()
        return client.send_request("state.getCurrent")
    finally:
        client.disconnect()


def mcp_get_selected() -> Dict[str, Any]:
    """Get currently selected CSG names.
    
    Returns:
        Dict with selected list of CSG names
    """
    client = CaDoodleMCPClient()
    try:
        client.connect()
        return client.send_request("state.getSelected")
    finally:
        client.disconnect()


def mcp_get_csgs() -> Dict[str, Any]:
    """Get list of all CSGs with details.
    
    Returns:
        Dict with success status and csgs array containing name, isGroup, bounds, etc.
    """
    client = CaDoodleMCPClient()
    try:
        client.connect()
        return client.send_request("state.getCSGs")
    finally:
        client.disconnect()


def mcp_select_csgs(names: List[str]) -> Dict[str, Any]:
    """Select CSGs by their names.
    
    Args:
        names: List of CSG names to select
        
    Returns:
        Dict with success status and selected names
    """
    client = CaDoodleMCPClient()
    try:
        client.connect()
        return client.send_request("state.select", {"names": names})
    finally:
        client.disconnect()


def mcp_add_operation(operation_type: str, params: Optional[Dict] = None) -> Dict[str, Any]:
    """Add an operation to the CaDoodleFile.
    
    Args:
        operation_type: Type of operation (e.g., "Align", "Delete", "ExtrudeSurface", "Fillet", "Group", "Hide", "LinearDistribution", "Lock", "Mirror", "Paste", "RadialDistribution", "Resize", "SetMaterial", "Show", "ToHole", "ToSolid", "UnGroup", "UnLock", "WireMeshView")
        params: Operation-specific parameters
        
    Returns:
        Dict with success status and addedNames
    """
    if params is None:
        params = {}
    params["operationType"] = operation_type
    
    client = CaDoodleMCPClient()
    try:
        client.connect()
        return client.send_request("operations.add", params)
    finally:
        client.disconnect()


def mcp_remove_operation(operation_id: str) -> Dict[str, Any]:
    """Remove an operation from the CaDoodleFile.
    
    Args:
        operation_id: ID of the operation to remove
        
    Returns:
        Dict with success status and message
    """
    client = CaDoodleMCPClient()
    try:
        client.connect()
        return client.send_request("operations.remove", {"operationId": operation_id})
    finally:
        client.disconnect()


def mcp_regenerate(source_operation_id: Optional[str] = None) -> Dict[str, Any]:
    """Regenerate operations from a source operation.
    
    Args:
        source_operation_id: Source operation ID (optional)
        
    Returns:
        Dict with success status and message
    """
    params = {}
    if source_operation_id:
        params["sourceOperationId"] = source_operation_id
        
    client = CaDoodleMCPClient()
    try:
        client.connect()
        return client.send_request("operations.regenerate", params)
    finally:
        client.disconnect()


def mcp_get_parameters(csg_name: str) -> Dict[str, Any]:
    """Get parameters for a specific CSG.
    
    Args:
        csg_name: Name of the CSG
        
    Returns:
        Dict with success status and parameters map
    """
    client = CaDoodleMCPClient()
    try:
        client.connect()
        return client.send_request("csg.getParameters", {"csgName": csg_name})
    finally:
        client.disconnect()


def mcp_set_bounds(
    csg_name: str,
    min_x: float,
    min_y: float,
    min_z: float,
    max_x: float,
    max_y: float,
    max_z: float
) -> Dict[str, Any]:
    """Set bounds for a specific CSG.
    
    Args:
        csg_name: Name of the CSG
        min_x: Minimum X coordinate
        min_y: Minimum Y coordinate
        min_z: Minimum Z coordinate
        max_x: Maximum X coordinate
        max_y: Maximum Y coordinate
        max_z: Maximum Z coordinate
        
    Returns:
        Dict with success status and message
    """
    client = CaDoodleMCPClient()
    try:
        client.connect()
        return client.send_request("csg.setBounds", {
            "csgName": csg_name,
            "minX": min_x,
            "minY": min_y,
            "minZ": min_z,
            "maxX": max_x,
            "maxY": max_y,
            "maxZ": max_z
        })
    finally:
        client.disconnect()


def mcp_get_shapes_palette() -> Dict[str, Any]:
    """Get shapes palette as structured JSON.
    
    Returns:
        Dict with success status and categories array
    """
    client = CaDoodleMCPClient()
    try:
        client.connect()
        return client.send_request("shapes.getPalette")
    finally:
        client.disconnect()
