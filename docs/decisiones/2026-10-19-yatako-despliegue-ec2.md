# Despliegue: todo en un solo EC2 con Docker

- **Autor:** Yatako2304 (con asistencia de IA)
- **Fecha:** 2026-10-19
- **HU / tarea:** despliegue (infraestructura, afecta a todos los módulos)
- **Estado:** terminado

## Qué hice

La app quedó publicada en un servidor EC2 de AWS Academy Learner Lab. La BD, el backend y el frontend corren en tres contenedores Docker en esa misma máquina. Cada merge a `main` con el CI en verde se despliega solo por SSH. Los pasos están en [`docs/guias/despliegue.md`](../guias/despliegue.md).

## Archivos principales

- `docker-compose.yml`: los 3 contenedores; solo el puerto 80 queda abierto.
- `backend/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf`: imágenes; nginx reenvía `/api` al backend.
- `deploy/instalar-servidor.sh`, `deploy/actualizar.sh`: instalación inicial y actualización.
- `.github/workflows/deploy.yml` (despliegue) y `.github/workflows/docker.yml` (prueba de humo).

## Decisiones y por qué

- **Decisión:** un solo EC2 con PostgreSQL en contenedor, sin RDS, S3/CloudFront ni ECS.
  **Por qué:** gasta pocos créditos del Learner Lab (unos 6–10 US$ al mes), EC2 siempre está disponible en la cuenta académica y alcanza para la demo y las pruebas del curso.
  **Alternativas descartadas:**
  - RDS: tiene respaldos automáticos, pero cuesta aparte.
  - S3 + CloudFront / Amplify para el frontend: son dos lugares que configurar y obligan a usar CORS.
  - Elastic Beanstalk / ECS / App Runner / Lightsail: son más caros o están limitados en Learner Lab.
- **Decisión:** el despliegue entra al servidor por SSH, no con credenciales de AWS.
  **Por qué:** en Learner Lab las credenciales de AWS cambian en cada sesión; la clave SSH (`labsuser.pem`) no vence.
- **Decisión:** el frontend y la API se sirven desde el mismo dominio, con nginx reenviando `/api`.
  **Por qué:** así no hace falta configurar CORS ni la URL del backend en el build.
- **Decisión:** el servidor carga los usuarios de prueba (`R__datos_prueba.sql`).
  **Por qué:** el profesor y el equipo los necesitan para revisar cada rol. Como el repositorio es público, no se deben cargar datos reales mientras estén activos.

## Pendientes / problemas conocidos

- [ ] Probar una vez **Actions → Despliegue → Run workflow** con el lab encendido.
- [ ] Correo real (SMTP de Gmail) en `deploy/.env` del servidor.
- La app solo está en línea con el lab iniciado. Si el workflow Despliegue falla con *timeout*, casi siempre es porque el lab está apagado.

## Notas para quien siga

- **Nunca uses "Reset" en el Learner Lab ni termines la instancia:** se borra el disco con la BD. Detenerla (*End Lab*) sí es seguro.
- Si se acaban los créditos, se repiten los pasos de la guía en otra cuenta y se actualizan los 3 secretos de GitHub. Los datos que no sean de prueba se mudan con `pg_dump` (ver *Operación diaria* en la guía).
- Para pasar la BD a RDS más adelante no hay que tocar el código Java. Basta con apuntar `DB_URL` y `DB_USER` del servicio `backend` en `docker-compose.yml` (hoy fijos a `db`) a RDS y quitar el contenedor `db`.
