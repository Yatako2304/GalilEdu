-- =====================================================================
-- Demo de calificaciones para probar a mano HU-60, HU-63, HU-64 y HU-69 en el entorno local.
-- Se carga desde classpath:db/demo, que está en las ubicaciones por defecto de Flyway (application.properties),
-- pero NO en las pruebas de integración (PruebaIntegracion) ni en el despliegue (docker-compose y deploy/.env).
--
-- Qué agrega (datos ficticios), siempre que el año 2026 esté ACTIVO:
--   · 1.° B de secundaria: tres estudiantes nuevos y María Fernández (U20260004) en Álgebra, Aritmética y
--     Geometría, para comparar secciones en HU-63.
--   · En 1.° A y 1.° B, para cada curso de María, dos ítems por periodo del I al III bimestre (Práctica 40 % y
--     Examen 60 % en ponderado) con notas. Solo si el curso aún no tiene ítems en ese periodo.
--   · Casos a propósito:
--       - Diego José (1.° A) no tiene el «Examen bimestral» de Geometría del III bimestre: su nota de
--         «Resuelve problemas de cantidad» queda Pendiente y se puede registrar fuera de plazo (HU-60).
--       - Camila Sofía (1.° A) es «No evaluado» en la Práctica de Álgebra del I bimestre.
--   · Carmen López Huamán (apoderada, U20269001, contraseña común Galiledu2026): madre de Ana Lucía García López
--     (1.° A) y de Joaquín García López (1.° B), para probar HU-69 con dos hijos.
--
-- Es idempotente y no pisa lo que ya existe: no cambia notas ni ítems guardados, ni usuarios con el mismo nombre.
-- =====================================================================
DO $$
DECLARE
    anio_demo UUID;
    escala_demo TEXT;
    regla_demo TEXT;
    docente_demo UUID := '10000000-0000-0000-0000-000000000004';
    seccion RECORD;
    periodo RECORD;
    curso RECORD;
    alumno RECORD;
    item RECORD;
    matricula_demo UUID;
    estudiante_demo UUID;
    persona_demo UUID;
    item_demo UUID;
    correlativo_demo INTEGER;
    nota_demo INTEGER;
    persona_apoderada UUID := '00000000-0000-0000-0000-000000000201';
    usuario_apoderada UUID := '10000000-0000-0000-0000-000000000201';
