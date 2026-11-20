# Despliegue en AWS (EC2)

GalilEdu corre en **un solo servidor EC2** con Docker. Después de configurarlo una vez, **cada merge a `main` con el CI en verde se despliega solo**.

```
Merge a main ─► CI (Backend + Frontend) ✅ ─► workflow "Despliegue" ─SSH─► EC2: deploy/actualizar.sh
                                                                              │
Internet :80/:443 ─► caddy (HTTPS) ─► frontend (nginx) ──/api──► backend (Spring Boot) ──► db (PostgreSQL 18, volumen)
```

| Archivo | Para qué |
| --- | --- |
| [`docker-compose.yml`](../../docker-compose.yml) | Los 4 contenedores y cómo se conectan. Solo caddy queda abierto (80 y 443). |
| [`deploy/Caddyfile`](../../deploy/Caddyfile) | Caddy: HTTPS con el certificado de Let's Encrypt (si hay `DOMINIO`) y reenvío al nginx. |
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
   - **Network settings → Edit → Security group:** permitir **SSH (22)**, **HTTP (80)** y **HTTPS (443)** desde *Anywhere* (`0.0.0.0/0`).
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

## 4. Dominio, HTTPS y correo

Sin dominio, la app se abre por `http://IP-del-servidor`. Para usar HTTPS (lo exige, por ejemplo, el inicio de sesión con Google) hace falta un **dominio**: ni Google ni Let's Encrypt aceptan una IP.

### 4.1 Dominio gratis (DuckDNS)

1. Entra a <https://www.duckdns.org> con tu cuenta (Google o GitHub).
2. En **sub domain** escribe el nombre (ej. `galiledu`) → **add domain**.
3. En **current ip** borra la IP que aparece (es la de tu casa), escribe la **Elastic IP** del EC2 → **update ip**.
4. Comprueba: `Resolve-DnsName galiledu.duckdns.org` (PowerShell) o `nslookup galiledu.duckdns.org` debe responder la Elastic IP.

> El token de DuckDNS es como una contraseña: no lo subas al repo. Si la Elastic IP cambia, actualízala en DuckDNS.

### 4.2 Abrir el puerto 443 en AWS

EC2 → **Instances** → `galiledu` → pestaña **Security** → el security group → **Edit inbound rules → Add rule**: Type **HTTPS**, Port **443**, Source **0.0.0.0/0** → **Save rules**. Deja también **HTTP (80)**: Let's Encrypt lo usa para entregar el certificado y Caddy lo usa para redirigir a HTTPS.

### 4.3 Configurar el servidor

Entra por SSH (`ssh -i labsuser.pem ubuntu@ELASTIC-IP`) y edita `nano /opt/galiledu/deploy/.env`:

```
DOMINIO=galiledu.duckdns.org
URL_PUBLICA=https://galiledu.duckdns.org

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USER=la-cuenta@gmail.com
MAIL_PASSWORD=la-contraseña-de-aplicación
MAIL_FROM=GalilEdu <la-cuenta@gmail.com>
```

Si tu `.env` es antiguo y no tiene la línea `DOMINIO`, agrégala. Guarda (Ctrl+O, Enter, Ctrl+X) y aplica:

```bash
bash /opt/galiledu/deploy/actualizar.sh
```

La primera vez, Caddy tarda unos segundos en obtener el certificado. Luego abre `https://galiledu.duckdns.org` (con candado); `http://` redirige solo a `https://`. El certificado se renueva solo y se guarda en el volumen `datos_caddy`, así que los despliegues no lo vuelven a pedir.

> **Correo:** la contraseña es una *contraseña de aplicación* de Google (Cuenta de Google → Seguridad → Verificación en dos pasos → Contraseñas de aplicaciones), no la contraseña normal. Va solo en `deploy/.env` del servidor, nunca en Git. Para probarlo, crea un usuario con un correo real: debe recibir sus credenciales.

### 4.4 Inicio de sesión con Google (cuando esté en `main`)

En Google Cloud → **Credenciales** → el cliente OAuth → **Orígenes autorizados de JavaScript**, agrega `https://galiledu.duckdns.org` (el mismo valor de `URL_PUBLICA`; no hace falta URI de redireccionamiento). Luego, en `deploy/.env`, completa las variables de Google que indique esa funcionalidad y vuelve a correr `actualizar.sh`.

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
| La página no carga | El lab o el EC2 está apagado, o falta abrir el puerto 80 (y el 443 con dominio) en el security group. |
| `https://` no carga o el navegador avisa "no seguro" | El dominio no apunta a la Elastic IP (`Resolve-DnsName`), falta el puerto 443, o Caddy aún no obtiene el certificado. Mira `docker compose --env-file deploy/.env logs caddy`. |
| No llegan los correos | Revisa `MAIL_*` en `deploy/.env` (la contraseña de aplicación, sin espacios) y `docker compose --env-file deploy/.env logs backend \| grep -i mail`. |
| La compilación se queda sin memoria | Usa `t3.medium`, o confirma que el swap existe (`swapon --show`). |
| "Despliegue omitido: falta configurar los secretos" | Normal hasta hacer el paso 3. |
