# HTTPS con Caddy y dominio de DuckDNS

- **Autor:** Yatako2304 (con asistencia de IA)
- **Fecha:** 2026-11-20
- **HU / tarea:** despliegue (requisito del inicio de sesión con Google, HU-3) y correo de credenciales
- **Estado:** terminado

## Qué hice

Agregué Caddy delante del nginx del frontend. Si `deploy/.env` tiene `DOMINIO`, Caddy obtiene y renueva solo el certificado de Let's Encrypt, sirve por HTTPS y redirige HTTP → HTTPS. Sin `DOMINIO`, todo sigue por HTTP en el puerto 80, como antes. La guía explica cómo crear el dominio gratis en DuckDNS, abrir el puerto 443 y configurar el correo.

## Archivos principales

- `deploy/Caddyfile`
- `docker-compose.yml` (servicio `caddy` y volúmenes `datos_caddy` y `config_caddy`)
- `deploy/.env.example` (`DOMINIO`)
- `docs/guias/despliegue.md` (sección 4)

## Decisiones y por qué

- **Decisión:** el certificado vive en el servidor (Caddy con Let's Encrypt), no en AWS.
  **Por qué:** es gratis y se renueva solo. La alternativa de AWS (un Load Balancer con un certificado de ACM) cobra por hora, gasta los créditos del Learner Lab y puede estar restringida ahí.
  **Alternativas descartadas:** ALB + ACM; certbot dentro del nginx, que obliga a programar la renovación.
- **Decisión:** dominio gratis de DuckDNS (`galiledu.duckdns.org`, apuntando a la Elastic IP).
  **Por qué:** Google y Let's Encrypt no aceptan una IP. Un dominio propio también sirve: solo cambia `DOMINIO`.
- **Decisión:** `DOMINIO` vacío mantiene el comportamiento anterior (HTTP en el 80).
  **Por qué:** así no se rompen la prueba de humo de GitHub (`docker.yml`, que usa `http://localhost`) ni un servidor sin dominio. `docker-compose.yml` pasa `:80` cuando la variable está vacía, porque Caddy no aplica su valor por defecto a una variable vacía.
- **Decisión:** el Caddyfile siempre atiende `http://localhost`.
  **Por qué:** `deploy/actualizar.sh` y la prueba de humo verifican con `curl http://localhost/...`, también cuando hay HTTPS.
- **Decisión:** los certificados se guardan en volúmenes (`datos_caddy`, `config_caddy`).
  **Por qué:** Let's Encrypt limita cuántos certificados se piden por semana; sin volumen se pedirían en cada despliegue.

## Pendientes / problemas conocidos

- [ ] Agregar `https://galiledu.duckdns.org` a los orígenes autorizados de Google cuando el inicio de sesión con Google llegue a `main`.
- [ ] Si la Elastic IP cambia, actualizarla en DuckDNS.

## Notas para quien siga

- Los secretos (`MAIL_PASSWORD`, el token de DuckDNS y las credenciales de Google) van solo en `deploy/.env` del servidor y en las cuentas de cada servicio, nunca en Git.
