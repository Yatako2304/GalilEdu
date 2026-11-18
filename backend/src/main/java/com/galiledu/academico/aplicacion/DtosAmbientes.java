package com.galiledu.academico.aplicacion;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.galiledu.academico.dominio.Ambiente;
import com.galiledu.academico.dominio.EstadoAmbiente;
import com.galiledu.academico.dominio.TipoAmbiente;

/**
 * Contratos JSON de los ambientes físicos (HU-43, Figma CI-26). Deben coincidir con frontend/src/types/api.ts.
 * <p>
 * El código no se ingresa: lo genera el sistema, y {@code AmbienteResumen} lo devuelve solo de lectura. En las
 * solicitudes solo se validan la presencia y el mínimo del aforo; el largo de los textos y los límites del aforo se
 * validan en el dominio ({@code Ambiente}), porque dependen del texto ya normalizado (sin espacios sobrantes) y del
 * tipo.
 */
public final class DtosAmbientes {
	private DtosAmbientes() {
	}

	/** Figma CI-26, tarjetas: "17 de 18 registrados", aulas regulares, espacios especiales y aforo total (activos). */
	public record ResumenAmbientes(long activos, long total, long aulasRegulares, long espaciosEspeciales,
			long aforoTotal) {
	}

	/** Fila de la lista. {@code recursos} va en el orden en que se ingresaron. */
	public record AmbienteResumen(UUID id, String codigo, String nombre, TipoAmbiente tipo, int aforoMaximo,
			List<String> recursos, EstadoAmbiente estado) {
		static AmbienteResumen de(Ambiente a) {
			return new AmbienteResumen(a.getId(), a.getCodigo(), a.getNombre(), a.getTipo(), a.getAforoMaximo(),
					a.getRecursos(), a.getEstado());
		}
	}

	/** Modal "Nuevo ambiente físico" (sin código: lo genera el sistema). */
	public record SolicitudAmbiente(
			@NotNull(message = "Elige el tipo de ambiente.") TipoAmbiente tipo,
			@NotBlank(message = "Ingresa el nombre del ambiente.") String nombre,
			@NotNull(message = "Ingresa el aforo máximo.") @Min(value = 1, message = "El aforo debe ser mayor que 0.") Integer aforoMaximo,
			List<String> recursos) {
	}

	/** Edición: nombre, aforo y recursos. El código y el tipo no se editan. */
	public record SolicitudEdicionAmbiente(
			@NotBlank(message = "Ingresa el nombre del ambiente.") String nombre,
			@NotNull(message = "Ingresa el aforo máximo.") @Min(value = 1, message = "El aforo debe ser mayor que 0.") Integer aforoMaximo,
			List<String> recursos) {
	}
}
