package com.galiledu.estudiantes.dominio;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Documento que acredita al apoderado, p. ej. la carta poder (tabla estudiantes.documento_vinculo, V3). */
@Entity
@Table(schema = "estudiantes", name = "documento_vinculo")
public class DocumentoVinculo {
	public static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
	public static final Set<String> TIPOS_PERMITIDOS = Set.of("application/pdf", "image/jpeg", "image/png");

	@Id
	@Column(name = "vinculo_id")
	private UUID vinculoId;
	@Column(nullable = false)
	private String tipo;
	@Column(name = "nombre_archivo", nullable = false)
	private String nombreArchivo;
	@Column(name = "tipo_mime", nullable = false)
	private String tipoMime;
	@Column(name = "tamano_bytes", nullable = false)
	private int tamanoBytes;
	@Column(nullable = false)
	private byte[] contenido;
	@Column(name = "subido_en", nullable = false)
	private Instant subidoEn;

	protected DocumentoVinculo() {
	}

	public DocumentoVinculo(UUID vinculoId) {
		this.vinculoId = vinculoId;
	}

	public void reemplazar(String tipo, String nombreArchivo, String tipoMime, byte[] contenido) {
		this.tipo = tipo;
		this.nombreArchivo = nombreArchivo;
		this.tipoMime = tipoMime;
		this.contenido = contenido;
		this.tamanoBytes = contenido.length;
		this.subidoEn = Instant.now();
	}

	public UUID getVinculoId() {
		return vinculoId;
	}

	public String getTipo() {
		return tipo;
	}

	public String getNombreArchivo() {
		return nombreArchivo;
	}

	public String getTipoMime() {
		return tipoMime;
	}

	public byte[] getContenido() {
		return contenido;
	}

	public Instant getSubidoEn() {
		return subidoEn;
	}
}
