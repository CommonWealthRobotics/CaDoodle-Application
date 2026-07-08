import { tool } from "@opencode-ai/plugin";
import path from "path";

// Get the path to the Python script relative to this file
const mcpScript = path.join(__dirname, "mcp.py");

// Helper to run Python script
async function runMcpCommand(command: string, args: string[] = []): Promise<string> {
  const { spawn } = await import("child_process");
  return new Promise((resolve, reject) => {
    const python = spawn("python3", [mcpScript, command, ...args]);
    let output = "";
    let error = "";

    python.stdout.on("data", (data: Buffer) => {
      output += data.toString();
    });

    python.stderr.on("data", (data: Buffer) => {
      error += data.toString();
    });

    python.on("close", (code) => {
      if (code === 0) {
        resolve(output.trim());
      } else {
        reject(new Error(`Process exited with code ${code}. Error: ${error}`));
      }
    });
  });
}

export const mcp_initialize = tool({
  description: "Initialize connection to the CaDoodle MCP server",
  args: {},
  async execute() {
    const result = await runMcpCommand("initialize");
    return result;
  },
});

export const mcp_get_current_state = tool({
  description: "Get current state of the CaDoodleFile including operation count and CSG list",
  args: {},
  async execute() {
    const result = await runMcpCommand("get_current_state");
    return result;
  },
});

export const mcp_get_selected = tool({
  description: "Get currently selected CSG names",
  args: {},
  async execute() {
    const result = await runMcpCommand("get_selected");
    return result;
  },
});

export const mcp_get_csgs = tool({
  description: "Get list of all CSGs with details including bounds, vertex count, and properties",
  args: {},
  async execute() {
    const result = await runMcpCommand("get_csgs");
    return result;
  },
});

export const mcp_select_csgs = tool({
  description: "Select CSGs by their names",
  args: {
    names: tool.schema.array(tool.schema.string()).describe("List of CSG names to select"),
  },
  async execute({ names }) {
    const result = await runMcpCommand("select_csgs", [JSON.stringify(names)]);
    return result;
  },
});

export const mcp_add_operation = tool({
  description: "Add an operation to the CaDoodleFile",
  args: {
    operation_type: tool.schema.string().describe("Type of operation (Align, Delete, ExtrudeSurface, Fillet, Group, Hide, LinearDistribution, Lock, Mirror, Paste, RadialDistribution, Resize, SetMaterial, Show, ToHole, ToSolid, UnGroup, UnLock, WireMeshView)"),
    params: tool.schema.record(tool.schema.any()).describe("Operation-specific parameters"),
  },
  async execute({ operation_type, params }) {
    const result = await runMcpCommand("add_operation", [operation_type, JSON.stringify(params)]);
    return result;
  },
});

export const mcp_remove_operation = tool({
  description: "Remove an operation from the CaDoodleFile",
  args: {
    operation_id: tool.schema.string().describe("ID of the operation to remove"),
  },
  async execute({ operation_id }) {
    const result = await runMcpCommand("remove_operation", [operation_id]);
    return result;
  },
});

export const mcp_regenerate = tool({
  description: "Regenerate operations from a source operation",
  args: {
    source_operation_id: tool.schema.string().optional().describe("Source operation ID (optional)"),
  },
  async execute({ source_operation_id }) {
    const args = source_operation_id ? [source_operation_id] : [];
    const result = await runMcpCommand("regenerate", args);
    return result;
  },
});

export const mcp_get_parameters = tool({
  description: "Get parameters for a specific CSG",
  args: {
    csg_name: tool.schema.string().describe("Name of the CSG"),
  },
  async execute({ csg_name }) {
    const result = await runMcpCommand("get_parameters", [csg_name]);
    return result;
  },
});

export const mcp_set_bounds = tool({
  description: "Set bounds for a specific CSG",
  args: {
    csg_name: tool.schema.string().describe("Name of the CSG"),
    min_x: tool.schema.number().describe("Minimum X coordinate"),
    min_y: tool.schema.number().describe("Minimum Y coordinate"),
    min_z: tool.schema.number().describe("Minimum Z coordinate"),
    max_x: tool.schema.number().describe("Maximum X coordinate"),
    max_y: tool.schema.number().describe("Maximum Y coordinate"),
    max_z: tool.schema.number().describe("Maximum Z coordinate"),
  },
  async execute({ csg_name, min_x, min_y, min_z, max_x, max_y, max_z }) {
    const result = await runMcpCommand("set_bounds", [
      csg_name,
      min_x.toString(),
      min_y.toString(),
      min_z.toString(),
      max_x.toString(),
      max_y.toString(),
      max_z.toString(),
    ]);
    return result;
  },
});

export const mcp_get_shapes_palette = tool({
  description: "Get shapes palette as structured JSON",
  args: {},
  async execute() {
    const result = await runMcpCommand("get_shapes_palette");
    return result;
  },
});
