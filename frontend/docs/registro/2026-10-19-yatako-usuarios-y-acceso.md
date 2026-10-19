# Base común (BD + backend + frontend) y módulo Usuarios y acceso

- **Autor:** Yatako2304 (con asistencia de IA)
- **Fecha:** 2026-10-19
- **HU / tarea:** HU-1, HU-6, HU-7, HU-9, HU-12 · Iteración 1
- **Estado:** terminado

## Qué hice

Se unió frontend y backend en un solo repositorio (`GalilEdu/`, rama `feat/integracion`), se rehízo la BD con migraciones Flyway y el backend quedó solo con personas, usuarios y estudiantes. El frontend ya usa el backend real para login, la gestión de usuarios del administrador y la inscripción de estudiantes, siguiendo las pantallas de Figma.

## Archivos principales

- `backend/src/main/resources/db/migration/`: esquema de la BD (V1 personas/seguridad, V2 estudiantes).
- `backend/src/main/java/com/galiledu/usuarios/`: login JWT y gestión de cuentas.
- `backend/src/main/java/com/galiledu/estudiantes/`: inscripción (HU-7).
- `src/features/usuarios/`: Inicio del administrador, lista y modales de usuarios.
- `src/features/estudiantes/`: listado, registro de inscripción en 5 pasos y ficha.
- `src/components/personas/`: campos y validaciones de datos personales compartidos.

## Decisiones y por qué

- **Decisión:** Figma es la fuente de verdad; los RF de Notion son apoyo.
  **Por qué:** varios RF se contradicen entre sí y con las pantallas validadas.
- **Decisión:** JWT en lugar de sesión por cookie.
  **Por qué:** el frontend ya estaba preparado para `{ jwt, user }`, evita CSRF y facilita desplegar front y back por separado.
- **Decisión:** el estudiante no tiene usuario; su responsable de matrícula (padre, madre o apoderado) tiene la cuenta de la familia con rol Apoderado.
  **Por qué:** así lo muestra Figma (Listado e Inscripción de estudiantes). Por eso Estudiantes está en el menú de Personal administrativo y el formulario "Nuevo usuario" no ofrece el rol Estudiante.
- **Decisión:** un usuario puede tener varios roles; "Eliminar" es borrado lógico.
  **Por qué:** Figma muestra varios roles por usuario y la opción Eliminar; borrar físicamente rompería el historial.
- **Decisión:** las credenciales se envían por correo (Figma: "Credenciales enviadas al correo"); sin SMTP configurado se escriben en el log del backend.
- **Decisión:** MSW en el navegador solo simula módulos sin backend (`browserHandlers`); los tests siguen simulando todo.
- **Decisión:** se corrigió `src/utils/cn.ts` para que `tailwind-merge` reconozca los tamaños de letra del design system (`text-body-3`, etc.).
  **Por qué:** los confundía con colores y borraba `text-white`, por eso el texto de los botones primarios no se veía.

## Pendientes / problemas conocidos

- [ ] HU-8 Carga masiva (botón deshabilitado).
- [ ] Recuperar y cambiar contraseña (HU-4, HU-5).
- [ ] Editar datos de una inscripción e "Iniciar matrícula" desde la ficha.
- [ ] Configurar SMTP real para el envío de credenciales.

## Notas para quien siga

- Datos de prueba y contraseña: ver el README principal.
- Para reiniciar la BD local, borra y crea de nuevo la base `galiledu`; Flyway la vuelve a poblar al arrancar.
- Nunca edites una migración ya aplicada: crea una nueva `V{n}__...sql`.
