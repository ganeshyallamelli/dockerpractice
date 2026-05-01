# 🐳 Docker: Zero to Hero Masterclass
### 3 Progressive Sessions · Offline / LAN · Production + Kubernetes Ready

> **Audience:** Developers who can code but have never touched Docker
> **Format:** Offline, everyone on the same Wi-Fi
> **End State:** Ship real apps in containers, understand production patterns, ready for Kubernetes

---

## 🗺️ The Full Journey

```
Session 1 (1.5 hr)          Session 2 (1.5 hr)          Session 3 (1.5 hr)
────────────────────         ────────────────────         ────────────────────
WHY + HOW CONTAINERS    →    REAL-WORLD PATTERNS     →    PRODUCTION MINDSET
Mental model                 Volumes & persistence         Multi-stage builds
Images & containers          Networking deep dive          Health checks
First Dockerfile             Compose orchestration         Secrets & env hygiene
LAN Counter Demo 🔥          Debugging live containers     Intro to Kubernetes
Push to Docker Hub           Logs & inspection             What comes after Docker
```

---

## 🧰 Prerequisites  *(send to friends before Session 1)*

```bash
# 1. Install Docker Desktop
#    Mac/Windows: https://www.docker.com/products/docker-desktop/
#    Linux:       https://docs.docker.com/engine/install/

# 2. Smoke test
docker run hello-world
# Should print "Hello from Docker!"

# 3. Clone the workshop repo
git clone https://github.com/YOUR_ORG/docker-masterclass
cd docker-masterclass

# 4. VS Code + Docker extension (optional but great)
#    Extension: ms-azuretools.vscode-docker
```

> 💡 **Host tip:** Before Session 1, verify everyone's Docker Desktop is running
> and `hello-world` works. Saves 10 minutes of setup frustration.

---
---

# SESSION 1 · Containers, Images & Your First Real App
**Duration:** 1.5 hours | Zero to running and building containers

## 🎯 What They'll Walk Away With
- A real mental model of how Docker works
- Confidence running, inspecting, and stopping containers
- Their own Dockerfile, built and running
- A LAN app that everyone in the room can hit from their phones

---

## Part 1 · The "Why"  *(15 min)*

### Open With This Story

> Dev A writes a Node.js app on macOS, Node 18.
> Dev B clones it — has Node 16, different npm. Broken.
> Staging server is Ubuntu with slightly different glibc. Broken differently.
> Production is a Linux VM. Works, but only after 3 hours of debugging env vars.
>
> **They shipped nothing that day.** Just fought the environment.

Docker's answer: **ship the environment, not just the code.**

### Containers vs VMs — Draw This Live

```
VIRTUAL MACHINE                         DOCKER CONTAINERS
────────────────────────────────        ────────────────────────────────
┌──────────┐  ┌──────────┐              ┌──────────┐  ┌──────────┐
│ App+Libs │  │ App+Libs │              │ App+Libs │  │ App+Libs │
│ Guest OS │  │ Guest OS │              └──────────┘  └──────────┘
└──────────┘  └──────────┘                  Docker Engine
    Hypervisor / Host OS                    Host OS Kernel (shared)

Each VM = GBs, boots in minutes         Each container = MBs, starts in ms
```

Key point: containers share the host OS kernel — they're lightweight and
start in milliseconds, not minutes.

### The Three Pillars

| Concept     | What it is                        | Analogy                        |
|-------------|-----------------------------------|--------------------------------|
| **Image**   | Read-only blueprint               | Recipe / class definition      |
| **Container** | Running instance of an image   | Dish made from the recipe      |
| **Registry** | Store of images (Docker Hub)   | App Store for containers       |

```
Dockerfile  ──build──>  Image  ──run──>  Container
                          ^
                     Docker Hub (pull)
```

---

## Part 2 · Hands-On: Feel the Power  *(20 min)*

