package com.galiledu.asistencia.dominio;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Justificación de una falta o tardanza (asistencia.justificacion, V26). Una por asistencia. El adjunto es opcional;
 * su contenido se carga solo al descargarlo.
 */
@Entity
@Table(schema = "asistencia", name = "justificacion")
public class Justificacion {
	public static final int MAXIMO_DETALLE = 500;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "asistencia_estudiante_id", nullable = false, unique = true)
	private UUID asistenciaEstudianteId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MotivoJustificacion motivo;

	private String detalle;

	@Column(name = "archivo_nombre")
	private String archivoNombre;

	@Column(name = "archivo_tipo")
	private String archivoTipo;

	@Column(name = "archivo_tamano")
	private Integer archivoTamano;

	@Basic(fetch = FetchType.LAZY)
	@Column(name = "archivo_contenido")
	private byte[] archivoContenido;

	@Column(name = "registrado_por", nullable = false)
	private UUID registradoPor;

	@Column(name = "registrado_en", nullable = false)
	private OffsetDateTime registradoEn;

	protected Justificacion() {
	}

	/**
	 * RF-145: solo una falta o una tardanza se justifica, con motivo; "Otro" necesita el detalle. Los mensajes se
	 * muestran tal cual.
	 */
	public static Justificacion de(AsistenciaEstudiante asistencia, MotivoJustificacion motivo, String detalle,
			UUID usuarioId, OffsetDateTime cuando) {
		if (asistencia.getEstado() == EstadoAsistencia.PRESENTE) {
			throw new IllegalStateException("Solo se justifica una falta o una tardanza.");
		}
		if (motivo == null) {
			throw new IllegalArgumentException("Elige el motivo de la justificación.");
		}
		String limpio = detalle == null || detalle.isBlank() ? null : detalle.strip();
		if (motivo == MotivoJustificacion.OTRO && limpio == null) {
			throw new IllegalArgumentException("Describe el motivo en el detalle.");
		}
		if (limpio != null && limpio.length() > MAXIMO_DETALLE) {
			throw new IllegalArgumentException("El detalle no puede pasar de 500 caracteres.");
		}
		Justificacion j = new Justificacion();
		j.asistenciaEstudianteId = asistencia.getId();
		j.motivo = motivo;
		j.detalle = limpio;
		j.registradoPor = usuarioId;
		j.registradoEn = cuando;
		return j;
	}

	public void adjuntar(String nombre, String tipo, byte[] contenido) {
		this.archivoNombre = nombre;
		this.archivoTipo = tipo;
		this.archivoTamano = contenido.length;
		this.archivoContenido = contenido;
	}

	public UUID getId() {
		return id;
	}

	public UUID getAsistenciaEstudianteId() {
		return asistenciaEstudianteId;
	}

	public MotivoJustificacion getMotivo() {
		return motivo;
	}

	public String getDetalle() {
		return detalle;
	}

	public String getArchivoNombre() {
		return archivoNombre;
	}

	public String getArchivoTipo() {
		return archivoTipo;
	}

	public Integer getArchivoTamano() {
		return archivoTamano;
	}

	public byte[] getArchivoContenido() {
		return archivoContenido;
	}

	public UUID getRegistradoPor() {
		return registradoPor;
	}

	public OffsetDateTime getRegistradoEn() {
		return registradoEn;
	}
}
