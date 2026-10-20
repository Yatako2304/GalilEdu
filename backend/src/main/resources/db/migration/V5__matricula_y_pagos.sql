-- =====================================================================
-- V5 · Matrícula y pagos (HU-21 a HU-33, HU-71, HU-72 · HU-35 a HU-42)
-- Se apoya en V4 (año escolar, secciones, periodo de matrícula y tarifario).
-- Figma > Personal Administrativo:
--   "A3 · Matrículas del periodo", "A5 · Detalle — Sección reservada", "A6 · Completar matrícula",
--   "B6 · Pendiente de pago", "B7 · Matriculado", modales "Cambiar sección" y "¿Aprobar documentación?"
--   "P1–P3 · Estado de cuenta", "P8 · Cronograma", "P9 · Pagos (Pago, PagoDetalle)",
--   "P10 · Descuento aplicado (DescuentoAplicado)", "P11 · Morosidad", "P21 · Catálogo de descuentos",
--   "P2b · Cuota adicional masiva", modales "Agregar cuota especial" y "Registrar retiro del alumno"
-- Figma > Apoderado: HU-28 a HU-32 (matrícula), HU-40 a HU-42 (cronograma, pago en línea, constancias).
--
-- Flujo: sección reservada → documentos en validación → pendiente de pago (se genera la cuota
-- de matrícula) → matriculado (código MAT-…, se genera el cronograma de pensiones).
-- Lo que se calcula al consultar y NO se guarda: cuota vencida (vencimiento + días de gracia),
-- días de atraso, recargo por mora pendiente, deuda del apoderado y vacantes libres por sección.
-- =====================================================================

-- =====================================================================
-- MATRÍCULA
-- =====================================================================

-- Documentos que se piden en el periodo de matrícula (HU-21/22: nombre, descripción y plantilla opcional).
CREATE TABLE matricula.documento_requerido (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    periodo_matricula_id     UUID         NOT NULL REFERENCES matricula.periodo_matricula (id) ON DELETE CASCADE,
    nombre                   VARCHAR(100) NOT NULL,
    descripcion              VARCHAR(2000),
    plantilla_nombre_archivo VARCHAR(200),
    plantilla_tipo_mime      VARCHAR(100),
    plantilla_contenido      BYTEA,
    orden                    SMALLINT     NOT NULL DEFAULT 1,
    CONSTRAINT uq_documento_requerido_nombre UNIQUE (periodo_matricula_id, nombre),
    CONSTRAINT ck_documento_requerido_plantilla CHECK (
        (plantilla_contenido IS NULL) = (plantilla_nombre_archivo IS NULL))
);

-- Matrícula de un estudiante en un año escolar (una por año).
CREATE TABLE matricula.matricula (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    estudiante_id        UUID        NOT NULL REFERENCES estudiantes.estudiante (id),
    anio_escolar_id      UUID        NOT NULL REFERENCES configuracion.anio_escolar (id),
    grado_codigo         VARCHAR(10) NOT NULL REFERENCES configuracion.grado (codigo),
    seccion_id           UUID REFERENCES configuracion.seccion (id),
    tipo_estudiante      VARCHAR(12) NOT NULL,
    estado               VARCHAR(20) NOT NULL DEFAULT 'SECCION_RESERVADA',
    -- HU-31 / RF-66: se genera al confirmarse el pago de la cuota de matrícula (MAT-2027-0044).
    codigo               VARCHAR(30) UNIQUE,
    -- HU-25 / RF-54, RF-55: matrícula fuera de fecha, con constancia del motivo.
    extemporanea         BOOLEAN     NOT NULL DEFAULT FALSE,
    motivo_extemporanea  VARCHAR(500),
    -- HU-32 / RF-68: fecha y hora exacta de la confirmación.
    confirmado_en        TIMESTAMPTZ,
    -- Retiro del alumno (anula las cuotas futuras, RF-85).
    fecha_retiro         DATE,
    motivo_retiro        VARCHAR(500),
    creado_por           UUID REFERENCES seguridad.usuario (id),
    creado_en            TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_matricula_estudiante_anio UNIQUE (estudiante_id, anio_escolar_id),
    CONSTRAINT ck_matricula_tipo CHECK (tipo_estudiante IN ('NUEVO', 'CONTINUADOR')),
    CONSTRAINT ck_matricula_estado CHECK (estado IN (
        'SECCION_RESERVADA', 'EN_VALIDACION', 'PENDIENTE_PAGO', 'MATRICULADO', 'RETIRADO', 'ANULADA')),
    CONSTRAINT ck_matricula_codigo CHECK (estado NOT IN ('MATRICULADO', 'RETIRADO') OR codigo IS NOT NULL),
    CONSTRAINT ck_matricula_extemporanea CHECK (NOT extemporanea OR motivo_extemporanea IS NOT NULL),
    CONSTRAINT ck_matricula_retiro CHECK (estado <> 'RETIRADO' OR fecha_retiro IS NOT NULL)
);

