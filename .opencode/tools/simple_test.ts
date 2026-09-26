import { tool } from "@opencode-ai/plugin";

export const simple_test = tool({
  description: "A simple test tool to verify tool loading",
  args: {
    message: tool.schema.string().describe("A test message"),
  },
  async execute({ message }) {
    return `Test received: ${message}`;
  },
});
