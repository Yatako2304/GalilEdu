package com.galiledu.academico.dominio;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Competencia de un área (tabla academico.competencia, V6). HU-53. El nombre no se repite dentro del área. */
@Entity
@Table(schema = "academico", name = "competencia")
public class Competencia {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "area_id", nullable = false, updatable = false)
	private UUID areaId;
	@Column(nullable = false)
	private String nombre;
	private String descripcion;
	@Column(nullable = false)
	private short orden;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoArea estado = EstadoArea.ACTIVA;
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	protected Competencia() {
	}

	public Competencia(UUID areaId, String nombre, String descripcion, short orden) {
		this.areaId = areaId;
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.orden = orden;
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

	public void editar(String nombre, String descripcion) {
		this.nombre = nombre;
		this.descripcion = descripcion;
	}

	public boolean estaActiva() {
		return estado == EstadoArea.ACTIVA;
	}

	public void desactivar() {
		estado = EstadoArea.INACTIVA;
	}

	public void reactivar() {
		estado = EstadoArea.ACTIVA;
	}

	public UUID getId() {
		return id;
	}
	public UUID getAreaId() {
		return areaId;
	}
	public String getNombre() {
		return nombre;
	}
	public String getDescripcion() {
		return descripcion;
	}
	public short getOrden() {
		return orden;
	}
	public EstadoArea getEstado() {
		return estado;
	}
}