### Exercise 1 — Hello World
```bash
docker run hello-world
```
Walk through what happened:
1. Docker checked locally — image not found
2. Pulled from Docker Hub automatically
3. Ran the container → printed → exited

### Exercise 2 — Run Another OS Interactively
```bash
docker run -it ubuntu bash

# Inside the container:
ls /
cat /etc/os-release     # You're inside Ubuntu!
whoami                  # root
exit
```
> Ask the room: *"How many of you just ran Ubuntu without installing Ubuntu?"*

### Exercise 3 — Run a Web Server in 1 Line
```bash
docker run -d -p 8080:80 --name my-nginx nginx
```

Open **http://localhost:8080** — Nginx welcome page appears.

```bash
docker ps                      # See it running
docker logs my-nginx           # See access logs
docker exec -it my-nginx sh    # Shell inside running container
  ls /usr/share/nginx/html     # The files being served
  exit
docker stop my-nginx
docker rm my-nginx
```

### Core Commands Cheat Sheet

```bash
docker pull <image>            # Download image
docker images                  # List local images
docker run <image>             # Create + start container
  -d                           # Detached (background)
  -it                          # Interactive terminal
  -p HOST:CONTAINER            # Port mapping
  --name <name>                # Give it a name
docker ps                      # Running containers
docker ps -a                   # All containers (including stopped)
docker stop <name>             # Stop
docker rm <name>               # Delete container
docker rmi <image>             # Delete image
docker exec -it <name> bash    # Shell into running container
docker logs -f <name>          # Follow logs
```

---

## Part 3 · Write Your First Dockerfile  *(20 min)*

### The App

A Node.js Express API — something everyone can code along with.

**Project structure:**
```
node-app/
├── Dockerfile
├── package.json
└── server.js
```

**`server.js`**
```javascript
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
```

**`package.json`**
```json
{
  "name": "docker-demo",
  "version": "1.0.0",
  "scripts": { "start": "node server.js" },
  "dependencies": { "express": "^4.18.0" }
}
```

**`Dockerfile`**
```dockerfile
# 1. Base image — Node 20, Alpine Linux (tiny, ~5MB base)
FROM node:20-alpine

# 2. Set working directory inside the container
WORKDIR /app

# 3. Copy dependency files FIRST (layer caching trick — explained below)
COPY package*.json ./

# 4. Install dependencies
RUN npm install

# 5. Copy the rest of the source code
COPY . .

# 6. Document which port this app uses
EXPOSE 3000

# 7. Default command when container starts
CMD ["npm", "start"]
```

**Build and run:**
```bash
docker build -t my-node-app .
docker images                           # Spot your new image
docker run -d -p 3000:3000 my-node-app
curl http://localhost:3000
```

### Why Copy `package.json` First? — Layer Caching

Docker builds images in layers. Each instruction = one layer. Layers are **cached**.

```
Layer 1: FROM node:20-alpine        -> cached (never changes)
Layer 2: WORKDIR /app               -> cached
Layer 3: COPY package*.json ./      -> cached UNLESS package.json changes
Layer 4: RUN npm install            -> cached UNLESS layer 3 changed  <-- key
Layer 5: COPY . .                   -> invalidated when your code changes
Layer 6: CMD [...]                  -> rebuilds
```

If you `COPY . .` first, every code change triggers `npm install` again.
This ordering makes rebuilds 10x faster.

---

## Part 4 · LAN Demo — The Room's First Shared App  *(20 min)*

> This is the moment that makes Docker real for everyone.
> All their browsers and phones hitting one machine, one counter.

### The Architecture
```
Friend's Phone / Laptop
       |
       v   http://YOUR_IP:3000/visits
 ─────────────────
 Your Laptop (Host)
 ─────────────────
       |  port 3000
       v
 ┌─────────────┐
 │  Node App   │ ---redis:6379---> ┌───────────┐
 │  Container  │                   │   Redis   │
 └─────────────┘                   │ Container │
                                   └───────────┘
```

