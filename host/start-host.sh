#!/bin/sh
# Mission Control — Host Startup Script
# Run this on YOUR laptop before the session

# Step 1: Build the Mission Control image
docker build -t mission-control ./host

# Step 2: Create a network so containers can talk to each other
docker network create workshop-net

# Step 3: Run Mission Control on the network
docker run -d \
  --name mission-control \
  --network workshop-net \
  -p 4000:4000 \
  -e PORT=4000 \
  mission-control

echo ""
echo "Mission Control is running!"
echo "Open on your machine : http://localhost:4000"
echo ""
echo "Share with the room  : http://$(hostname -I | awk '{print $1}'):4000"
echo ""
echo "Useful commands:"
echo "  docker logs -f mission-control   <- watch live registrations"
echo "  docker stop mission-control      <- stop the board"
echo "  docker rm mission-control        <- remove the container"