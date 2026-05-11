# ── Étape 1 : Build Angular ─────────────────────────────────────────
FROM node:lts-alpine3.22 AS BUILD
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ── Étape 2 : Web Server ─────────────────────────────────────────────
FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=BUILD /app/dist/space_app_Agular/browser /usr/share/nginx/html
