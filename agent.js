const http = require("http");
const os = require("os");

const AGENT_NAME = process.env.AGENT_NAME || "Unknown";
const AGENT_ROLE = process.env.AGENT_ROLE || "Crew Member";
const HOST_URL = process.env.HOST_URL || "http://host.docker.internal:4000";
const HEARTBEAT_INTERVAL = 10000; // 10 seconds

function post(path, body) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const url = new URL(HOST_URL + path);
    const options = {
      hostname: url.hostname,
      port: url.port || 80,
      path: url.pathname,
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Content-Length": Buffer.byteLength(data),
      },
    };

    const req = http.request(options, (res) => {
      let raw = "";
      res.on("data", (chunk) => (raw += chunk));
      res.on("end", () => {
        try { resolve(JSON.parse(raw)); }
        catch { resolve(raw); }
      });
    });

    req.on("error", reject);
    req.write(data);
    req.end();
  });
}

async function register() {
  try {
    const result = await post("/register", {
      name: AGENT_NAME,
      role: AGENT_ROLE,
      container_id: os.hostname(), // Docker sets hostname = container ID
    });
    console.log(`[agent] Registered: ${AGENT_NAME} as "${AGENT_ROLE}"`);
    console.log(`[agent] ${result.message} (${result.agents} agents online)`);
  } catch (err) {
    console.error(`[agent] Failed to register:`, err.message);
    console.log(`[agent] Retrying in 3s...`);
    setTimeout(register, 3000);
  }
}

async function heartbeat() {
  try {
    await post("/heartbeat", { name: AGENT_NAME });
    console.log(`[agent] Heartbeat sent — ${new Date().toLocaleTimeString()}`);
  } catch (err) {
    console.error(`[agent] Heartbeat failed:`, err.message);
  }
}

async function deregister() {
  try {
    await post("/deregister", { name: AGENT_NAME });
    console.log(`[agent] ${AGENT_NAME} deregistered. Goodbye!`);
  } catch {}
}

// Graceful shutdown — deregister when container stops
process.on("SIGTERM", async () => {
  console.log(`[agent] Received SIGTERM — deregistering...`);
  await deregister();
  process.exit(0);
});

process.on("SIGINT", async () => {
  console.log(`[agent] Received SIGINT — deregistering...`);
  await deregister();
  process.exit(0);
});

// Start
(async () => {
  console.log(`[agent] Starting agent: ${AGENT_NAME} | Role: ${AGENT_ROLE}`);
  console.log(`[agent] Connecting to Mission Control at ${HOST_URL}`);
  await register();
  setInterval(heartbeat, HEARTBEAT_INTERVAL);
  console.log(`[agent] Heartbeat active every ${HEARTBEAT_INTERVAL / 1000}s`);
})();