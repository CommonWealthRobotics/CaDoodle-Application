import { tool } from "@opencode-ai/plugin";

export const test_echo = tool({
  description: "Test echo tool",
  args: {
    message: tool.schema.string().describe("Message to echo"),
  },
  async execute({ message }) {
    return `Echo: ${message}`;
  },
});
