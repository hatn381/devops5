FROM node:16 AS build-stage

# Cretate app directory
WORKDIR /app

# Install app dependencies
COPY package*.json ./
RUN npm install

# Copy source
COPY . ./

RUN npm run build

FROM nginx:1.15
# Copy config nginx
COPY --from=build-stage /app/nginx/nginx.conf /etc/nginx/conf.d/default.conf
WORKDIR /usr/share/nginx/html
# Remove default nginx static assets
RUN rm -rf ./*
# Copy static assets from builder stage
COPY --from=build-stage /app/build .
# Containers run nginx with global directives and daemon off
EXPOSE 80

ENTRYPOINT ["nginx", "-g", "daemon off;"]
