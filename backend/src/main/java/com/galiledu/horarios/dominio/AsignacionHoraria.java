package com.galiledu.horarios.dominio;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Curso de una sección en un bloque y día, con los periodos en que rige. Identifica el bloque
 * por su id, porque el número de orden se repite entre estructuras horarias.
 */
public final class AsignacionHoraria {
	private static final int LONGITUD_MAXIMA_TIPO_SESION = 40;

	private final UUID id;
	private final UUID horarioSeccionId;
	private final UUID cargaAcademicaId;
	private final UUID bloqueHorarioId;
	private final UUID espacioFisicoId;
	private final UUID subgrupoId;
	private final String tipoSesion;
	private final DiaSemana dia;
	private final Set<UUID> periodos;
	private final boolean activa;

	public AsignacionHoraria(UUID id, UUID horarioSeccionId, UUID cargaAcademicaId,
		UUID bloqueHorarioId, UUID espacioFisicoId, UUID subgrupoId, String tipoSesion,
		DiaSemana dia, Set<UUID> periodos, boolean activa) {
		this.id = Objects.requireNonNull(id, "El identificador es obligatorio");
		this.horarioSeccionId = Objects.requireNonNull(horarioSeccionId, "El horario es obligatorio");
		this.cargaAcademicaId = Objects.requireNonNull(cargaAcademicaId, "La carga académica es obligatoria");
		this.bloqueHorarioId = Objects.requireNonNull(bloqueHorarioId, "El bloque es obligatorio");
		this.espacioFisicoId = espacioFisicoId;
		this.subgrupoId = subgrupoId;
		this.tipoSesion = normalizarTipoSesion(tipoSesion);
		this.dia = Objects.requireNonNull(dia, "El día es obligatorio");
		if (periodos == null || periodos.isEmpty()) {
			throw new IllegalArgumentException("La asignación debe regir en al menos un periodo");
		}
		this.periodos = Set.copyOf(periodos);
		this.activa = activa;
	}

	public static AsignacionHoraria nueva(UUID horarioSeccionId, UUID cargaAcademicaId,
		UUID bloqueHorarioId, UUID espacioFisicoId, UUID subgrupoId, String tipoSesion,
		DiaSemana dia, Set<UUID> periodos) {
		return new AsignacionHoraria(UUID.randomUUID(), horarioSeccionId, cargaAcademicaId,
			bloqueHorarioId, espacioFisicoId, subgrupoId, tipoSesion, dia, periodos, true);
	}

	public UUID id() {
		return id;
	}

	public UUID horarioSeccionId() {
		return horarioSeccionId;
	}

	public UUID cargaAcademicaId() {
		return cargaAcademicaId;
	}

	public UUID bloqueHorarioId() {
		return bloqueHorarioId;
	}

	/** Puede ser nulo: la base de datos no exige un espacio físico en toda sesión. */
	public UUID espacioFisicoId() {
		return espacioFisicoId;
	}

	/** Nulo cuando la sesión es para toda la sección. */
	public UUID subgrupoId() {
		return subgrupoId;
	}

	public String tipoSesion() {
		return tipoSesion;
	}

	public DiaSemana dia() {
		return dia;
	}

	public Set<UUID> periodos() {
		return periodos;
	}

	public boolean activa() {
		return activa;
	}

	public AsignacionHoraria desactivar() {
		return new AsignacionHoraria(id, horarioSeccionId, cargaAcademicaId, bloqueHorarioId,
			espacioFisicoId, subgrupoId, tipoSesion, dia, periodos, false);
	}

	private static String normalizarTipoSesion(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		String normalizado = valor.strip();
		if (normalizado.length() > LONGITUD_MAXIMA_TIPO_SESION) {
			throw new IllegalArgumentException(
				"El tipo de sesión admite hasta " + LONGITUD_MAXIMA_TIPO_SESION + " caracteres");
		}
		return normalizado;
	}
}
