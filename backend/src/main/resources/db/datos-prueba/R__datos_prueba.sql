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

-- ---------------------------------------------------------------------
-- Matrícula y pagos (V5). Familia de Luis Paredes (U20260003):
--   · Sofía (4.° de secundaria) matriculada en 2026, con pensiones pagadas hasta agosto y la de
--     septiembre vencida (para probar morosidad, deuda del apoderado y constancias).
--   · Mateo (inscrito para 2027) con sección reservada en 1.° A de 2027.
-- ---------------------------------------------------------------------
INSERT INTO configuracion.seccion (anio_escolar_id, grado_codigo, nombre, capacidad_maxima)
SELECT '30000000-0000-0000-0000-000000002027', g.codigo, s.nombre, 30
FROM configuracion.grado g CROSS JOIN (VALUES ('A'), ('B')) AS s (nombre)
WHERE g.nivel = 'SECUNDARIA'
ON CONFLICT DO NOTHING;

INSERT INTO matricula.periodo_matricula (anio_escolar_id, inicio, cierre, bloquear_por_morosidad, permitir_extemporanea, estado)
VALUES ('30000000-0000-0000-0000-000000002026', '2025-11-02 08:00-05', '2026-02-27 23:59-05', TRUE, TRUE, 'CERRADO')
ON CONFLICT DO NOTHING;

-- Los 4 documentos de matrícula de Figma, para 2026 y 2027.
INSERT INTO matricula.documento_requerido (periodo_matricula_id, nombre, descripcion, orden)
SELECT p.id, d.nombre, d.descripcion, d.orden
FROM matricula.periodo_matricula p
CROSS JOIN (VALUES ('Ficha de matrícula firmada', 'Descarga la ficha, fírmala y súbela.', 1),
                   ('DNI del estudiante', 'Copia legible por ambas caras.', 2),
                   ('DNI del apoderado', 'Copia legible por ambas caras.', 3),
                   ('Declaración jurada del apoderado', NULL, 4)) AS d (nombre, descripcion, orden)
ON CONFLICT DO NOTHING;

-- Tarifario 2026 (confirmado): matrícula S/ 420 y 10 pensiones de S/ 600 (Figma P8).
INSERT INTO pagos.tarifario (id, anio_escolar_id, monto_cuota_ingreso, monto_matricula, vencimiento_matricula, monto_pension,
                             cantidad_cuotas, dias_gracia, recargo_porcentaje, tope_recargo_porcentaje, estado)
VALUES ('32000000-0000-0000-0000-000000002026', '30000000-0000-0000-0000-000000002026', 500, 420, '2026-02-28', 600, 10, 5, 2,
        10, 'CONFIRMADO')
ON CONFLICT DO NOTHING;

INSERT INTO pagos.cuota_tarifario (tarifario_id, numero, nombre, fecha_vencimiento)
SELECT '32000000-0000-0000-0000-000000002026', m.numero, 'Pensión de ' || m.mes,
       CASE WHEN m.numero = 10 THEN DATE '2026-12-15'
            ELSE (DATE '2026-03-01' + make_interval(months => m.numero) - INTERVAL '1 day')::date END
FROM (VALUES (1, 'marzo'), (2, 'abril'), (3, 'mayo'), (4, 'junio'), (5, 'julio'), (6, 'agosto'),
             (7, 'septiembre'), (8, 'octubre'), (9, 'noviembre'), (10, 'diciembre')) AS m (numero, mes)
ON CONFLICT DO NOTHING;

-- Catálogo de descuentos (Figma P21).
INSERT INTO pagos.descuento (id, nombre, descripcion, tipo, aplicacion, porcentaje, condicion, estado)
VALUES
    ('52000000-0000-0000-0000-000000000001', 'Hermanos matriculados',
     'Para familias con dos o más hijos matriculados en el mismo año escolar.', 'POR_HERMANOS', 'AUTOMATICO', NULL,
     'Dos o más hermanos matriculados con el mismo apoderado', 'ACTIVO'),
    ('52000000-0000-0000-0000-000000000002', 'Beca institucional', NULL, 'PORCENTAJE_UNICO', 'MANUAL', 50,
     'Aprobada por dirección', 'ACTIVO'),
    ('52000000-0000-0000-0000-000000000003', 'Acuerdo particular', NULL, 'PORCENTAJE_UNICO', 'MANUAL', 20,
     'Convenio con la familia', 'ACTIVO'),
    ('52000000-0000-0000-0000-000000000004', 'Deportista destacado', NULL, 'PORCENTAJE_UNICO', 'MANUAL', 25,
     'Representa al colegio', 'INACTIVO')
