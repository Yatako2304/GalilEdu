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

-- ---------------------------------------------------------------------
-- Configuración del año escolar (V4), con los valores de las pantallas de Figma.
-- 2026 activo (bimestral) y 2027 planificado (trimestral).
-- ---------------------------------------------------------------------
INSERT INTO configuracion.anio_escolar (id, anio, fecha_inicio, fecha_fin, regimen, estado)
VALUES
    ('30000000-0000-0000-0000-000000002026', 2026, '2026-03-02', '2026-12-18', 'BIMESTRAL', 'ACTIVO'),
    ('30000000-0000-0000-0000-000000002027', 2027, '2027-03-01', '2027-12-17', 'TRIMESTRAL', 'PLANIFICADO')
ON CONFLICT DO NOTHING;

INSERT INTO configuracion.periodo_academico (anio_escolar_id, numero, nombre, fecha_inicio, fecha_fin, fecha_limite_notas, estado)
VALUES
    ('30000000-0000-0000-0000-000000002026', 1, 'I Bimestre',   '2026-03-02', '2026-05-08', '2026-05-15', 'CERRADO'),
    ('30000000-0000-0000-0000-000000002026', 2, 'II Bimestre',  '2026-05-18', '2026-07-24', '2026-07-31', 'CERRADO'),
    ('30000000-0000-0000-0000-000000002026', 3, 'III Bimestre', '2026-08-03', '2026-10-09', '2026-10-16', 'EN_CURSO'),
    ('30000000-0000-0000-0000-000000002026', 4, 'IV Bimestre',  '2026-10-19', '2026-12-18', '2026-12-23', 'PLANIFICADO'),
    ('30000000-0000-0000-0000-000000002027', 1, 'I Trimestre',  '2027-03-01', '2027-05-31', '2027-06-05', 'PLANIFICADO'),
    ('30000000-0000-0000-0000-000000002027', 2, 'II Trimestre', '2027-06-08', '2027-09-07', '2027-09-12', 'PLANIFICADO'),
    ('30000000-0000-0000-0000-000000002027', 3, 'III Trimestre','2027-09-15', '2027-12-17', '2027-12-22', 'PLANIFICADO')
ON CONFLICT DO NOTHING;

-- Secciones A y B de secundaria en 2026. María Fernández (Docente + Tutor) es tutora de 1.° A.
INSERT INTO configuracion.seccion (anio_escolar_id, grado_codigo, nombre, capacidad_maxima, tutor_id)
SELECT '30000000-0000-0000-0000-000000002026', g.codigo, s.nombre, 30,
       CASE WHEN g.codigo = 'SEC-1' AND s.nombre = 'A' THEN '10000000-0000-0000-0000-000000000004'::uuid END
FROM configuracion.grado g CROSS JOIN (VALUES ('A'), ('B')) AS s (nombre)
WHERE g.nivel = 'SECUNDARIA'
ON CONFLICT DO NOTHING;

INSERT INTO configuracion.estructura_horaria (id, anio_escolar_id, codigo, descripcion, dias_lectivos, hora_entrada,
                                              bloques_por_dia, duracion_bloque_min)
VALUES
    ('31000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000002026', 'EH-PRI-26',
     'Primaria · jornada de mañana', ARRAY['LUN', 'MAR', 'MIE', 'JUE', 'VIE'], '07:45', 8, 45),
    ('31000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000002026', 'EH-SEC-26',
     'Secundaria · jornada de mañana', ARRAY['LUN', 'MAR', 'MIE', 'JUE', 'VIE'], '07:30', 9, 45)
ON CONFLICT DO NOTHING;

INSERT INTO configuracion.receso (estructura_horaria_id, despues_de_bloque, duracion_min)
VALUES
    ('31000000-0000-0000-0000-000000000001', 3, 20),
    ('31000000-0000-0000-0000-000000000001', 6, 15),
    ('31000000-0000-0000-0000-000000000002', 3, 20),
    ('31000000-0000-0000-0000-000000000002', 6, 15)
ON CONFLICT DO NOTHING;

-- Matrícula 2027 habilitada (15/09/2026 – 31/10/2026) y tarifario 2027 en configuración.
INSERT INTO matricula.periodo_matricula (anio_escolar_id, inicio, cierre, bloquear_por_morosidad, permitir_extemporanea, estado)
VALUES ('30000000-0000-0000-0000-000000002027', '2026-09-15 08:00-05', '2026-10-31 23:59-05', TRUE, TRUE, 'HABILITADO')
ON CONFLICT DO NOTHING;

INSERT INTO pagos.tarifario (id, anio_escolar_id, monto_cuota_ingreso, monto_matricula, vencimiento_matricula, monto_pension,
                             cantidad_cuotas, dias_gracia, recargo_porcentaje, tope_recargo_porcentaje)
VALUES ('32000000-0000-0000-0000-000000002027', '30000000-0000-0000-0000-000000002027', 500, 450, '2027-02-28', 620, 10, 5, 2, 10)
ON CONFLICT DO NOTHING;

INSERT INTO pagos.cuota_tarifario (tarifario_id, numero, nombre, fecha_vencimiento)
SELECT '32000000-0000-0000-0000-000000002027', m.numero, 'Pensión de ' || m.mes,
       CASE WHEN m.numero = 10 THEN DATE '2027-12-15'
            ELSE (DATE '2027-03-01' + make_interval(months => m.numero) - INTERVAL '1 day')::date END
FROM (VALUES (1, 'marzo'), (2, 'abril'), (3, 'mayo'), (4, 'junio'), (5, 'julio'), (6, 'agosto'),
             (7, 'septiembre'), (8, 'octubre'), (9, 'noviembre'), (10, 'diciembre')) AS m (numero, mes)
ON CONFLICT DO NOTHING;
