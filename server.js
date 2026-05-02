const express = require("express");
const app = express();

app.get("/", (req, res) => {
  res.send("<h1>Hello from my container!</h1>");
});

app.get("/health", (req, res) => {
  res.json({ status: "ok", container: true });
});

app.listen(3000, "0.0.0.0", () => {
  console.log("Server running on port 3000");
});