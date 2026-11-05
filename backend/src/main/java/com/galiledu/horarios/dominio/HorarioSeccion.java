package com.galiledu.horarios.dominio;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Cabecera del horario semanal de una sección (tabla horarios.horario_seccion, V10). HU-45.
 * <p>
 * Como máximo hay una por sección y la crea el servicio la primera vez que se guarda el horario. La sección y la
 * estructura horaria son de Configuración: solo se guardan sus ids.
 */
@Entity
@Table(schema = "horarios", name = "horario_seccion")
public class HorarioSeccion {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "seccion_id", nullable = false, updatable = false)
	private UUID seccionId;
	@Column(name = "estructura_horaria_id", nullable = false)
	private UUID estructuraHorariaId;
	/**
	 * Bloqueo optimista. Hibernate solo la incrementa cuando cambia esta fila; si en el guardado cambian únicamente los
	 * bloques, el servicio (Fase 2) debe forzar el incremento.
	 */
	@Version
	@Column(nullable = false)
	private long version;
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	protected HorarioSeccion() {
	}

	public static HorarioSeccion crear(UUID seccionId, UUID estructuraHorariaId) {
		HorarioSeccion h = new HorarioSeccion();
		h.seccionId = seccionId;
		h.estructuraHorariaId = estructuraHorariaId;
		return h;
	}

	@PrePersist
	void alCrear() {
		creadoEn = Instant.now();
		actualizadoEn = creadoEn;
	}

	@PreUpdate
	void alActualizar() {
		actualizadoEn = Instant.now();
	}

	public UUID getId() {
		return id;
	}
	public UUID getSeccionId() {
		return seccionId;
	}
	public UUID getEstructuraHorariaId() {
		return estructuraHorariaId;
	}
	public long getVersion() {
		return version;
	}
	public Instant getCreadoEn() {
		return creadoEn;
	}
	public Instant getActualizadoEn() {
		return actualizadoEn;
	}
}