CREATE INDEX ix_matricula_seccion ON matricula.matricula (seccion_id);
CREATE INDEX ix_matricula_anio_estado ON matricula.matricula (anio_escolar_id, estado);

-- Correlativo del código de matrícula por año (igual que seguridad.correlativo_usuario).
CREATE TABLE matricula.correlativo_matricula (
    anio   SMALLINT PRIMARY KEY,
    ultimo INTEGER NOT NULL DEFAULT 0
);

-- HU-29 / HU-72: documentos firmados de la matrícula (PDF, JPG o PNG de hasta 10 MB, RF-62).
CREATE TABLE matricula.documento_matricula (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    matricula_id           UUID         NOT NULL REFERENCES matricula.matricula (id) ON DELETE CASCADE,
    documento_requerido_id UUID         NOT NULL REFERENCES matricula.documento_requerido (id),
    nombre_archivo         VARCHAR(200) NOT NULL,
    tipo_mime              VARCHAR(100) NOT NULL,
    tamano_bytes           INTEGER      NOT NULL,
    contenido              BYTEA        NOT NULL,
    estado                 VARCHAR(12)  NOT NULL DEFAULT 'EN_REVISION',
    observacion            VARCHAR(500),
    subido_por             UUID REFERENCES seguridad.usuario (id),
    subido_en              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    revisado_por           UUID REFERENCES seguridad.usuario (id),
    revisado_en            TIMESTAMPTZ,
    CONSTRAINT uq_documento_matricula UNIQUE (matricula_id, documento_requerido_id),
    CONSTRAINT ck_documento_matricula_mime CHECK (tipo_mime IN ('application/pdf', 'image/jpeg', 'image/png')),
    CONSTRAINT ck_documento_matricula_tamano CHECK (tamano_bytes > 0 AND tamano_bytes <= 10485760),
    CONSTRAINT ck_documento_matricula_estado CHECK (estado IN ('EN_REVISION', 'APROBADO', 'OBSERVADO')),
    CONSTRAINT ck_documento_matricula_observacion CHECK (estado <> 'OBSERVADO' OR observacion IS NOT NULL)
);

-- HU-26: historial de cambios de sección (las vacantes se calculan contando matrículas por sección).
CREATE TABLE matricula.cambio_seccion (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    matricula_id       UUID        NOT NULL REFERENCES matricula.matricula (id) ON DELETE CASCADE,
    seccion_origen_id  UUID        NOT NULL REFERENCES configuracion.seccion (id),
    seccion_destino_id UUID        NOT NULL REFERENCES configuracion.seccion (id),
    motivo             VARCHAR(500),
    realizado_por      UUID REFERENCES seguridad.usuario (id),
    realizado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_cambio_seccion_distinta CHECK (seccion_origen_id <> seccion_destino_id)
);

-- =====================================================================
-- PAGOS
-- =====================================================================

-- Conceptos de cobro (catálogo fijo). Solo la pensión genera recargo por mora (RF-91).
CREATE TABLE pagos.concepto (
    codigo      VARCHAR(20) PRIMARY KEY,
    nombre      VARCHAR(60) NOT NULL,
    genera_mora BOOLEAN     NOT NULL,
    orden       SMALLINT    NOT NULL
);

INSERT INTO pagos.concepto (codigo, nombre, genera_mora, orden) VALUES
    ('CUOTA_INGRESO',  'Cuota de ingreso',      FALSE, 1),
    ('MATRICULA',      'Matrícula',             FALSE, 2),
    ('PENSION',        'Pensión',               TRUE,  3),
    ('SERVICIO',       'Servicio mensual',      FALSE, 4),
    ('EXTRAORDINARIA', 'Cuota extraordinaria',  FALSE, 5);