### Step 1 — Find Your LAN IP
```bash
# macOS
ipconfig getifaddr en0

# Linux
hostname -I

# Windows
ipconfig
# Look for IPv4 Address under your Wi-Fi adapter
```

### Step 2 — Update `server.js` with the Counter
```javascript
const express = require("express");
const redis = require("redis");

const app = express();

const client = redis.createClient({ url: "redis://redis:6379" });
client.connect();

app.get("/", (req, res) => {
  res.send(`<h1>Hello!</h1><p><a href="/visits">See the counter</a></p>`);
});

app.get("/visits", async (req, res) => {
  const visits = await client.incr("counter");
  res.send(`
    <html>
      <head><meta http-equiv="refresh" content="2"></head>
      <body style="font-family:sans-serif;text-align:center;padding:50px">
        <h1>🐳 LAN Visit Counter</h1>
        <h2 style="font-size:80px;color:#0db7ed">${visits}</h2>
        <p>Everyone in the room is hitting this counter!</p>
      </body>
    </html>
  `);
});

app.get("/health", (req, res) => res.json({ status: "ok" }));

app.listen(3000, "0.0.0.0", () => console.log("Running on :3000"));
```

Update `package.json`:
```json
{
  "dependencies": {
    "express": "^4.18.0",
    "redis": "^4.6.0"
  }
}
```

### Step 3 — Create a Docker Network
```bash
# Containers on the same network can reach each other by service name
docker network create workshop-net
```

### Step 4 — Run Redis
```bash
docker run -d \
  --name redis \
  --network workshop-net \
  redis:alpine
```

### Step 5 — Build & Run the App
```bash
docker build -t docker-lan-app .

docker run -d \
  --name app \
  --network workshop-net \
  -p 3000:3000 \
  docker-lan-app
```

### Step 6 — Share Your IP With the Room
```
Everyone opens:  http://YOUR_LAN_IP:3000/visits
```

Watch the counter climb in real time as everyone opens it on their phones.

> Point out: the Node app reaches Redis using the hostname `redis` —
> that's the **container name**, not localhost. This is Docker networking.

---

## Part 5 · Push to Docker Hub  *(10 min)*

```bash
# Create a free account at hub.docker.com first

docker login

# Tag your image with your username
docker tag docker-lan-app YOUR_USERNAME/docker-lan-app:v1

# Push it
docker push YOUR_USERNAME/docker-lan-app:v1

# Now anyone in the world can run your app:
docker run -p 3000:3000 YOUR_USERNAME/docker-lan-app:v1
```

---

## Session 1 Recap

| Concept | Learned |
|---------|---------|
| Why Docker exists | yes |
| Images vs Containers | yes |
| Running containers | yes |
| Writing a Dockerfile | yes |
| Layer caching | yes |
| Docker networking basics | yes |
| LAN sharing a live app | yes |
| Pushing to Docker Hub | yes |

**Homework before Session 2:**
- Dockerize a small app you've already written
- Explore Docker Hub — find images for Postgres, MongoDB, Redis
- Run `docker system df` to see how much space images take

---
---

# SESSION 2 · Real-World Patterns: Compose, Volumes & Networking
**Duration:** 1.5 hours | Running containers to orchestrating real stacks

## 🎯 What They'll Walk Away With
- Manage multi-container apps with Docker Compose (no more long `docker run` commands)
- Persist data with volumes so Redis survives restarts
- Understand Docker networking properly
- Debug running containers like a pro
- Run a production-like stack with Nginx as a reverse proxy

---

## Part 1 · Docker Compose — One File to Rule Them All  *(25 min)*

### The Problem With Session 1's Approach

```bash
# Manual, fragile, 6 commands to remember:
docker network create workshop-net
docker run -d --name redis --network workshop-net redis:alpine
docker build -t docker-lan-app .
docker run -d --name app --network workshop-net -p 3000:3000 docker-lan-app
# ...and teardown is another 4 commands
```

