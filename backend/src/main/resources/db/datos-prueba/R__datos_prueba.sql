-- =====================================================================
-- Datos de prueba para desarrollo y demos (NO usar en producción).
-- Se cargan porque FLYWAY_LOCATIONS incluye classpath:db/datos-prueba (ver application.properties).
-- Todas las cuentas usan la contraseña: Galiledu2026
-- Es idempotente: se puede volver a ejecutar sin duplicar datos.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO personas.persona (id, tipo_documento, numero_documento, apellido_paterno, apellido_materno, nombres,
                              fecha_nacimiento, nacionalidad, sexo, celular, correo, distrito, direccion)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'DNI', '70000001', 'Curo', 'Reyes', 'Selene', '1995-03-12', 'Peruana', 'FEMENINO', '987000001', 'selene.curo@galiledu.edu.pe', 'San Miguel', 'Av. La Marina 100'),
    ('00000000-0000-0000-0000-000000000002', 'DNI', '70000002', 'Torres', 'Vega', 'Ana', '1990-07-21', 'Peruana', 'FEMENINO', '987000002', 'ana.torres@galiledu.edu.pe', 'Jesús María', 'Jr. Huiracocha 1450'),
    ('00000000-0000-0000-0000-000000000003', 'DNI', '70000003', 'Paredes', 'Silva', 'Luis', '1985-01-05', 'Peruana', 'MASCULINO', '987000003', 'luis.paredes@galiledu.edu.pe', 'Magdalena del Mar', 'Calle 123'),
    ('00000000-0000-0000-0000-000000000004', 'DNI', '70000004', 'Fernández', 'Ríos', 'María', '1988-11-30', 'Peruana', 'FEMENINO', '987000004', 'maria.fernandez@galiledu.edu.pe', 'Lince', 'Av. Arequipa 2500'),
    ('00000000-0000-0000-0000-000000000005', 'DNI', '70000005', 'Ramírez', 'Soto', 'Carlos', '1982-04-18', 'Peruana', 'MASCULINO', '987000005', 'carlos.ramirez@galiledu.edu.pe', 'Pueblo Libre', 'Av. Bolívar 800'),
    ('00000000-0000-0000-0000-000000000006', 'DNI', '70000006', 'Sánchez', 'Ferrara', 'Valeria', '2009-11-02', 'Peruana', 'FEMENINO', '987000006', 'valeria.sanchez@galiledu.edu.pe', 'Magdalena del Mar', 'Calle 123'),
    ('00000000-0000-0000-0000-000000000007', 'DNI', '70000007', 'Montoya', 'Spray', 'Leonardo', '1992-09-09', 'Peruana', 'MASCULINO', '987000007', 'leo.montoya@galiledu.edu.pe', 'Surco', 'Av. Primavera 300')
ON CONFLICT DO NOTHING;

INSERT INTO seguridad.usuario (id, persona_id, nombre_usuario, contrasena_hash, estado)
VALUES
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'U20260001', crypt('Galiledu2026', gen_salt('bf', 10)), 'ACTIVO'),
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 'U20260002', crypt('Galiledu2026', gen_salt('bf', 10)), 'ACTIVO'),
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003', 'U20260003', crypt('Galiledu2026', gen_salt('bf', 10)), 'ACTIVO'),
    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000004', 'U20260004', crypt('Galiledu2026', gen_salt('bf', 10)), 'ACTIVO'),
    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000005', 'U20260005', crypt('Galiledu2026', gen_salt('bf', 10)), 'ACTIVO'),
    ('10000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000006', 'U20260006', crypt('Galiledu2026', gen_salt('bf', 10)), 'ACTIVO'),
    ('10000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000007', 'U20260007', crypt('Galiledu2026', gen_salt('bf', 10)), 'INACTIVO')
ON CONFLICT DO NOTHING;

INSERT INTO seguridad.usuario_rol (usuario_id, rol_codigo)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'ADMINISTRADOR'),
    ('10000000-0000-0000-0000-000000000002', 'PERSONAL_ADMINISTRATIVO'),
    ('10000000-0000-0000-0000-000000000003', 'APODERADO'),
    ('10000000-0000-0000-0000-000000000004', 'DOCENTE'),
    ('10000000-0000-0000-0000-000000000004', 'TUTOR'),
    ('10000000-0000-0000-0000-000000000005', 'COORDINADOR'),
    ('10000000-0000-0000-0000-000000000006', 'ESTUDIANTE'),
    ('10000000-0000-0000-0000-000000000007', 'ADMINISTRADOR')
ON CONFLICT DO NOTHING;

-- Los próximos usuarios creados en 2026 continúan desde U20260008.
INSERT INTO seguridad.correlativo_usuario (anio, ultimo)
VALUES (2026, 7)
ON CONFLICT (anio) DO UPDATE SET ultimo = GREATEST(seguridad.correlativo_usuario.ultimo, EXCLUDED.ultimo);

-- Un estudiante inscrito (sin usuario) cuyo responsable es Luis Paredes.
INSERT INTO personas.persona (id, tipo_documento, numero_documento, apellido_paterno, apellido_materno, nombres,
                              fecha_nacimiento, nacionalidad, sexo, distrito, direccion)
VALUES ('00000000-0000-0000-0000-000000000101', 'DNI', '78945612', 'Paredes', 'Rojas', 'Mateo Alonso',
        '2014-02-14', 'Peruana', 'MASCULINO', 'Magdalena del Mar', 'Calle 123')
ON CONFLICT DO NOTHING;

INSERT INTO estudiantes.estudiante (id, persona_id, pais_nacimiento, departamento_nacimiento, provincia_nacimiento,
                                    distrito_nacimiento, lengua_materna, anio_academico, nivel, grado, forma_ingreso,
                                    modo_salida, estado)
VALUES ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000101', 'Perú', 'Lima', 'Lima',
        'Magdalena del Mar', 'Castellano', 2027, 'SECUNDARIA', '1.° grado', 'Edad normativa', 'PERSONA_AUTORIZADA',
        'INSCRITO')
ON CONFLICT DO NOTHING;

INSERT INTO estudiantes.ficha_medica (estudiante_id, tipo_sangre, tipo_seguro)
VALUES ('20000000-0000-0000-0000-000000000001', 'O+', 'EsSalud')
ON CONFLICT DO NOTHING;

INSERT INTO estudiantes.vinculo_familiar (id, estudiante_id, persona_id, parentesco, vive_con_estudiante,
                                          tiene_patria_potestad, es_responsable_matricula)
VALUES ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000003', 'PADRE', TRUE, TRUE, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO estudiantes.contacto_emergencia (id, estudiante_id, nombre_completo, parentesco, celular, orden)
VALUES ('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Rosa Medina Torres', 'Abuela', '987654321', 1),
       ('40000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'Pedro Rojas Medina', 'Tío', '976543210', 2)
ON CONFLICT DO NOTHING;