-- RF-77: porcentaje máximo de descuento que admite la institución (una sola fila).
CREATE TABLE pagos.configuracion (
    id                          SMALLINT PRIMARY KEY DEFAULT 1,
    porcentaje_maximo_descuento NUMERIC(5, 2) NOT NULL,
    actualizado_en              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_pagos_configuracion_unica CHECK (id = 1),
    CONSTRAINT ck_pagos_configuracion_maximo CHECK (porcentaje_maximo_descuento BETWEEN 1 AND 100)
);

INSERT INTO pagos.configuracion (id, porcentaje_maximo_descuento) VALUES (1, 50);

-- HU-35: catálogo de descuentos. PORCENTAJE_UNICO usa "porcentaje"; POR_HERMANOS usa sus tramos.
CREATE TABLE pagos.descuento (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre         VARCHAR(80)  NOT NULL UNIQUE,
    descripcion    VARCHAR(300),
    tipo           VARCHAR(20)  NOT NULL,
    aplicacion     VARCHAR(12)  NOT NULL,
    porcentaje     NUMERIC(5, 2),
    condicion      VARCHAR(300),
    estado         VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_descuento_tipo CHECK (tipo IN ('PORCENTAJE_UNICO', 'POR_HERMANOS')),
    CONSTRAINT ck_descuento_aplicacion CHECK (aplicacion IN ('MANUAL', 'AUTOMATICO')),
    CONSTRAINT ck_descuento_porcentaje CHECK (
        (tipo = 'PORCENTAJE_UNICO' AND porcentaje BETWEEN 1 AND 100)
        OR (tipo = 'POR_HERMANOS' AND porcentaje IS NULL)),
    CONSTRAINT ck_descuento_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

-- RF-75: tramos del descuento por hermanos ("2 hermanos: 10 % · 3+: 15 %"); hasta NULL = "o más".
CREATE TABLE pagos.descuento_tramo (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    descuento_id      UUID          NOT NULL REFERENCES pagos.descuento (id) ON DELETE CASCADE,
    hermanos_desde    SMALLINT      NOT NULL,
    hermanos_hasta    SMALLINT,
    porcentaje        NUMERIC(5, 2) NOT NULL,
    CONSTRAINT uq_descuento_tramo_desde UNIQUE (descuento_id, hermanos_desde),
    CONSTRAINT ck_descuento_tramo_rango CHECK (
        hermanos_desde >= 2 AND (hermanos_hasta IS NULL OR hermanos_hasta >= hermanos_desde)),
    CONSTRAINT ck_descuento_tramo_porcentaje CHECK (porcentaje BETWEEN 1 AND 100)
);

-- RF-74: conceptos sobre los que rige cada descuento (al menos uno, lo valida el servicio).
CREATE TABLE pagos.descuento_concepto (
    descuento_id    UUID        NOT NULL REFERENCES pagos.descuento (id) ON DELETE CASCADE,
    concepto_codigo VARCHAR(20) NOT NULL REFERENCES pagos.concepto (codigo),
    PRIMARY KEY (descuento_id, concepto_codigo)
);

-- HU-37: compromiso de pago de un estudiante en un año escolar (uno por año, RF-83).
-- Se crea al generar la cuota de matrícula (HU-36) y se completa con las pensiones al matricularse.
CREATE TABLE pagos.compromiso_pago (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    estudiante_id    UUID        NOT NULL REFERENCES estudiantes.estudiante (id),
    anio_escolar_id  UUID        NOT NULL REFERENCES configuracion.anio_escolar (id),
    matricula_id     UUID UNIQUE REFERENCES matricula.matricula (id),
    tarifario_id     UUID        NOT NULL REFERENCES pagos.tarifario (id),
    estado           VARCHAR(10) NOT NULL DEFAULT 'VIGENTE',
    motivo_anulacion VARCHAR(500),
    anulado_por      UUID REFERENCES seguridad.usuario (id),
    anulado_en       TIMESTAMPTZ,
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_compromiso_estudiante_anio UNIQUE (estudiante_id, anio_escolar_id),
    CONSTRAINT ck_compromiso_estado CHECK (estado IN ('VIGENTE', 'ANULADO')),
    CONSTRAINT ck_compromiso_anulacion CHECK (estado <> 'ANULADO' OR motivo_anulacion IS NOT NULL)
);

-- "Cuota adicional masiva" (P2b): la misma cuota extraordinaria para varios estudiantes.
CREATE TABLE pagos.lote_cuota (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anio_escolar_id   UUID          NOT NULL REFERENCES configuracion.anio_escolar (id),
    detalle           VARCHAR(100)  NOT NULL,
    monto             NUMERIC(10, 2) NOT NULL,
    fecha_vencimiento DATE          NOT NULL,
    motivo            VARCHAR(500)  NOT NULL,
    creado_por        UUID REFERENCES seguridad.usuario (id),
    creado_en         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_lote_cuota_monto CHECK (monto > 0)
);

-- Cuotas del compromiso: matrícula (HU-36), pensiones (HU-37), especiales y masivas.
-- monto_final = monto_base - monto_descuento. "Vencida" no es un estado: se calcula (RF-91).
CREATE TABLE pagos.cuota (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    compromiso_id     UUID           NOT NULL REFERENCES pagos.compromiso_pago (id) ON DELETE CASCADE,
    concepto_codigo   VARCHAR(20)    NOT NULL REFERENCES pagos.concepto (codigo),
    numero            SMALLINT,
    nombre            VARCHAR(80)    NOT NULL,
    monto_base        NUMERIC(10, 2) NOT NULL,
    monto_descuento   NUMERIC(10, 2) NOT NULL DEFAULT 0,
    monto_final       NUMERIC(10, 2) GENERATED ALWAYS AS (monto_base - monto_descuento) STORED,
    monto_pagado      NUMERIC(10, 2) NOT NULL DEFAULT 0,
    fecha_vencimiento DATE           NOT NULL,
    estado            VARCHAR(16)    NOT NULL DEFAULT 'PENDIENTE',
    fecha_pago        DATE,
    lote_id           UUID REFERENCES pagos.lote_cuota (id),
    motivo            VARCHAR(500),
    creado_por        UUID REFERENCES seguridad.usuario (id),
    creado_en         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_cuota_montos CHECK (
        monto_base >= 0 AND monto_descuento >= 0 AND monto_descuento <= monto_base AND monto_pagado >= 0),
    CONSTRAINT ck_cuota_estado CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'PAGADA_PARCIAL', 'PAGADA', 'ANULADA')),
    CONSTRAINT ck_cuota_pagada CHECK (estado <> 'PAGADA' OR fecha_pago IS NOT NULL)
);

