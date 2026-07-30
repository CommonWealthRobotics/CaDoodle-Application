import { tool } from "@opencode-ai/plugin";
import { Socket } from 'net';
import crypto from 'crypto';

const SERVER_HOST = "127.0.0.1";
const SERVER_PORT = 8080;
const TIMEOUT = 30000;

interface Request {
    jsonrpc: string;
    method: string;
    params?: object;
    id: string;
}

interface Response {
    jsonrpc: string;
    id: string;
    result?: object;
    error?: { code: string; message: string };
}

class CaDoodleMCPClient {
    private socket: Socket | null = null;

    connect(): Promise<void> {
        return new Promise((resolve, reject) => {
            this.socket = new Socket();
            this.socket.setEncoding('utf8');

            this.socket.on('connect', () => {
                resolve();
            });

            this.socket.on('error', (error) => {
                reject(error);
            });

            this.socket.setTimeout(TIMEOUT, () => {
                this.socket?.destroy();
                reject(new Error('Connection timeout'));
            });

            this.socket.connect(SERVER_PORT, SERVER_HOST);
        });
    }

    disconnect(): void {
        if (this.socket) {
            this.socket.end();
            this.socket = null;
        }
    }

    sendRequest(method: string, params: object = {}): Promise<any> {
        return new Promise((resolve, reject) => {
            if (!this.socket) {
                reject(new Error('Not connected to server'));
                return;
            }

            const request: Request = {
                jsonrpc: "2.0",
                method: method,
                params: params,
                id: crypto.randomUUID()
            };

            const message = JSON.stringify(request) + '\n';
            this.socket.write(message);

            let responseData = '';
            let responseCallback: ((value: any) => void) | null = null;
            let errorCallback: ((error: any) => void) | null = null;

            const onData = (chunk: string) => {
                responseData += chunk;
                try {
                    const parsed = JSON.parse(responseData);
                    this.socket!.removeListener('data', onData);
                    this.socket!.removeListener('error', onError);
                    this.socket!.removeListener('timeout', onTimeout);
                    if (parsed.error) {
                        reject(new Error(parsed.error.message));
                    } else {
                        resolve(parsed.result || parsed);
                    }
                } catch (e) {
                    // Continue collecting data
                }
            };

            const onError = (error: any) => {
                this.socket!.removeListener('data', onData);
                this.socket!.removeListener('error', onError);
                this.socket!.removeListener('timeout', onTimeout);
                reject(error);
            };

            const onTimeout = () => {
                this.socket!.removeListener('data', onData);
                this.socket!.removeListener('error', onError);
                this.socket!.removeListener('timeout', onTimeout);
                this.socket?.destroy();
                reject(new Error('Request timeout'));
            };

            this.socket.on('data', onData);
            this.socket.on('error', onError);
            this.socket.on('timeout', onTimeout);
        });
    }
}

export const initialize = tool({
    description: "Initialize connection to the MCP server",
    args: {},
    async execute() {
        const client = new CaDoodleMCPClient();
        try {
            await client.connect();
            const result = await client.sendRequest("initialize");
            client.disconnect();
            return result;
        } catch (error) {
            client.disconnect();
            throw error;
        }
    }
});

export const get_current_state = tool({
    description: "Get current scene state",
    args: {},
    async execute() {
        const client = new CaDoodleMCPClient();
        try {
            await client.connect();
            const result = await client.sendRequest("state.getCurrent");
            client.disconnect();
            return result;
        } catch (error) {
            client.disconnect();
            throw error;
        }
    }
});

export const get_selected = tool({
    description: "Get selected CSG names",
    args: {},
    async execute() {
        const client = new CaDoodleMCPClient();
        try {
            await client.connect();
            const result = await client.sendRequest("state.getSelected");
            client.disconnect();
            return result;
        } catch (error) {
            client.disconnect();
            throw error;
        }
    }
});

export const get_csgs = tool({
    description: "Get all CSGs with details",
    args: {},
    async execute() {
        const client = new CaDoodleMCPClient();
        try {
            await client.connect();
            const result = await client.sendRequest("state.getCSGs");
            client.disconnect();
            return result;
        } catch (error) {
            client.disconnect();
            throw error;
        }
    }
});

