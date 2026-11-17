package com.galiledu.compartido.seguridad;

import java.util.Set;
import java.util.UUID;

/**
 * Decide si el usuario dueño de un token todavía puede usar el sistema. Se consulta en cada petición, así un usuario
 * desactivado o eliminado pierde el acceso de inmediato aunque su token no haya vencido (HU-12).
 * También entrega los roles que la cuenta tiene ahora, para que un cambio de rol (HU-10) valga desde la siguiente
 * petición sin esperar a que venza el token. La implementación vive en el módulo de usuarios.
 */
public interface VerificadorSesion {
	boolean sesionVigente(UUID usuarioId);

	/** Códigos de los roles actuales de la cuenta; vacío si la sesión ya no es vigente. */
	Set<String> rolesVigentes(UUID usuarioId);
}
