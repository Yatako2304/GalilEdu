-- =====================================================================
-- V17 · Horarios (HU-48): reserva de espacios físicos especiales
-- Un esquema por módulo: horarios (reserva_ambiente, junto a horario_seccion y horario_bloque).
-- Una reserva dice: "en esta fecha y de esta hora a esta hora, la sección usa este espacio especial para esta
-- actividad, a cargo de este responsable". Las horas son reales (no números de bloque): la reserva no depende de
-- la estructura horaria.
-- "Pendiente" y "Finalizado" no se guardan: se calculan al leer con la fecha y la hora de fin. "Eliminar" es
-- cancelar (estado CANCELADA): la fila se conserva.
-- Las reglas que la BD no puede garantizar (el ambiente es un espacio especial activo, la sección cabe en el
-- aforo, la fecha es un día lectivo dentro de un periodo y de la jornada, y los cruces con otras reservas, con
-- los horarios semanales y con las clases del responsable) las valida el servicio, dentro del bloqueo del año;
-- no se modelan con triggers.
-- =====================================================================

CREATE TABLE horarios.reserva_ambiente (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- RESTRICT: un ambiente con reservas no se elimina (en Configuración los ambientes solo se desactivan).
    ambiente_id    UUID         NOT NULL REFERENCES configuracion.ambiente (id) ON DELETE RESTRICT,
    -- La reserva nace y muere con la sección: al eliminarla se borran sus reservas.
    seccion_id     UUID         NOT NULL REFERENCES configuracion.seccion (id) ON DELETE CASCADE,
    -- Quien se hace cargo de la actividad (rol DOCENTE o TUTOR; lo valida el servicio).
    responsable_id UUID         NOT NULL REFERENCES seguridad.usuario (id),
    actividad      VARCHAR(120) NOT NULL,
    fecha          DATE         NOT NULL,
    hora_inicio    TIME         NOT NULL,
    hora_fin       TIME         NOT NULL,
    estado         VARCHAR(10)  NOT NULL DEFAULT 'ACTIVA',
    -- Coordinador que la registró.
    creado_por     UUID         NOT NULL REFERENCES seguridad.usuario (id),
    -- Bloqueo optimista: Hibernate la incrementa en cada cambio de la fila.
    version        BIGINT       NOT NULL DEFAULT 0,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_reserva_ambiente_horas CHECK (hora_fin > hora_inicio),
    CONSTRAINT ck_reserva_ambiente_estado CHECK (estado IN ('ACTIVA', 'CANCELADA'))
);

-- Cruces de un ambiente en una fecha, de una sección y de un responsable.
CREATE INDEX ix_reserva_ambiente_ambiente_fecha ON horarios.reserva_ambiente (ambiente_id, fecha);
CREATE INDEX ix_reserva_ambiente_seccion ON horarios.reserva_ambiente (seccion_id);
CREATE INDEX ix_reserva_ambiente_responsable ON horarios.reserva_ambiente (responsable_id);
