package com.galiledu.configuracion.dominio;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Documento que se pide al matricular (tabla matricula.documento_requerido, V5). HU-21/22 en la pantalla 13.
 *
 * <p>
 * De la plantilla solo se mapean el nombre del archivo y su tipo, de solo lectura: el contenido (BYTEA, hasta 10 MB) no
 * se carga con la entidad y se guarda, lee y quita con SQL en una sola sentencia, porque el CHECK
 * ck_documento_requerido_plantilla exige que nombre y contenido estén los dos o ninguno.
 */
@Entity
@Table(schema = "matricula", name = "documento_requerido")
public class DocumentoRequerido {
	public static final int LONGITUD_MAXIMA_NOMBRE = 100;
	/** Figma: el contador de la descripción llega a 100. La columna admite 2000. */
	public static final int LONGITUD_MAXIMA_DESCRIPCION = 100;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "periodo_matricula_id", nullable = false, updatable = false)
	private PeriodoMatricula periodo;

	@Column(nullable = false)
	private String nombre;
	private String descripcion;
	@Column(nullable = false)
	private short orden;

	@Column(name = "plantilla_nombre_archivo", insertable = false, updatable = false)
	private String plantillaNombreArchivo;
	@Column(name = "plantilla_tipo_mime", insertable = false, updatable = false)
	private String plantillaTipoMime;

	protected DocumentoRequerido() {
	}

	/** Solo la crea {@link PeriodoMatricula}. */
	DocumentoRequerido(PeriodoMatricula periodo, String nombre, String descripcion, int orden) {
		this.periodo = periodo;
		actualizar(nombre, descripcion, orden);
	}

	void actualizar(String nombre, String descripcion, int orden) {
		this.nombre = nombre.strip();
		this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
		this.orden = (short) orden;
	}

	public boolean tienePlantilla() {
		return plantillaNombreArchivo != null;
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
	public int getOrden() {
		return orden;
	}
	public String getPlantillaNombreArchivo() {
		return plantillaNombreArchivo;
	}
	public String getPlantillaTipoMime() {
		return plantillaTipoMime;
	}
}
