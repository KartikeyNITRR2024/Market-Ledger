# ==========================================
# Stage 1: Build Vite / React Application
# ==========================================
FROM node:20-alpine AS build
WORKDIR /app

# Copy package manifests first to leverage Docker layer caching
COPY package*.json ./

# Install exact dependencies using lockfile
RUN npm ci

# Copy all source files
COPY . .

# Build production bundle (outputs to /app/dist)
RUN npm run build

# ==========================================
# Stage 2: Serve via Nginx
# ==========================================
FROM nginx:alpine

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled static assets from builder stage
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]