Compose replaces all of that with **one file + one command**.

### The Upgrade — Full Stack with Nginx

**Project structure:**
```
docker-masterclass/
├── docker-compose.yml
├── nginx.conf
└── app/
    ├── Dockerfile
    ├── package.json
    └── server.js
```

**`docker-compose.yml`**
```yaml
version: "3.9"

services:

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"          # LAN friends hit port 80 — no port number in URL!
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    depends_on:
      - app

  app:
    build: ./app
    environment:
      - REDIS_URL=redis://redis:6379
      - NODE_ENV=development
    depends_on:
      - redis
    # No ports here — Nginx is the only entry point

  redis:
    image: redis:7-alpine
    volumes:
      - redis-data:/data              # Named volume — persists across restarts
    command: redis-server --appendonly yes

volumes:
  redis-data:
```

**`nginx.conf`**
```nginx
server {
  listen 80;

  location / {
    proxy_pass http://app:3000;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
  }
}
```

```bash
docker compose up --build           # Build + start everything

# LAN URL is now clean — no port number needed!
# http://YOUR_LAN_IP/visits
```

### Docker Compose Cheat Sheet

```bash
docker compose up                   # Start (foreground)
docker compose up -d                # Start (background)
docker compose up --build           # Rebuild then start
docker compose down                 # Stop + remove containers
docker compose down -v              # Also remove volumes (deletes data)
docker compose ps                   # Status of all services
docker compose logs -f app          # Follow logs for one service
docker compose exec app sh          # Shell into a service
docker compose restart app          # Restart one service
docker compose build                # Rebuild without starting
```

---

## Part 2 · Volumes — Making Data Survive  *(20 min)*

### Why This Matters

```bash
# Without volumes:
docker compose down      # Redis container gone
docker compose up        # New Redis — counter resets to 0
```

### Two Types of Volumes

**Named Volumes** — Docker manages the location. Best for databases.
```yaml
services:
  redis:
    volumes:
      - redis-data:/data       # volume_name:container_path

volumes:
  redis-data:                  # declare at top level
```

**Bind Mounts** — You control the path. Best for development hot-reload.
```yaml
services:
  app:
    volumes:
      - ./app:/app             # host_path:container_path
      - /app/node_modules      # don't overwrite node_modules!
```

### Live Demo — Prove Data Persists

```bash
docker compose up -d

# Hit /visits a few times — counter reaches, say, 12

docker compose down          # Stop everything

docker compose up -d         # Start again

# Visit /visits — counter is STILL at 12
```

### Dev Mode — Hot Reload With Bind Mounts

```yaml
  app:
    build: ./app
    volumes:
      - ./app:/app
      - /app/node_modules
```

```dockerfile
RUN npm install -g nodemon
CMD ["nodemon", "server.js"]
```

Edit `server.js` on your laptop → container reloads automatically. No rebuild needed.

---

## Part 3 · Networking Deep Dive  *(15 min)*

In Docker Compose, all services share a default network automatically.
Services use each other's **service names** as hostnames.

```
nginx  -->  app:3000    (service name = hostname)
app    -->  redis:6379
```

```bash
# Prove it — shell into app and ping redis
docker compose exec app sh
ping redis           # Resolves to Redis container's IP
exit
```

### Exposing to the LAN

Only services with `ports:` are reachable from outside Docker.

```
nginx  →  ports: "80:80"      ✅ reachable from LAN
app    →  no ports             ✅ only reachable by nginx (safe)
redis  →  no ports             ✅ only reachable by app (safe)
```

Redis should never be exposed to the network. This is correct.

```bash
docker network ls
docker network inspect docker-masterclass_default
```

---

## Part 4 · Debugging Containers Like a Pro  *(15 min)*

