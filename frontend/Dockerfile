# Imagen del frontend: compila la app de Vite y la sirve con nginx, que además
# redirige /api al backend (así navegador y API comparten dominio y no hace falta CORS).

# 1) Compilación
FROM node:20-alpine AS compilacion
WORKDIR /app
COPY package.json package-lock.json ./
# HUSKY=0: en la imagen no hay repositorio git, no se instalan hooks.
RUN HUSKY=0 npm ci
COPY . .
# La API se llama con ruta relativa: nginx la reenvía al backend.
ENV VITE_APP_API_URL=/api \
    VITE_APP_ENABLE_API_MOCKING=false
RUN npx vite build --base=/

# 2) Servidor web
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=compilacion /app/dist /usr/share/nginx/html
EXPOSE 80
