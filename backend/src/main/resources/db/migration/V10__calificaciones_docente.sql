-- =====================================================================
-- V10 · Calificaciones del docente (HU-57, HU-59 y HU-62).
-- Los ítems pertenecen a un curso y periodo; sus competencias deben ser
-- parte del plan del curso. Cada estudiante matriculado tiene como máximo
-- una calificación y una evidencia por ítem.
-- =====================================================================

-- Las claves compuestas permiten que PostgreSQL compruebe que curso, sección,
-- periodo y matrícula pertenecen al mismo año y grado, además de las validaciones
-- de permisos que realiza la aplicación.
ALTER TABLE academico.plan_curso
    ADD CONSTRAINT uq_plan_curso_id_anio_grado UNIQUE (id, anio_escolar_id, grado_codigo);
ALTER TABLE configuracion.seccion
    ADD CONSTRAINT uq_seccion_id_anio_grado UNIQUE (id, anio_escolar_id, grado_codigo);
ALTER TABLE configuracion.periodo_academico
    ADD CONSTRAINT uq_periodo_id_anio UNIQUE (id, anio_escolar_id);
ALTER TABLE matricula.matricula
    ADD CONSTRAINT uq_matricula_id_seccion UNIQUE (id, seccion_id);

CREATE TABLE academico.item_evaluacion (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_curso_id         UUID         NOT NULL,
    anio_escolar_id       UUID         NOT NULL,
    grado_codigo          VARCHAR(10)  NOT NULL,
    seccion_id            UUID         NOT NULL,
    periodo_academico_id  UUID         NOT NULL,
    nombre                VARCHAR(120) NOT NULL,
    descripcion           VARCHAR(500) NOT NULL,
    tipo                  VARCHAR(24)  NOT NULL,
    creado_por            UUID         NOT NULL REFERENCES seguridad.usuario (id),
    creado_en             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_item_evaluacion_plan_seccion_periodo_nombre
        UNIQUE (plan_curso_id, seccion_id, periodo_academico_id, nombre),
    CONSTRAINT uq_item_evaluacion_id_plan UNIQUE (id, plan_curso_id),
    CONSTRAINT uq_item_evaluacion_id_seccion UNIQUE (id, seccion_id),
    CONSTRAINT fk_item_evaluacion_plan
        FOREIGN KEY (plan_curso_id, anio_escolar_id, grado_codigo)
        REFERENCES academico.plan_curso (id, anio_escolar_id, grado_codigo) ON DELETE CASCADE,
    CONSTRAINT fk_item_evaluacion_seccion
        FOREIGN KEY (seccion_id, anio_escolar_id, grado_codigo)
        REFERENCES configuracion.seccion (id, anio_escolar_id, grado_codigo),
    CONSTRAINT fk_item_evaluacion_periodo
        FOREIGN KEY (periodo_academico_id, anio_escolar_id)
        REFERENCES configuracion.periodo_academico (id, anio_escolar_id),
    CONSTRAINT ck_item_evaluacion_nombre CHECK (length(trim(nombre)) BETWEEN 1 AND 120),
    CONSTRAINT ck_item_evaluacion_descripcion CHECK (length(trim(descripcion)) BETWEEN 1 AND 500),
    CONSTRAINT ck_item_evaluacion_tipo CHECK (tipo IN (
        'PRACTICA', 'EXAMEN', 'PROYECTO', 'TRABAJO_INDIVIDUAL', 'EXPOSICION', 'PORTAFOLIO', 'OTRO'))
);

CREATE INDEX ix_item_evaluacion_plan_seccion_periodo
    ON academico.item_evaluacion (plan_curso_id, seccion_id, periodo_academico_id);

CREATE TABLE academico.item_competencia (
    item_evaluacion_id UUID          NOT NULL,
    plan_curso_id      UUID          NOT NULL,
    competencia_id     UUID          NOT NULL,
    peso_porcentaje    NUMERIC(5, 2) NOT NULL,
    PRIMARY KEY (item_evaluacion_id, competencia_id),
    CONSTRAINT fk_item_competencia_item_plan
        FOREIGN KEY (item_evaluacion_id, plan_curso_id)
        REFERENCES academico.item_evaluacion (id, plan_curso_id) ON DELETE CASCADE,
    CONSTRAINT fk_item_competencia_plan
        FOREIGN KEY (plan_curso_id, competencia_id)
        REFERENCES academico.plan_competencia (plan_curso_id, competencia_id),
    CONSTRAINT ck_item_competencia_peso CHECK (peso_porcentaje > 0 AND peso_porcentaje <= 100)
);

CREATE INDEX ix_item_competencia_competencia
    ON academico.item_competencia (plan_curso_id, competencia_id);

CREATE TABLE academico.calificacion_item (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_evaluacion_id   UUID          NOT NULL,
    seccion_id           UUID          NOT NULL,
    matricula_id         UUID          NOT NULL,
    nota_numerica        NUMERIC(4, 2),
    nivel_logro          VARCHAR(2),
    registrado_por       UUID          NOT NULL REFERENCES seguridad.usuario (id),
    creado_en            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_calificacion_item_matricula UNIQUE (item_evaluacion_id, matricula_id),
    CONSTRAINT fk_calificacion_item_seccion
        FOREIGN KEY (item_evaluacion_id, seccion_id)
        REFERENCES academico.item_evaluacion (id, seccion_id) ON DELETE CASCADE,
    CONSTRAINT fk_calificacion_matricula_seccion
        FOREIGN KEY (matricula_id, seccion_id)
        REFERENCES matricula.matricula (id, seccion_id),
    CONSTRAINT ck_calificacion_item_un_valor CHECK (
        (nota_numerica IS NOT NULL AND nivel_logro IS NULL)
        OR (nota_numerica IS NULL AND nivel_logro IS NOT NULL)),
    CONSTRAINT ck_calificacion_item_nota CHECK (nota_numerica IS NULL OR nota_numerica BETWEEN 0 AND 20),
    CONSTRAINT ck_calificacion_item_nivel CHECK (nivel_logro IS NULL OR nivel_logro IN ('AD', 'A', 'B', 'C'))
);

CREATE INDEX ix_calificacion_item_matricula ON academico.calificacion_item (matricula_id);

CREATE TABLE academico.evidencia_calificacion (
    calificacion_id UUID PRIMARY KEY REFERENCES academico.calificacion_item (id) ON DELETE CASCADE,
    nombre_archivo  VARCHAR(200) NOT NULL,
    tipo_mime       VARCHAR(100) NOT NULL,
    tamano_bytes    INTEGER      NOT NULL,
    contenido       BYTEA        NOT NULL,
    subido_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_evidencia_calificacion_tipo CHECK (tipo_mime IN (
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'image/jpeg',
        'image/png')),
    CONSTRAINT ck_evidencia_calificacion_tamano CHECK (tamano_bytes BETWEEN 1 AND 10485760)
);
