package com.galiledu.compartido.seguridad;

import java.util.UUID;

/**
 * Decide si el usuario dueño de un token todavía puede usar el sistema. Se consulta en cada petición, así un usuario
 * desactivado o eliminado pierde el acceso de inmediato aunque su token no haya vencido (HU-12).
 * La implementación vive en el módulo de usuarios.
 */
public interface VerificadorSesion {
	boolean sesionVigente(UUID usuarioId);
}
