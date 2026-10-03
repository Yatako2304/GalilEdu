package com.galiledu.horarios.dominio;

import java.util.Objects;
import java.util.UUID;

/** Versión del horario de una sección; no es un DTO ni una entidad JPA. */
public final class HorarioSeccion {
	private final UUID id;
	private final UUID seccionId;
	private final int version;
	private final EstadoHorario estado;
	private final boolean activo;

	public HorarioSeccion(UUID id, UUID seccionId, int version, EstadoHorario estado, boolean activo) {
		this.id = Objects.requireNonNull(id, "El identificador es obligatorio");
		this.seccionId = Objects.requireNonNull(seccionId, "La sección es obligatoria");
		if (version < 1) {
			throw new IllegalArgumentException("La versión debe ser positiva");
		}
		this.version = version;
		this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
		this.activo = activo;
	}

	public static HorarioSeccion nuevoBorrador(UUID seccionId, int version) {
		return new HorarioSeccion(UUID.randomUUID(), seccionId, version, EstadoHorario.BORRADOR, true);
	}

	public UUID id() {
		return id;
	}

	public UUID seccionId() {
		return seccionId;
	}

	public int version() {
		return version;
	}

	public EstadoHorario estado() {
		return estado;
	}

	public boolean activo() {
		return activo;
	}

	public HorarioSeccion desactivar() {
		return new HorarioSeccion(id, seccionId, version, estado, false);
	}
}
