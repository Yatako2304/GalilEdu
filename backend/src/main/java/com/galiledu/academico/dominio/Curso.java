package com.galiledu.academico.dominio;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Curso de un área y los grados en los que se dicta (tablas academico.curso y academico.curso_grado, V6). HU-54,
 * RF-128.
 */
@Entity
@Table(schema = "academico", name = "curso")
public class Curso {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "area_id", nullable = false, updatable = false)
	private UUID areaId;
	@Column(nullable = false, unique = true)
	private String nombre;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoCurso estado = EstadoCurso.ACTIVO;
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	/** Códigos del catálogo configuracion.grado (PRI-1…PRI-6, SEC-1…SEC-5). */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(schema = "academico", name = "curso_grado", joinColumns = @JoinColumn(name = "curso_id"))
	@Column(name = "grado_codigo")
	private Set<String> grados = new LinkedHashSet<>();

	protected Curso() {
	}

	public Curso(UUID areaId, String nombre, Collection<String> grados) {
		this.areaId = areaId;
		this.nombre = nombre;
		this.grados = new LinkedHashSet<>(grados);
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

	public void editar(String nombre, Collection<String> grados) {
		this.nombre = nombre;
		this.grados.retainAll(grados);
		this.grados.addAll(grados);
	}

	public boolean estaActivo() {
		return estado == EstadoCurso.ACTIVO;
	}

	public void desactivar() {
		estado = EstadoCurso.INACTIVO;
	}

	public void reactivar() {
		estado = EstadoCurso.ACTIVO;
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
	public EstadoCurso getEstado() {
		return estado;
	}
	public Set<String> getGrados() {
		return grados;
	}
}
