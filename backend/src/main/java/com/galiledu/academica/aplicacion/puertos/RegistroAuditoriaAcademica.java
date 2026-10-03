package com.galiledu.academica.aplicacion.puertos;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** IAuditoria: el cambio de nota y su evento se guardan en la misma transacción (RF-36). */
public interface RegistroAuditoriaAcademica {
	void registrar(EventoAuditoria evento);

	record EventoAuditoria(UUID usuarioResponsableId, String entidad, UUID entidadId, String operacion,
		Map<String, String> referencias, Map<String, String> valorAnterior, Map<String, String> valorNuevo) {
		public EventoAuditoria {
			Objects.requireNonNull(usuarioResponsableId, "El responsable es obligatorio");
			Objects.requireNonNull(entidadId, "La entidad es obligatoria");
			if (entidad == null || entidad.isBlank() || operacion == null || operacion.isBlank()) {
				throw new IllegalArgumentException("Entidad y operación son obligatorias");
			}
			referencias = referencias == null ? Map.of() : Map.copyOf(referencias);
			valorAnterior = valorAnterior == null ? null : Map.copyOf(valorAnterior);
			valorNuevo = valorNuevo == null ? null : Map.copyOf(valorNuevo);
		}
	}
}