BEGIN
    SELECT ae.id, ae.escala_evaluacion, ae.regla_nota INTO anio_demo, escala_demo, regla_demo
    FROM configuracion.anio_escolar ae WHERE ae.anio = 2026 AND ae.estado = 'ACTIVO';
    IF anio_demo IS NULL THEN RETURN; END IF;

    -- ---------- 1.° B: tres estudiantes y María en sus tres cursos ----------
    SELECT s.id AS id INTO seccion
    FROM configuracion.seccion s
    WHERE s.anio_escolar_id = anio_demo AND s.grado_codigo = 'SEC-1' AND s.nombre = 'B';
    IF seccion.id IS NOT NULL THEN
        FOR alumno IN SELECT * FROM (VALUES
            (4, '99006104', 'García', 'López', 'Joaquín Andrés', DATE '2013-05-02', 'MASCULINO'),
            (5, '99006105', 'Rojas', 'Huamán', 'Valentina', DATE '2013-07-19', 'FEMENINO'),
            (6, '99006106', 'Flores', 'Castillo', 'Sebastián', DATE '2013-01-28', 'MASCULINO')
        ) AS d(numero, documento, paterno, materno, nombres, nacimiento, sexo)
        LOOP
            persona_demo := md5('demo-calificaciones-maria-persona-' || alumno.numero)::uuid;
            estudiante_demo := md5('demo-calificaciones-maria-estudiante-' || alumno.numero)::uuid;
            INSERT INTO personas.persona
                (id, tipo_documento, numero_documento, apellido_paterno, apellido_materno, nombres,
                 fecha_nacimiento, nacionalidad, sexo, distrito, direccion)
            VALUES (persona_demo, 'DNI', alumno.documento, alumno.paterno, alumno.materno, alumno.nombres,
                    alumno.nacimiento, 'Peruana', alumno.sexo, 'Lima', 'Dirección ficticia para pruebas')
            ON CONFLICT DO NOTHING;
            INSERT INTO estudiantes.estudiante
                (id, persona_id, pais_nacimiento, departamento_nacimiento, provincia_nacimiento, distrito_nacimiento,
                 lengua_materna, anio_academico, grado_codigo, forma_ingreso, modo_salida, estado)
            VALUES (estudiante_demo, persona_demo, 'Perú', 'Lima', 'Lima', 'Lima', 'Castellano', 2026, 'SEC-1',
                    'Edad normativa', 'SOLO', 'ACTIVO')
            ON CONFLICT DO NOTHING;
            IF NOT EXISTS (SELECT 1 FROM matricula.matricula m
                           WHERE m.estudiante_id = estudiante_demo AND m.anio_escolar_id = anio_demo) THEN
                INSERT INTO matricula.correlativo_matricula AS actual (anio, ultimo)
                SELECT 2026, coalesce(max(split_part(m.codigo, '-', 3)::integer), 0) + 1
                FROM matricula.matricula m WHERE m.codigo ~ '^MAT-2026-[0-9]+$'
                ON CONFLICT (anio) DO UPDATE SET ultimo = greatest(actual.ultimo,
                    (SELECT coalesce(max(split_part(m.codigo, '-', 3)::integer), 0)
                     FROM matricula.matricula m WHERE m.codigo ~ '^MAT-2026-[0-9]+$')) + 1
                RETURNING ultimo INTO correlativo_demo;
                INSERT INTO matricula.matricula
                    (id, estudiante_id, anio_escolar_id, grado_codigo, seccion_id, tipo_estudiante, estado,
                     codigo, confirmado_en, creado_por)
                VALUES (md5('demo-calificaciones-maria-matricula-' || alumno.numero)::uuid, estudiante_demo,
                        anio_demo, 'SEC-1', seccion.id, 'NUEVO', 'MATRICULADO',
                        'MAT-2026-' || lpad(correlativo_demo::text, 4, '0'), '2026-02-11 09:00-05',
                        '10000000-0000-0000-0000-000000000002')
                ON CONFLICT DO NOTHING;
            END IF;
        END LOOP;

        INSERT INTO academico.docente_seccion (plan_curso_id, seccion_id, docente_id)
        SELECT pc.id, seccion.id, docente_demo
        FROM academico.plan_curso pc JOIN academico.curso cu ON cu.id = pc.curso_id
        WHERE pc.anio_escolar_id = anio_demo AND pc.grado_codigo = 'SEC-1'
          AND cu.nombre IN ('Álgebra', 'Aritmética', 'Geometría')
        ON CONFLICT DO NOTHING;
    END IF;

    -- ---------- Ítems y notas del I al III bimestre en 1.° A y 1.° B ----------
    FOR seccion IN SELECT s.id, s.nombre FROM configuracion.seccion s
                   WHERE s.anio_escolar_id = anio_demo AND s.grado_codigo = 'SEC-1' AND s.nombre IN ('A', 'B')
    LOOP
        FOR curso IN SELECT pc.id, cu.nombre FROM academico.plan_curso pc
                     JOIN academico.curso cu ON cu.id = pc.curso_id
                     JOIN academico.docente_seccion ds ON ds.plan_curso_id = pc.id AND ds.seccion_id = seccion.id
                     WHERE pc.anio_escolar_id = anio_demo AND pc.grado_codigo = 'SEC-1'
                       AND cu.nombre IN ('Álgebra', 'Aritmética', 'Geometría') AND ds.docente_id = docente_demo
        LOOP
            FOR periodo IN SELECT p.id, p.numero, p.nombre FROM configuracion.periodo_academico p
                           WHERE p.anio_escolar_id = anio_demo AND p.numero <= 3 ORDER BY p.numero
            LOOP
                -- Si el docente ya armó ese periodo, se respeta tal cual.
                IF EXISTS (SELECT 1 FROM academico.item_evaluacion ie
                           WHERE ie.plan_curso_id = curso.id AND ie.seccion_id = seccion.id
                             AND ie.periodo_academico_id = periodo.id) THEN CONTINUE; END IF;

                FOR item IN SELECT * FROM (VALUES
                    ('Práctica calificada', 'PRACTICA', 40, 0),
                    ('Examen bimestral', 'EXAMEN', 60, 1)
                ) AS d(nombre, tipo, peso, orden)
                LOOP
                    item_demo := md5('demo-hu69-item-' || seccion.id || '-' || curso.id || '-' || periodo.id
                                     || '-' || item.orden)::uuid;
                    INSERT INTO academico.item_evaluacion
                        (id, plan_curso_id, anio_escolar_id, grado_codigo, seccion_id, periodo_academico_id,
                         nombre, descripcion, tipo, creado_por, creado_en, actualizado_en)
                    VALUES (item_demo, curso.id, anio_demo, 'SEC-1', seccion.id, periodo.id, item.nombre,
                            item.nombre || ' de ' || curso.nombre || ' (' || periodo.nombre || '). Dato ficticio.',
                            item.tipo, docente_demo, now() + make_interval(secs => item.orden),
                            now() + make_interval(secs => item.orden))
                    ON CONFLICT DO NOTHING;
                    INSERT INTO academico.item_competencia
                        (item_evaluacion_id, plan_curso_id, competencia_id, peso_porcentaje)
                    SELECT item_demo, curso.id, pc.competencia_id,
                           CASE WHEN regla_demo = 'PROMEDIO_PONDERADO' THEN item.peso END
                    FROM academico.plan_competencia pc WHERE pc.plan_curso_id = curso.id
                    ON CONFLICT DO NOTHING;

                    FOR alumno IN SELECT m.id AS matricula, p.nombres
                                  FROM matricula.matricula m
                                  JOIN estudiantes.estudiante e ON e.id = m.estudiante_id
                                  JOIN personas.persona p ON p.id = e.persona_id
                                  WHERE m.seccion_id = seccion.id AND m.estado = 'MATRICULADO'
                    LOOP
                        -- Caso a propósito: a Diego le falta el examen de Geometría del III bimestre.
                        IF alumno.nombres = 'Diego José' AND curso.nombre = 'Geometría' AND periodo.numero = 3
                           AND item.orden = 1 THEN CONTINUE; END IF;
                        -- Caso a propósito: Camila es «No evaluado» en la práctica de Álgebra del I bimestre.
                        IF alumno.nombres = 'Camila Sofía' AND curso.nombre = 'Álgebra' AND periodo.numero = 1
                           AND item.orden = 0 THEN
                            INSERT INTO academico.calificacion_item
                                (item_evaluacion_id, seccion_id, matricula_id, no_evaluado, motivo_no_evaluado,
                                 registrado_por)
                            VALUES (item_demo, seccion.id, alumno.matricula, TRUE,
                                    'Se incorporó a la sección después de la práctica.', docente_demo)
                            ON CONFLICT DO NOTHING;
                            CONTINUE;
                        END IF;
                        -- Nota ficticia estable: un nivel base por estudiante con una variación de -3 a +3.
                        nota_demo := least(20, greatest(0,
                            CASE alumno.nombres
                                WHEN 'Ana Lucía' THEN 16 WHEN 'Diego José' THEN 11 WHEN 'Camila Sofía' THEN 14
                                WHEN 'Joaquín Andrés' THEN 18 WHEN 'Valentina' THEN 9 WHEN 'Sebastián' THEN 13
                                ELSE 13 END
                            + abs(hashtext(item_demo::text || alumno.matricula::text)) % 7 - 3));
                        INSERT INTO academico.calificacion_item
                            (item_evaluacion_id, seccion_id, matricula_id, nota_numerica, nivel_logro, registrado_por)
                        VALUES (item_demo, seccion.id, alumno.matricula,
                                CASE WHEN escala_demo = 'VIGESIMAL' THEN nota_demo END,
                                CASE WHEN escala_demo = 'VIGESIMAL' THEN NULL
                                     WHEN nota_demo >= 18 THEN 'AD' WHEN nota_demo >= 14 THEN 'A'
                                     WHEN nota_demo >= 11 THEN 'B' ELSE 'C' END,
                                docente_demo)
                        ON CONFLICT DO NOTHING;
                    END LOOP;
                END LOOP;
            END LOOP;
        END LOOP;
    END LOOP;

    -- ---------- Apoderada: Carmen López Huamán (U20269001) ----------
    IF NOT EXISTS (SELECT 1 FROM seguridad.usuario u WHERE u.nombre_usuario = 'U20269001'
                   AND u.id <> usuario_apoderada) THEN
        INSERT INTO personas.persona
            (id, tipo_documento, numero_documento, apellido_paterno, apellido_materno, nombres, fecha_nacimiento,
             nacionalidad, sexo, celular, correo, distrito, direccion)
        VALUES (persona_apoderada, 'DNI', '70000201', 'López', 'Huamán', 'Carmen Rosa', '1984-03-12', 'Peruana',
                'FEMENINO', '987000201', 'carmen.lopez@correo.com', 'Lima', 'Dirección ficticia para pruebas')
        ON CONFLICT DO NOTHING;
        INSERT INTO seguridad.usuario (id, persona_id, nombre_usuario, contrasena_hash, estado)
        VALUES (usuario_apoderada, persona_apoderada, 'U20269001', crypt('Galiledu2026', gen_salt('bf', 10)),
                'ACTIVO')
        ON CONFLICT DO NOTHING;
        INSERT INTO seguridad.usuario_rol (usuario_id, rol_codigo)
        VALUES (usuario_apoderada, 'APODERADO')
        ON CONFLICT DO NOTHING;
        INSERT INTO estudiantes.vinculo_familiar
            (id, estudiante_id, persona_id, parentesco, vive_con_estudiante, tiene_patria_potestad,
             es_responsable_matricula)
        SELECT md5('demo-hu69-vinculo-' || d.numero)::uuid,
               md5('demo-calificaciones-maria-estudiante-' || d.numero)::uuid, persona_apoderada, 'MADRE', TRUE,
               TRUE, TRUE
        FROM (VALUES (1), (4)) AS d(numero)
        WHERE EXISTS (SELECT 1 FROM estudiantes.estudiante e
                      WHERE e.id = md5('demo-calificaciones-maria-estudiante-' || d.numero)::uuid)
        ON CONFLICT DO NOTHING;
    END IF;
END $$;
