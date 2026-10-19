# Despliegue en AWS (EC2)

GalilEdu corre en **un solo servidor EC2** con Docker. Después de configurarlo una vez, **cada merge a `main` con el CI en verde se despliega solo**.

```
Merge a main ─► CI (Backend + Frontend) ✅ ─► workflow "Despliegue" ─SSH─► EC2: deploy/actualizar.sh
                                                                              │
Internet :80 ─► frontend (nginx) ──/api──► backend (Spring Boot) ──► db (PostgreSQL 18, volumen)
```

| Archivo | Para qué |
| --- | --- |
| [`docker-compose.yml`](../../docker-compose.yml) | Los 3 contenedores y cómo se conectan. Solo el puerto 80 queda abierto. |
| [`backend/Dockerfile`](../../backend/Dockerfile), [`frontend/Dockerfile`](../../frontend/Dockerfile), [`frontend/nginx.conf`](../../frontend/nginx.conf) | Imágenes. nginx sirve la app y reenvía `/api` al backend. |
| [`deploy/instalar-servidor.sh`](../../deploy/instalar-servidor.sh) | Instalación inicial en el servidor (una vez). |
| [`deploy/actualizar.sh`](../../deploy/actualizar.sh) | Descarga `main` y reconstruye. Lo usa el despliegue automático. |
| [`deploy/.env.example`](../../deploy/.env.example) | Configuración de producción (en el servidor: `deploy/.env`, nunca en Git). |
| [`.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml) | Despliegue automático por SSH. |
| [`.github/workflows/docker.yml`](../../.github/workflows/docker.yml) | Prueba de humo: levanta todo en GitHub y verifica que se pueda iniciar sesión. |

## Antes de empezar: AWS Academy Learner Lab

Si usan la cuenta académica (**Learner Lab**), tengan en cuenta:

- **La sesión dura unas 4 horas.** Al terminar, el EC2 **se apaga** y vuelve a encender al iniciar el lab otra vez (los datos se conservan). **La app solo está en línea con el lab iniciado**: inícienlo antes de una demo o de que el profesor revise el link.
- **Las credenciales de AWS cambian en cada sesión**; por eso el despliegue automático usa **SSH** (la clave no vence), no claves de AWS.
- **Créditos limitados:** usen **una** instancia `t3.small` (o `t3.medium` si va lenta). Apagada no consume cómputo.
- Usen siempre la **misma región** (normalmente `us-east-1`, N. Virginia).

## 1. Crear el servidor (una vez, en la consola de AWS)

1. **Start Lab** → cuando el punto esté verde, **AWS** para abrir la consola.
2. **EC2 → Launch instance**:
   - **Name:** `galiledu`
   - **AMI:** Ubuntu Server 24.04 LTS
   - **Instance type:** `t3.small`
   - **Key pair:** `vockey` (la del lab). Descárgala desde *AWS Details → Download PEM* en la página del lab: es el archivo `labsuser.pem`.
   - **Network settings → Edit → Security group:** permitir **SSH (22)** y **HTTP (80)** desde *Anywhere* (`0.0.0.0/0`).
   - **Storage:** 20 GiB.
   - **Launch instance**.
3. **IP fija:** EC2 → **Elastic IPs → Allocate** → selecciónala → **Actions → Associate** → elige la instancia `galiledu`. Esa IP no cambia aunque el lab se apague; anótala (ej. `3.85.12.34`).

## 2. Instalar GalilEdu (una vez)

Desde tu PC (PowerShell o Git Bash), en la carpeta donde está `labsuser.pem`:

```bash
ssh -i labsuser.pem ubuntu@3.85.12.34
```

Ya dentro del servidor:

```bash
curl -fsSL https://raw.githubusercontent.com/Yatako2304/GalilEdu/main/deploy/instalar-servidor.sh | bash
```

Tarda unos 5–10 minutos la primera vez. Al terminar, abre **http://3.85.12.34** e inicia sesión con un usuario de prueba (ver README).

> Si Windows dice que los permisos de `labsuser.pem` son "demasiado abiertos": clic derecho → Propiedades → Seguridad → Opciones avanzadas → deshabilitar herencia y dejar solo tu usuario.

## 3. Activar el despliegue automático (una vez, en GitHub)

GitHub → **Settings → Secrets and variables → Actions → New repository secret**:

| Secreto | Valor |
| --- | --- |
| `SERVIDOR_HOST` | La Elastic IP (ej. `3.85.12.34`) |
| `SERVIDOR_USUARIO` | `ubuntu` |
| `SERVIDOR_CLAVE_SSH` | Todo el contenido de `labsuser.pem` (desde `-----BEGIN` hasta `END-----`) |

Desde ahí, cada merge a `main` despliega solo. Para probarlo: **Actions → Despliegue → Run workflow**.

## 4. Correo (opcional)

En el servidor, edita `nano /opt/galiledu/deploy/.env`, completa `MAIL_HOST`, `MAIL_USER`, `MAIL_PASSWORD` y `MAIL_FROM` (Gmail con contraseña de aplicación) y aplica:

```bash
bash /opt/galiledu/deploy/actualizar.sh
```

## Datos de prueba y seguridad

Por defecto el servidor carga los **usuarios de prueba** (contraseña `Galiledu2026`) porque el profesor los necesita para revisar cada rol. Como el repositorio es público, **cualquiera puede entrar como administrador** mientras estén activos. Para uso real, en `deploy/.env`:

```
FLYWAY_LOCATIONS=classpath:db/migration
```

(con una base nueva: los datos de prueba ya cargados no se borran solos).

## Operación diaria

| Quiero… | Comando (en el servidor) |
| --- | --- |
| Ver si todo está arriba | `cd /opt/galiledu && docker compose --env-file deploy/.env ps` |
| Ver logs del backend | `cd /opt/galiledu && docker compose --env-file deploy/.env logs -f backend` |
| Desplegar a mano | `bash /opt/galiledu/deploy/actualizar.sh` |
| Ver credenciales enviadas (sin SMTP) | `cd /opt/galiledu && docker compose --env-file deploy/.env logs backend \| grep Credenciales` |
| Copia de la BD | `cd /opt/galiledu && docker compose --env-file deploy/.env exec db pg_dump -U galiledu galiledu > respaldo.sql` |

## Problemas frecuentes

| Síntoma | Causa y solución |
| --- | --- |
| El workflow **Despliegue** falla con *Connection timed out* | El lab está apagado. Inícialo y relanza el workflow (Actions → Despliegue → Re-run). |
| La página no carga | El lab o el EC2 está apagado, o falta abrir el puerto 80 en el security group. |
| La compilación se queda sin memoria | Usa `t3.medium`, o confirma que el swap existe (`swapon --show`). |
| "Despliegue omitido: falta configurar los secretos" | Normal hasta hacer el paso 3. |
