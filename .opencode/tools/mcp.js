#!/usr/bin/env node
var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));
var import_net = require("net");
var import_crypto = __toESM(require("crypto"));
const SERVER_HOST = "127.0.0.1";
const SERVER_PORT = 8080;
const TIMEOUT = 3e4;
class CaDoodleMCPClient {
  socket = null;
  connect() {
    return new Promise((resolve, reject) => {
      this.socket = new import_net.Socket();
      this.socket.setEncoding("utf8");
      this.socket.on("connect", () => {
        resolve();
      });
      this.socket.on("error", (error) => {
        reject(error);
      });
      this.socket.setTimeout(TIMEOUT, () => {
        this.socket?.destroy();
        reject(new Error("Connection timeout"));
      });
      this.socket.connect(SERVER_PORT, SERVER_HOST);
    });
  }
  disconnect() {
    if (this.socket) {
      this.socket.end();
      this.socket = null;
    }
  }
  sendRequest(method, params = {}) {
    return new Promise((resolve, reject) => {
      if (!this.socket) {
        reject(new Error("Not connected to server"));
        return;
      }
      const request = {
        jsonrpc: "2.0",
        method,
        params,
        id: import_crypto.default.randomUUID()
      };
      const message = JSON.stringify(request) + "\n";
      this.socket.write(message);
      let responseData = "";
      let responseCallback = null;
      let errorCallback = null;
      const onData = (chunk) => {
        responseData += chunk;
        try {
          const parsed = JSON.parse(responseData);
          this.socket.removeListener("data", onData);
          this.socket.removeListener("error", onError);
          this.socket.removeListener("timeout", onTimeout);
          if (parsed.error) {
            reject(new Error(parsed.error.message));
          } else {
            resolve(parsed.result || parsed);
          }
        } catch (e) {
        }
      };
      const onError = (error) => {
        this.socket.removeListener("data", onData);
        this.socket.removeListener("error", onError);
        this.socket.removeListener("timeout", onTimeout);
        reject(error);
      };
      const onTimeout = () => {
        this.socket.removeListener("data", onData);
        this.socket.removeListener("error", onError);
        this.socket.removeListener("timeout", onTimeout);
        this.socket?.destroy();
        reject(new Error("Request timeout"));
      };
      this.socket.on("data", onData);
      this.socket.on("error", onError);
      this.socket.on("timeout", onTimeout);
    });
  }
}
async function main() {
  if (process.argv.length < 3) {
    console.log(JSON.stringify({ error: "No command provided" }));
    process.exit(1);
  }
  const command = process.argv[2];
  const args = process.argv.slice(3);
  let client = null;
  try {
    client = new CaDoodleMCPClient();
    await client.connect();
    let result;
    switch (command) {
      case "initialize":
        result = await client.sendRequest("initialize");
        break;
      case "get_current_state":
        result = await client.sendRequest("state.getCurrent");
        break;
      case "get_selected":
        result = await client.sendRequest("state.getSelected");
        break;
      case "get_csgs":
        result = await client.sendRequest("state.getCSGs");
        break;
      case "select_csgs":
        if (args.length < 1) {
          throw new Error("Missing names argument");
        }
        const names = JSON.parse(args[0]);
        result = await client.sendRequest("state.select", { names });
        break;
      case "add_operation":
        if (args.length < 2) {
          throw new Error("Missing operation_type and params arguments");
        }
        const operationType = args[0];
        const params = JSON.parse(args[1]);
        params.operationType = operationType;
        result = await client.sendRequest("operations.add", params);
        break;
      case "remove_operation":
        if (args.length < 1) {
          throw new Error("Missing operation_id argument");
        }
        const operationId = args[0];
        result = await client.sendRequest("operations.remove", { operationId });
        break;
      case "regenerate":
        const sourceOperationId = args[0] || null;
        const regenParams = {};
        if (sourceOperationId) {
          regenParams.sourceOperationId = sourceOperationId;
        }
        result = await client.sendRequest("operations.regenerate", regenParams);
        break;
      case "get_parameters":
        if (args.length < 1) {
          throw new Error("Missing csg_name argument");
        }
        const csgName = args[0];
        result = await client.sendRequest("csg.getParameters", { csgName });
        break;
      case "set_bounds":
        if (args.length < 7) {
          throw new Error("Missing bounds arguments");
        }
        const minX = parseFloat(args[1]);
        const minY = parseFloat(args[2]);
        const minZ = parseFloat(args[3]);
        const maxX = parseFloat(args[4]);
        const maxY = parseFloat(args[5]);
        const maxZ = parseFloat(args[6]);
        if (isNaN(minX) || isNaN(minY) || isNaN(minZ) || isNaN(maxX) || isNaN(maxY) || isNaN(maxZ)) {
          throw new Error("Invalid numeric value in bounds parameters");
        }
        const bounds = {
          csgName: args[0],
          minX,
          minY,
          minZ,
          maxX,
          maxY,
          maxZ
        };
        result = await client.sendRequest("csg.setBounds", bounds);
        break;
      case "get_shapes_palette":
        result = await client.sendRequest("shapes.getPalette");
        break;
      case "add_shape_by_name":
        if (args.length < 1) {
          throw new Error("Missing name argument");
        }
        const name = args[0];
        result = await client.sendRequest("shapes.addByName", { name });
        break;
      default:
        console.log(JSON.stringify({ error: `Unknown command: ${command}` }));
        process.exit(1);
    }
    console.log(JSON.stringify(result));
  } catch (error) {
    console.log(JSON.stringify({ error: error instanceof Error ? error.message : String(error) }));
    process.exit(1);
  } finally {
    if (client) {
      client.disconnect();
    }
  }
}
main().catch((err) => {
  console.log(JSON.stringify({ error: err.message }));
  process.exit(1);
});