ON CONFLICT DO NOTHING;

INSERT INTO pagos.descuento_tramo (descuento_id, hermanos_desde, hermanos_hasta, porcentaje)
VALUES ('52000000-0000-0000-0000-000000000001', 2, 2, 10),
       ('52000000-0000-0000-0000-000000000001', 3, NULL, 15)
ON CONFLICT DO NOTHING;

INSERT INTO pagos.descuento_concepto (descuento_id, concepto_codigo)
VALUES ('52000000-0000-0000-0000-000000000001', 'PENSION'),
       ('52000000-0000-0000-0000-000000000002', 'PENSION'),
       ('52000000-0000-0000-0000-000000000002', 'MATRICULA'),
       ('52000000-0000-0000-0000-000000000003', 'PENSION'),
       ('52000000-0000-0000-0000-000000000004', 'PENSION')
ON CONFLICT DO NOTHING;

-- Sofía Paredes Rojas: hermana de Mateo, matriculada en 4.° A de 2026.
INSERT INTO personas.persona (id, tipo_documento, numero_documento, apellido_paterno, apellido_materno, nombres,
                              fecha_nacimiento, nacionalidad, sexo, distrito, direccion)
VALUES ('00000000-0000-0000-0000-000000000102', 'DNI', '74125896', 'Paredes', 'Rojas', 'Sofía',
        '2011-06-20', 'Peruana', 'FEMENINO', 'Magdalena del Mar', 'Calle 123')
ON CONFLICT DO NOTHING;

INSERT INTO estudiantes.estudiante (id, persona_id, pais_nacimiento, lengua_materna, anio_academico, nivel, grado,
                                    forma_ingreso, modo_salida, estado)
VALUES ('20000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000102', 'Perú', 'Castellano', 2026,
        'SECUNDARIA', '4.° grado', 'Edad normativa', 'SOLO', 'MATRICULADO')
ON CONFLICT DO NOTHING;

INSERT INTO estudiantes.ficha_medica (estudiante_id, tipo_sangre, tipo_seguro)
VALUES ('20000000-0000-0000-0000-000000000002', 'O+', 'EsSalud')
ON CONFLICT DO NOTHING;

INSERT INTO estudiantes.vinculo_familiar (id, estudiante_id, persona_id, parentesco, vive_con_estudiante,
                                          tiene_patria_potestad, es_responsable_matricula)
VALUES ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000003', 'PADRE', TRUE, TRUE, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO estudiantes.contacto_emergencia (id, estudiante_id, nombre_completo, parentesco, celular, orden)
VALUES ('40000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002', 'Rosa Medina Torres', 'Abuela', '987654321', 1),
       ('40000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000002', 'Pedro Rojas Medina', 'Tío', '976543210', 2)
ON CONFLICT DO NOTHING;

INSERT INTO matricula.matricula (id, estudiante_id, anio_escolar_id, grado_codigo, seccion_id, tipo_estudiante, estado,
                                 codigo, confirmado_en, creado_por)
SELECT '50000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002',
       '30000000-0000-0000-0000-000000002026', 'SEC-4', s.id, 'CONTINUADOR', 'MATRICULADO', 'MAT-2026-0001',
       '2026-02-12 10:30-05', '10000000-0000-0000-0000-000000000002'
FROM configuracion.seccion s
WHERE s.anio_escolar_id = '30000000-0000-0000-0000-000000002026' AND s.grado_codigo = 'SEC-4' AND s.nombre = 'A'
ON CONFLICT DO NOTHING;