CREATE INDEX ix_cuota_compromiso ON pagos.cuota (compromiso_id, fecha_vencimiento);
-- RF-79: una sola cuota de matrícula por compromiso (es decir, por estudiante y año).
CREATE UNIQUE INDEX ux_cuota_una_matricula ON pagos.cuota (compromiso_id)
    WHERE concepto_codigo = 'MATRICULA' AND estado <> 'ANULADA';
-- RF-83: no se duplica una pensión del cronograma si se reintenta la generación.
CREATE UNIQUE INDEX ux_cuota_pension_numero ON pagos.cuota (compromiso_id, numero)
    WHERE concepto_codigo = 'PENSION' AND estado <> 'ANULADA';

-- HU-38: descuento aplicado a un compromiso. Guarda una copia del nombre y del porcentaje (RF-89).
CREATE TABLE pagos.descuento_aplicado (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    compromiso_id     UUID          NOT NULL REFERENCES pagos.compromiso_pago (id) ON DELETE CASCADE,
    descuento_id      UUID          NOT NULL REFERENCES pagos.descuento (id),
    nombre            VARCHAR(80)   NOT NULL,
    porcentaje        NUMERIC(5, 2) NOT NULL,
    vigencia_desde    DATE          NOT NULL,
    vigencia_hasta    DATE,
    motivo            VARCHAR(500)  NOT NULL,
    estado            VARCHAR(12)   NOT NULL DEFAULT 'ACTIVO',
    aplicado_por      UUID REFERENCES seguridad.usuario (id),
    aplicado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    motivo_revocacion VARCHAR(500),
    revocado_por      UUID REFERENCES seguridad.usuario (id),
    revocado_en       TIMESTAMPTZ,
    CONSTRAINT ck_descuento_aplicado_porcentaje CHECK (porcentaje BETWEEN 1 AND 100),
    CONSTRAINT ck_descuento_aplicado_vigencia CHECK (vigencia_hasta IS NULL OR vigencia_hasta >= vigencia_desde),
    CONSTRAINT ck_descuento_aplicado_estado CHECK (estado IN ('ACTIVO', 'REVOCADO', 'REEMPLAZADO')),
    CONSTRAINT ck_descuento_aplicado_revocacion CHECK (estado <> 'REVOCADO' OR motivo_revocacion IS NOT NULL)
);

