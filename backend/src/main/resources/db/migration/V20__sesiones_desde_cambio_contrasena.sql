-- HU-5 (y HU-4): al cambiar o restablecer la contraseña se cierran las sesiones abiertas en todos los dispositivos.
-- Un token emitido antes de este momento deja de valer (VerificadorSesionUsuarios compara su fecha de emisión).
ALTER TABLE seguridad.usuario ADD COLUMN sesiones_desde TIMESTAMPTZ;