```bash
# Shell into any service
docker compose exec app sh
docker compose exec redis redis-cli

# Full container details (IP, mounts, env vars, health)
docker inspect <container_name>

# Real-time resource usage
docker stats

# Check env vars inside a container
docker compose exec app env

# Copy files between container and host
docker cp app_container:/app/logs/error.log ./error.log

# Logs with timestamps
docker compose logs -f --timestamps app
```

### Common Problems & Fixes

| Problem | Likely Cause | Fix |
|---------|-------------|-----|
| Container exits immediately | App crashes on start | `docker compose logs app` |
| Port already in use | Another process | `lsof -i :3000` then kill |
| Can't reach Redis | Wrong hostname | Check service names match |
| Old code running | Didn't rebuild | `docker compose up --build` |
| Data not persisting | No volume defined | Add named volume |

---

## Part 5 · Environment Variables & Config  *(15 min)*

### Never Hardcode Config

```javascript
// Bad
const client = redis.createClient({ url: "redis://redis:6379" });

// Good
const client = redis.createClient({ url: process.env.REDIS_URL });
```

### Three Patterns

**1. Inline in compose** (fine for non-secrets):
```yaml
environment:
  - NODE_ENV=production
  - PORT=3000
```

**2. `.env` file** (add to `.gitignore`):
```bash
# .env
REDIS_URL=redis://redis:6379
NODE_ENV=development
```
```yaml
# Compose auto-loads .env
environment:
  - REDIS_URL=${REDIS_URL}
```

**3. `env_file` directive:**
```yaml
  app:
    env_file:
      - .env
```

---

## Session 2 Recap

| Concept | Learned |
|---------|---------|
| Docker Compose — full stack in one file | yes |
| Named volumes — data persistence | yes |
| Bind mounts — hot reload for dev | yes |
| Docker networking & service discovery | yes |
| Debugging with exec, logs, inspect | yes |
| Environment variable patterns | yes |
| Nginx as reverse proxy | yes |

**Homework before Session 3:**
- Add Postgres to your compose stack
- Try `docker compose up --scale app=3` (hint: think about port conflicts)
- Read: https://docs.docker.com/build/building/multi-stage/

---
---

# SESSION 3 · Production Mindset & The Bridge to Kubernetes
**Duration:** 1.5 hours | Competent to production-ready + K8s-aware

## 🎯 What They'll Walk Away With
- Build lean, secure production images with multi-stage builds
- Implement health checks and container self-healing
- Handle secrets without leaking them
- Understand what Kubernetes is and why it exists
- Leave with a concrete roadmap

---

## Part 1 · Multi-Stage Builds — Small, Secure Images  *(25 min)*

### The Problem With Dev Images in Production

```bash
docker images
# node:20 base image  -->  ~1.1 GB
# Your app on node:20 -->  ~1.2 GB
```

Your production image contains npm, npx, build tools, test deps —
none of which are needed at runtime. Just attack surface and wasted space.

### Multi-Stage Dockerfile

```dockerfile
# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci                    # clean install — faster, deterministic
COPY . .
RUN npm run build             # compile TypeScript, bundle, etc.


# Stage 2: Production — only what's needed to run
FROM node:20-alpine AS production

# Never run as root in production
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/package*.json ./
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist

USER appuser

EXPOSE 3000
CMD ["node", "dist/server.js"]
```

```bash
docker build -t my-app:prod .
docker images my-app            # Compare sizes vs dev build
```

### Why Non-Root Matters

```bash
# Current container:
docker run --rm my-node-app whoami
# root  <- dangerous

# Multi-stage with USER:
docker run --rm my-app:prod whoami
# appuser  <- safe
```

If someone exploits a vulnerability in your app:
- root user = game over for the host
- non-root user = limited blast radius

---

## Part 2 · Health Checks — Self-Healing Containers  *(20 min)*

Docker and Kubernetes can periodically probe your container to check it's
actually working, not just running. On failure, they restart it automatically.

### Health Check in Compose

