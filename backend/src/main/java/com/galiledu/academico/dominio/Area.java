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

/**
 * Área curricular (tabla academico.area, V6). HU-52. No se borra: se desactiva y ya no puede agregarse a planes
 * nuevos.
 */
@Entity
@Table(schema = "academico", name = "area")
public class Area {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
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

	protected Area() {
	}

	public Area(String nombre, String descripcion, short orden) {
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
