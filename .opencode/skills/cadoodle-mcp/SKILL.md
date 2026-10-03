---
name: cadoodle-mcp
description: Connect to and interact with CaDoodle MCP server for 3D modeling operations
---

## What I do

I provide access to the CaDoodle 3D modeling application's MCP (Model Context Protocol) server. This allows you to:

- Initialize connections to the MCP server
- Query current state of the 3D scene (CSGs)
- Select, add, and manage 3D objects
- Perform operations like align, delete, extrude, fillet, group, hide, mirror, paste, etc.
- Get parameters for CSG objects
- Access the shapes palette

## When to use me

Use this skill when you need to:
- Create or modify 3D models using CaDoodle
- Query the current state of the 3D workspace
- Perform CAD operations programmatically
- Access the library of shapes and primitives

## How to use me

The MCP server runs on `localhost:8080` and communicates via JSON-RPC over TCP.

### Available operations:

1. **Connection**
   - `initialize` - Initialize connection to the server

2. **State queries**
   - `state.getCurrent` - Get current scene state
   - `state.getSelected` - Get selected CSG names
   - `state.getCSGs` - Get all CSGs with details

3. **Selection**
   - `state.select` - Select CSGs by names

4. **Operations**
   - `operations.add` - Add new operation (Align, Delete, ExtrudeSurface, Fillet, Group, Hide, LinearDistribution, Lock, Mirror, Paste, RadialDistribution, Resize, SetMaterial, Show, ToHole, ToSolid, UnGroup, UnLock, WireMeshView)
   - `operations.remove` - Remove an operation
   - `operations.regenerate` - Regenerate from source operation

5. **CSG manipulation**
   - `csg.getParameters` - Get parameters for a specific CSG
   - `csg.setBounds` - Set bounds for a CSG

6. **Shapes**
    - `shapes.getPalette` - Get available shapes and primitives
    - `shapes.addByName` - Add a shape from the palette by name/description


## Example usage

```
I want to add a cube to the 3D scene.
I need to get all currently selected objects.
Can you align two objects?
```

## Limitations

- The MCP server must be running on localhost:8080
- Some operations require the main CaDoodle application to be initialized
- Not all operations are fully implemented in the API