```yaml
services:
  app:
    build: ./app
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:3000/health"]
      interval: 30s        # Check every 30s
      timeout: 5s          # Fail if no response in 5s
      start_period: 10s    # Grace period on startup
      retries: 3           # Mark unhealthy after 3 failures

  redis:
    image: redis:7-alpine
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 3s
      retries: 3

  nginx:
    image: nginx:alpine
    depends_on:
      app:
        condition: service_healthy    # Wait for healthy, not just started
```

### Your `/health` Endpoint Should Check Dependencies

```javascript
app.get("/health", async (req, res) => {
  try {
    await client.ping();             // Check Redis is reachable
    res.json({
      status: "ok",
      redis: "ok",
      uptime: process.uptime()
    });
  } catch (err) {
    res.status(503).json({ status: "error", redis: "unreachable" });
  }
});
```

```bash
docker compose ps                        # Shows health column
docker inspect <container> | grep -A 10 Health
```

---

## Part 3 · Secrets & Security  *(15 min)*

### The Wrong Ways

```bash
# Hardcoded in code
const DB_PASS = "supersecret";

# In docker-compose.yml (committed to git)
environment:
  - DB_PASSWORD=supersecret

# In .env that accidentally gets committed
DB_PASSWORD=supersecret
```

### The Right Ways

**Minimum viable:** `.env` file + `.gitignore`
```bash
echo ".env" >> .gitignore
```

**Docker Secrets** (for production):
```yaml
services:
  app:
    secrets:
      - db_password
    environment:
      - DB_PASSWORD_FILE=/run/secrets/db_password

secrets:
  db_password:
    file: ./secrets/db_password.txt    # Never commit this
```

```javascript
// Read secret from file at runtime
const fs = require("fs");
const dbPassword = process.env.DB_PASSWORD_FILE
  ? fs.readFileSync(process.env.DB_PASSWORD_FILE, "utf8").trim()
  : process.env.DB_PASSWORD;
```

**Production standard:** External secret managers
- AWS Secrets Manager
- HashiCorp Vault
- Azure Key Vault

### Scan Your Images for Vulnerabilities

```bash
docker scout cves my-app:prod

# Or with Trivy (open source):
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy image my-app:prod
```

---

## Part 4 · The Bridge to Kubernetes  *(20 min)*

### Why Kubernetes? Compose Isn't Enough for Production

| Need | Docker Compose | Kubernetes |
|------|---------------|------------|
| Run on multiple machines | No | Yes |
| Auto-restart crashed containers | Partial | Yes |
| Scale to 50 instances | Manual | Yes, automatic |
| Zero-downtime rolling deploys | No | Yes |
| Auto-scale based on traffic | No | Yes |
| Load balancing | No | Yes |

**Docker is how you build the unit. Kubernetes is how you run 1000 of them.**

### The Conceptual Bridge

```
Docker Compose concept      Kubernetes equivalent
──────────────────────────  ─────────────────────
docker-compose.yml      ->  deployment.yaml
service (compose)       ->  Service (K8s resource)
volumes                 ->  PersistentVolumeClaims
.env / env_file         ->  ConfigMaps & Secrets
healthcheck             ->  readinessProbe / livenessProbe
depends_on              ->  Init containers
```

### Run Kubernetes Locally

```bash
# Enable Kubernetes in Docker Desktop
# Settings -> Kubernetes -> Enable Kubernetes

# Verify
kubectl get nodes
```

### Your First Kubernetes Deployment

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: docker-lan-app
spec:
  replicas: 3                      # Run 3 copies
  selector:
    matchLabels:
      app: docker-lan-app
  template:
    metadata:
      labels:
        app: docker-lan-app
    spec:
      containers:
        - name: app
          image: YOUR_USERNAME/docker-lan-app:v1
          ports:
            - containerPort: 3000
          env:
            - name: REDIS_URL
              value: redis://redis:6379
          readinessProbe:           # K8s equivalent of healthcheck
            httpGet:
              path: /health
              port: 3000
            initialDelaySeconds: 10
            periodSeconds: 30