-- RF-87: un solo descuento activo por estudiante y año.
CREATE UNIQUE INDEX ux_descuento_aplicado_activo ON pagos.descuento_aplicado (compromiso_id) WHERE estado = 'ACTIVO';

-- HU-41: orden de pago en línea (una o varias cuotas de los hijos del apoderado).
CREATE TABLE pagos.orden_pago (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo               VARCHAR(30)    NOT NULL UNIQUE,
    usuario_id           UUID           NOT NULL REFERENCES seguridad.usuario (id),
    monto_total          NUMERIC(10, 2) NOT NULL,
    estado               VARCHAR(12)    NOT NULL DEFAULT 'EN_PROCESO',
    -- RF-103: la referencia de la pasarela es única; una notificación repetida no duplica el pago.
    referencia_pasarela  VARCHAR(60) UNIQUE,
    monto_confirmado     NUMERIC(10, 2),
    motivo_rechazo       VARCHAR(300),
    creado_en            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    resuelto_en          TIMESTAMPTZ,
    CONSTRAINT ck_orden_pago_monto CHECK (monto_total > 0),
    CONSTRAINT ck_orden_pago_estado CHECK (estado IN ('EN_PROCESO', 'APROBADA', 'RECHAZADA', 'EXPIRADA'))
);

CREATE TABLE pagos.orden_pago_cuota (
    orden_pago_id UUID           NOT NULL REFERENCES pagos.orden_pago (id) ON DELETE CASCADE,
    cuota_id      UUID           NOT NULL REFERENCES pagos.cuota (id),
    capital       NUMERIC(10, 2) NOT NULL,
    recargo       NUMERIC(10, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (orden_pago_id, cuota_id),
    CONSTRAINT ck_orden_pago_cuota_montos CHECK (capital > 0 AND recargo >= 0)
);

-- HU-41 / HU-42: pago confirmado (Pago, PagoDetalle en Figma) con su constancia correlativa (C-2026-01230).
-- Si el monto confirmado no coincide con la orden, queda OBSERVADO para revisión (RF-105).
CREATE TABLE pagos.pago (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    orden_pago_id      UUID UNIQUE REFERENCES pagos.orden_pago (id),
    estudiante_id      UUID           NOT NULL REFERENCES estudiantes.estudiante (id),
    numero_constancia  VARCHAR(20)    NOT NULL UNIQUE,
    fecha_pago         TIMESTAMPTZ    NOT NULL,
    referencia         VARCHAR(60),
    monto_total        NUMERIC(10, 2) NOT NULL,
    estado             VARCHAR(12)    NOT NULL DEFAULT 'CONFIRMADO',
    observacion        VARCHAR(500),
    registrado_por     UUID REFERENCES seguridad.usuario (id),
    creado_en          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_pago_monto CHECK (monto_total > 0),
    CONSTRAINT ck_pago_estado CHECK (estado IN ('CONFIRMADO', 'OBSERVADO')),
    CONSTRAINT ck_pago_observacion CHECK (estado <> 'OBSERVADO' OR observacion IS NOT NULL)
);

CREATE INDEX ix_pago_estudiante ON pagos.pago (estudiante_id, fecha_pago DESC);

CREATE TABLE pagos.pago_detalle (
    pago_id  UUID           NOT NULL REFERENCES pagos.pago (id) ON DELETE CASCADE,
    cuota_id UUID           NOT NULL REFERENCES pagos.cuota (id),
    capital  NUMERIC(10, 2) NOT NULL,
    recargo  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (pago_id, cuota_id),
    CONSTRAINT ck_pago_detalle_montos CHECK (capital > 0 AND recargo >= 0)
);

-- Correlativo de constancias por año (RF-107).
CREATE TABLE pagos.correlativo_constancia (
    anio   SMALLINT PRIMARY KEY,
    ultimo INTEGER NOT NULL DEFAULT 0
);
