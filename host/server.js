const express = require("express");
const app = express();
app.use(express.json());

// In-memory agent registry
const agents = {};

// Agent registers itself on startup
app.post("/register", (req, res) => {
  const { name, role, container_id } = req.body;
  if (!name || !role) return res.status(400).json({ error: "name and role required" });

  agents[name] = {
    name,
    role,
    container_id: container_id || "unknown",
    joinedAt: new Date().toISOString(),
    lastSeen: Date.now(),
    status: "online",
  };

  console.log(`[+] ${name} (${role}) joined the network`);
  res.json({ message: `Welcome to the network, ${name}!`, agents: Object.keys(agents).length });
});

// Agent heartbeat — proves it's still alive
app.post("/heartbeat", (req, res) => {
  const { name } = req.body;
  if (agents[name]) {
    agents[name].lastSeen = Date.now();
    agents[name].status = "online";
  }
  res.json({ ok: true });
});

// Agent deregisters on shutdown
app.post("/deregister", (req, res) => {
  const { name } = req.body;
  if (agents[name]) {
    agents[name].status = "offline";
    console.log(`[-] ${name} left the network`);
  }
  res.json({ ok: true });
});

// API — list all agents
app.get("/agents", (req, res) => {
  // Mark agents as offline if no heartbeat in 15s
  const now = Date.now();
  Object.values(agents).forEach(a => {
    if (a.status === "online" && now - a.lastSeen > 15000) {
      a.status = "offline";
    }
  });
  res.json(Object.values(agents));
});

// Mission Control UI
app.get("/", (req, res) => {
  res.send(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>🐳 Docker Mission Control</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      background: #0d1117;
      color: #e6edf3;
      font-family: 'Segoe UI', system-ui, sans-serif;
      min-height: 100vh;
      padding: 32px;
    }
    header {
      text-align: center;
      margin-bottom: 40px;
    }
    header h1 {
      font-size: 2.5rem;
      color: #0db7ed;
      letter-spacing: 2px;
    }
    header p {
      color: #8b949e;
      margin-top: 8px;
      font-size: 1rem;
    }
    #stats {
      display: flex;
      justify-content: center;
      gap: 32px;
      margin-bottom: 40px;
    }
    .stat {
      text-align: center;
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 12px;
      padding: 20px 32px;
    }
    .stat-number {
      font-size: 2.5rem;
      font-weight: bold;
      color: #0db7ed;
    }
    .stat-label {
      font-size: 0.85rem;
      color: #8b949e;
      margin-top: 4px;
      text-transform: uppercase;
      letter-spacing: 1px;
    }
    #grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 20px;
      max-width: 1100px;
      margin: 0 auto;
    }
    .card {
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 14px;
      padding: 24px;
      transition: all 0.3s ease;
      position: relative;
      overflow: hidden;
    }
    .card.online {
      border-color: #238636;
      box-shadow: 0 0 16px rgba(35, 134, 54, 0.2);
    }
    .card.offline {
      border-color: #6e7681;
      opacity: 0.5;
    }
    .card::before {
      content: '';
      position: absolute;
      top: 0; left: 0; right: 0;
      height: 3px;
      background: #238636;
      border-radius: 14px 14px 0 0;
    }
    .card.offline::before {
      background: #6e7681;
    }
    .card-name {
      font-size: 1.3rem;
      font-weight: bold;
      color: #e6edf3;
      margin-bottom: 6px;
    }
    .card-role {
      font-size: 0.9rem;
      color: #0db7ed;
      margin-bottom: 16px;
      font-style: italic;
    }
    .card-meta {
      font-size: 0.75rem;
      color: #8b949e;
      line-height: 1.8;
    }
    .badge {
      display: inline-block;
      padding: 3px 10px;
      border-radius: 20px;
      font-size: 0.75rem;
      font-weight: bold;
      margin-bottom: 12px;
    }
    .badge.online { background: #1a4d2e; color: #3fb950; }
    .badge.offline { background: #2d2d2d; color: #8b949e; }
    #empty {
      text-align: center;
      color: #8b949e;
      margin-top: 80px;
      font-size: 1.1rem;
      display: none;
    }
    .pulse {
      animation: pulse 2s infinite;
    }
    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.4; }
    }
  </style>
</head>
<body>
  <header>
    <h1>🐳 Docker Mission Control</h1>
    <p>Live view of all containers on the network · Auto-refreshes every 3s</p>
  </header>

  <div id="stats">
    <div class="stat">
      <div class="stat-number" id="total">0</div>
      <div class="stat-label">Total Agents</div>
    </div>
    <div class="stat">
      <div class="stat-number" id="online-count" style="color:#3fb950">0</div>
      <div class="stat-label">Online</div>
    </div>
    <div class="stat">
      <div class="stat-number" id="offline-count" style="color:#6e7681">0</div>
      <div class="stat-label">Offline</div>
    </div>
  </div>

  <div id="grid"></div>
  <div id="empty">Waiting for agents to join the network...</div>

  <script>
    async function refresh() {
      const res = await fetch("/agents");
      const agents = await res.json();

      const grid = document.getElementById("grid");
      const empty = document.getElementById("empty");

      document.getElementById("total").textContent = agents.length;
      document.getElementById("online-count").textContent = agents.filter(a => a.status === "online").length;
      document.getElementById("offline-count").textContent = agents.filter(a => a.status === "offline").length;

      if (agents.length === 0) {
        grid.innerHTML = "";
        empty.style.display = "block";
        return;
      }
      empty.style.display = "none";

      // Sort: online first, then by join time
      agents.sort((a, b) => {
        if (a.status !== b.status) return a.status === "online" ? -1 : 1;
        return new Date(a.joinedAt) - new Date(b.joinedAt);
      });

      grid.innerHTML = agents.map(agent => {
        const joined = new Date(agent.joinedAt).toLocaleTimeString();
        return \`
          <div class="card \${agent.status}">
            <span class="badge \${agent.status}">\${agent.status === "online" ? "● ONLINE" : "○ OFFLINE"}</span>
            <div class="card-name">\${agent.name}</div>
            <div class="card-role">\${agent.role}</div>
            <div class="card-meta">
              Container: \${agent.container_id.substring(0, 12)}<br>
              Joined: \${joined}
            </div>
          </div>
        \`;
      }).join("");
    }

    refresh();
    setInterval(refresh, 3000);
  </script>
</body>
</html>`);
});

const PORT = process.env.PORT || 4000;
app.listen(PORT, "0.0.0.0", () => {
  console.log(`Mission Control running on :${PORT}`);
  console.log(`Board: http://localhost:${PORT}`);
});