```

```bash
kubectl apply -f deployment.yaml
kubectl get pods                   # See 3 running pods
kubectl get deployments
kubectl logs -f <pod-name>
kubectl scale deployment docker-lan-app --replicas=5    # Scale live!
```

### The Key Insight

Everything learned in Docker translates directly to Kubernetes:
- Images you built → what K8s runs in pods
- Health checks → readiness/liveness probes
- Env vars & secrets → ConfigMaps & Secrets
- Volumes → PersistentVolumes
- Networking → Services

**Docker fluency IS the prerequisite for Kubernetes.**

---

## Part 5 · What's Next — Your Roadmap  *(10 min)*

```
You Are Here
     |
     v
Docker Compose mastery         <- After this course
     |
     v
Container registries           <- Docker Hub, AWS ECR, GitHub Container Registry
(private, versioned tags)
     |
     v
CI/CD with Docker              <- GitHub Actions builds + pushes on every commit
     |
     v
Kubernetes fundamentals        <- Pods, Deployments, Services, Ingress
     |
     v
Production Kubernetes          <- Helm, cert-manager, autoscaling
     |
     v
Platform Engineering           <- The top of the stack
```

### Best Resources

| Resource | What it's for |
|---------|--------------|
| [Play with Docker](https://labs.play-with-docker.com) | Free browser Docker playground |
| [Docker Official Docs](https://docs.docker.com) | Authoritative reference |
| [Awesome Docker](https://github.com/veggiemonk/awesome-docker) | Best curated list of tools & tutorials |
| [Ivan Velichko's Blog](https://iximiuz.com/en/) | Best visual deep-dives on containers |
| [Docker Curriculum](https://docker-curriculum.com/) | Free full beginner course |
| [KodeKloud](https://kodekloud.com/) | Hands-on K8s + Docker labs |
| [Kubernetes.io Docs](https://kubernetes.io/docs/home/) | Official K8s docs |

---

## Session 3 Recap

| Concept | Learned |
|---------|---------|
| Multi-stage builds | yes |
| Non-root containers | yes |
| Health checks | yes |
| depends_on with health conditions | yes |
| Secrets patterns | yes |
| Image vulnerability scanning | yes |
| Why Kubernetes exists | yes |
| K8s concepts mapped from Docker | yes |
| First kubectl deployment | yes |

---
---

# Master Cheat Sheet

```
IMAGES
  docker pull <img>              download image
  docker images                  list local images
  docker build -t name .         build from Dockerfile
  docker tag img user/img:v1     tag for push
  docker push user/img:v1        push to registry
  docker rmi <img>               delete image

CONTAINERS
  docker run -d -p 8080:80 --name x nginx
  docker ps                      running containers
  docker ps -a                   all containers
  docker stop x / docker rm x
  docker exec -it x sh           shell in
  docker logs -f x               follow logs
  docker stats                   resource usage
  docker inspect x               full details

COMPOSE
  docker compose up --build -d
  docker compose down -v
  docker compose logs -f app
  docker compose exec app sh
  docker compose ps
  docker compose build

NETWORKING
  docker network create net
  docker network ls
  service names = hostnames in compose

VOLUMES
  -v name:/path                  named volume
  -v ./local:/container          bind mount
  docker volume ls
  docker volume prune

CLEANUP
  docker system df               see disk usage
  docker system prune -a         remove everything unused
  docker volume prune            remove unused volumes
```

---

## LAN Demo Quick-Start  *(for the host)*

```bash
# 1. Find your IP
ipconfig getifaddr en0     # macOS
hostname -I                # Linux

# 2. Start the full stack
cd docker-masterclass
docker compose up --build -d

# 3. Share this URL with everyone in the room
http://YOUR_IP/visits

# 4. Watch the counter climb in real time
docker compose logs -f app
```

---

*Built for offline sessions · All on one Wi-Fi · Zero to hero*