INSERT INTO matricula.matricula (id, estudiante_id, anio_escolar_id, grado_codigo, seccion_id, tipo_estudiante, estado,
                                 creado_por)
SELECT '50000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
       '30000000-0000-0000-0000-000000002027', 'SEC-1', s.id, 'NUEVO', 'SECCION_RESERVADA',
       '10000000-0000-0000-0000-000000000002'
FROM configuracion.seccion s
WHERE s.anio_escolar_id = '30000000-0000-0000-0000-000000002027' AND s.grado_codigo = 'SEC-1' AND s.nombre = 'A'
ON CONFLICT DO NOTHING;

INSERT INTO matricula.correlativo_matricula (anio, ultimo) VALUES (2026, 1)
ON CONFLICT (anio) DO UPDATE SET ultimo = GREATEST(matricula.correlativo_matricula.ultimo, EXCLUDED.ultimo);

-- Compromiso 2026 de Sofía: matrícula y pensiones de marzo a agosto pagadas; septiembre vencida.
INSERT INTO pagos.compromiso_pago (id, estudiante_id, anio_escolar_id, matricula_id, tarifario_id)
VALUES ('51000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000002026', '50000000-0000-0000-0000-000000000001',
        '32000000-0000-0000-0000-000000002026')
ON CONFLICT DO NOTHING;

INSERT INTO pagos.cuota (id, compromiso_id, concepto_codigo, numero, nombre, monto_base, monto_pagado, fecha_vencimiento,
                         estado, fecha_pago)
VALUES (md5('galiledu-cuota-sofia-0')::uuid, '51000000-0000-0000-0000-000000000001', 'MATRICULA', NULL,
        'Matrícula 2026', 420, 420, '2026-02-28', 'PAGADA', '2026-02-12')
ON CONFLICT DO NOTHING;

INSERT INTO pagos.cuota (id, compromiso_id, concepto_codigo, numero, nombre, monto_base, monto_pagado, fecha_vencimiento,
                         estado, fecha_pago)
SELECT md5('galiledu-cuota-sofia-' || ct.numero)::uuid, '51000000-0000-0000-0000-000000000001', 'PENSION', ct.numero,
       ct.nombre, 600, CASE WHEN ct.numero <= 6 THEN 600 ELSE 0 END, ct.fecha_vencimiento,
       CASE WHEN ct.numero <= 6 THEN 'PAGADA' ELSE 'PENDIENTE' END,
       CASE WHEN ct.numero <= 6 THEN ct.fecha_vencimiento - 6 END
FROM pagos.cuota_tarifario ct
WHERE ct.tarifario_id = '32000000-0000-0000-0000-000000002026'
ON CONFLICT DO NOTHING;

-- Un pago confirmado (con su constancia) por cada cuota pagada.
INSERT INTO pagos.pago (id, estudiante_id, numero_constancia, fecha_pago, referencia, monto_total)
SELECT md5('galiledu-pago-sofia-' || coalesce(c.numero, 0))::uuid, '20000000-0000-0000-0000-000000000002',
       'C-2026-' || lpad((coalesce(c.numero, 0) + 1)::text, 5, '0'), c.fecha_pago + TIME '10:00',
       'TX' || (848568 + coalesce(c.numero, 0) * 7), c.monto_pagado
FROM pagos.cuota c
WHERE c.compromiso_id = '51000000-0000-0000-0000-000000000001' AND c.estado = 'PAGADA'
ON CONFLICT DO NOTHING;

INSERT INTO pagos.pago_detalle (pago_id, cuota_id, capital, recargo)
SELECT md5('galiledu-pago-sofia-' || coalesce(c.numero, 0))::uuid, c.id, c.monto_pagado, 0
FROM pagos.cuota c
WHERE c.compromiso_id = '51000000-0000-0000-0000-000000000001' AND c.estado = 'PAGADA'
ON CONFLICT DO NOTHING;

INSERT INTO pagos.correlativo_constancia (anio, ultimo) VALUES (2026, 7)
ON CONFLICT (anio) DO UPDATE SET ultimo = GREATEST(pagos.correlativo_constancia.ultimo, EXCLUDED.ultimo);
