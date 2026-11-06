-- Validación de la iteración 1 con el PO: el padre o la madre fallecidos se registran con sus datos
-- (antes solo se guardaba "no registra" con el motivo). Un fallecido no tiene cuenta de usuario, no puede ser
-- responsable de matrícula ni recoger al estudiante. Es un dato de la persona: lo comparten sus hijos (hermanos).
ALTER TABLE personas.persona
    ADD COLUMN fallecido BOOLEAN NOT NULL DEFAULT FALSE;