export const select_csgs = tool({
    description: "Select CSGs by names",
    args: {
        names: tool.schema.array().of(tool.schema.string()).describe("Names of CSGs to select")
    },
    async execute({ names }) {
        const client = new CaDoodleMCPClient();
        try {
            await client.connect();
            const result = await client.sendRequest("state.select", { names });
            client.disconnect();
            return result;
        } catch (error) {
            client.disconnect();
            throw error;
        }
    }
});

export const add_operation = tool({
    description: "Add new operation",
    args: {
        operationType: tool.schema.string().describe("Type of operation"),
        params: tool.schema.object().describe("Parameters for the operation")
    },
    async execute({ operationType, params }) {
        const client = new CaDoodleMCPClient();
        try {
            await client.connect();
            params.operationType = operationType;
            const result = await client.sendRequest("operations.add", params);
            client.disconnect();
            return result;
        } catch (error) {
            client.disconnect();
            throw error;
        }
    }
});

export const remove_operation = tool({
    description: "Remove an operation",
    args: {
        operationId: tool.schema.string().describe("ID of operation to remove")
    },
    async execute({ operationId }) {
        const client = new CaDoodleMCPClient();
        try {
            await client.connect();
            const result = await client.sendRequest("operations.remove", { operationId });
            client.disconnect();
            return result;
        } catch (error) {
            client.disconnect();
            throw error;
        }
    }
});

export const regenerate = tool({
    description: "Regenerate from source operation",
    args: {
        sourceOperationId: tool.schema.string().optional().describe("ID of source operation")
    },
    async execute({ sourceOperationId }) {
        const client = new CaDoodleMCPClient();
        try {
            await client.connect();
            const params: { sourceOperationId?: string } = {};
            if (sourceOperationId) {
                params.sourceOperationId = sourceOperationId;
            }
            const result = await client.sendRequest("operations.regenerate", params);
            client.disconnect();
            return result;
        } catch (error) {
            client.disconnect();
            throw error;
        }
    }
});

export const get_parameters = tool({
    description: "Get parameters for a specific CSG",
    args: {
        csgName: tool.schema.string().describe("Name of CSG")
    },
    async execute({ csgName }) {
        const client = new CaDoodleMCPClient();
        try {
            await client.connect();
            const result = await client.sendRequest("csg.getParameters", { csgName });
            client.disconnect();
            return result;
        } catch (error) {
            client.disconnect();
            throw error;
        }
    }
});

export const set_bounds = tool({
    description: "Set bounds for a CSG",
    args: {
        csgName: tool.schema.string().describe("Name of CSG"),
        minX: tool.schema.number().describe("Minimum X bound"),
        minY: tool.schema.number().describe("Minimum Y bound"),
        minZ: tool.schema.number().describe("Minimum Z bound"),
        maxX: tool.schema.number().describe("Maximum X bound"),
        maxY: tool.schema.number().describe("Maximum Y bound"),
        maxZ: tool.schema.number().describe("Maximum Z bound")
    },
    async execute({ csgName, minX, minY, minZ, maxX, maxY, maxZ }) {
        const client = new CaDoodleMCPClient();
        try {
            await client.connect();
            const bounds = { csgName, minX, minY, minZ, maxX, maxY, maxZ };
            const result = await client.sendRequest("csg.setBounds", bounds);
            client.disconnect();
            return result;
        } catch (error) {
            client.disconnect();
            throw error;
        }
    }
});

export const get_shapes_palette = tool({
    description: "Get available shapes and primitives",
    args: {},
    async execute() {
        const client = new CaDoodleMCPClient();
        try {
            await client.connect();
            const result = await client.sendRequest("shapes.getPalette");
            client.disconnect();
            return result;
        } catch (error) {
            client.disconnect();
            throw error;
        }
    }
});

export const add_shape_by_name = tool({
    description: "Add a shape from the palette by name",
    args: {
        name: tool.schema.string().describe("Name of shape to add")
    },
    async execute({ name }) {
        const client = new CaDoodleMCPClient();
        try {
            await client.connect();
            const result = await client.sendRequest("shapes.addByName", { name });
            client.disconnect();
            return result;
        } catch (error) {
            client.disconnect();
            throw error;
        }
    }
});
