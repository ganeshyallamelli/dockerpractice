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