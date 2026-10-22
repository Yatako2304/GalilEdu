package com.galiledu.configuracion.aplicacion;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.galiledu.configuracion.dominio.EstadoSeccion;
import com.galiledu.configuracion.dominio.Seccion;

/** Contratos JSON de las secciones (HU-75). Deben coincidir con frontend/src/types/api.ts. */
public final class DtosSecciones {
	private DtosSecciones() {
	}

	/**
	 * Cuerpo de crear sección ("Modal - Nueva sección"): grado, nombre (se guarda en mayúsculas) y capacidad de 1 a
	 * 45. El año va en la ruta.
	 */
	public record SolicitudSeccion(
			@NotBlank(message = "Elige un grado.") String gradoCodigo,
			@NotBlank(message = "Ingresa el nombre de la sección.") String nombre,
			@NotNull(message = "Ingresa la capacidad máxima.") Integer capacidadMaxima) {
	}

	/** Cuerpo de cambiar capacidad: el valor nuevo, de 1 a 45. El nombre de la sección no se edita. */
	public record SolicitudCapacidad(@NotNull(message = "Ingresa la capacidad máxima.") Integer capacidadMaxima) {
	}

	/** Cuerpo de asignar tutores ("Modal - Asignar tutor"): tutor obligatorio; cotutor opcional (null lo quita). */
	public record SolicitudTutores(@NotNull(message = "Elige un tutor.") UUID tutorId, UUID cotutorId) {
	}

	/**
	 * Un grupo de "CI-09": el grado, cuántas secciones activas tiene y sus secciones (lista vacía si no tiene).
	 * {@code numero} sirve para armar la etiqueta "1.° A".
	 */
	public record GradoConSecciones(String codigo, String nombre, String nivel, int numero,
			int cantidadSeccionesActivas, List<SeccionResumen> secciones) {
	}

	/**
	 * Fila de "CI-09": capacidad, alumnos que ocupan vacante (RETIRADO y ANULADA no cuentan), tutor y cotutor con su
	 * id y nombre (null si no tiene) y estado.
	 */
	public record SeccionResumen(UUID id, String gradoCodigo, String nombre, int capacidadMaxima,
			int alumnosOcupanVacante, UUID tutorId, String tutor, UUID cotutorId, String cotutor,
			EstadoSeccion estado) {
		static SeccionResumen de(Seccion s, Map<UUID, String> nombresDeUsuarios) {
			return new SeccionResumen(s.getId(), s.getGradoCodigo(), s.getNombre(), s.getCapacidadMaxima(),
					s.getAlumnosOcupanVacante(), s.getTutorId(), nombreDe(nombresDeUsuarios, s.getTutorId()),
					s.getCotutorId(), nombreDe(nombresDeUsuarios, s.getCotutorId()), s.getEstado());
		}

		private static String nombreDe(Map<UUID, String> nombres, UUID usuarioId) {
			return usuarioId == null ? null : nombres.get(usuarioId);
		}
	}

	/**
	 * Opción de los selectores del "Modal - Asignar tutor": un docente o tutor activo. {@code esTutorEsteAnio} dice si
	 * ya es tutor de una sección activa del año.
	 */
	public record DocenteElegible(UUID id, String nombre, boolean esTutorEsteAnio) {
	}

	/**
	 * Sección activa para que Matrícula reserve vacantes (HU-23); las libres son capacidadMaxima menos
	 * alumnosOcupanVacante. Sin endpoint.
	 */
	public record SeccionConVacantes(UUID id, String gradoCodigo, String gradoNombre, String nombre, String tutor,
			int capacidadMaxima, int alumnosOcupanVacante) {
	}
}
