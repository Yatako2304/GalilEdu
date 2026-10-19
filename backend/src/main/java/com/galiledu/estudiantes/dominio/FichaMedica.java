package com.galiledu.estudiantes.dominio;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Paso 2 de la inscripción (tabla estudiantes.ficha_medica). */
@Entity
@Table(schema = "estudiantes", name = "ficha_medica")
public class FichaMedica {
	@Id
	@Column(name = "estudiante_id")
	private UUID estudianteId;

	@MapsId
	@OneToOne
	@JoinColumn(name = "estudiante_id")
	private Estudiante estudiante;

	@Column(name = "tipo_sangre")
	private String tipoSangre;
	@Column(name = "peso_kg")
	private BigDecimal pesoKg;
	@Column(name = "estatura_m")
	private BigDecimal estaturaM;
	@Column(name = "tipo_seguro")
	private String tipoSeguro;
	@Column(name = "tiene_alergias", nullable = false)
	private boolean tieneAlergias;
	@Column(name = "alergias_alimentarias")
	private String alergiasAlimentarias;
	@Column(name = "alergias_medicamentosas")
	private String alergiasMedicamentosas;
	@Column(name = "fiebre_maxima")
	private String fiebreMaxima;
	@Column(name = "ha_convulsionado", nullable = false)
	private boolean haConvulsionado;
	@Column(name = "tiene_enfermedad", nullable = false)
	private boolean tieneEnfermedad;
	@Column(name = "enfermedad_detalle")
	private String enfermedadDetalle;
	@Column(name = "toma_medicamento", nullable = false)
	private boolean tomaMedicamento;
	@Column(name = "medicamento_detalle")
	private String medicamentoDetalle;
	private String observaciones;

	protected FichaMedica() {
	}

	public FichaMedica(String tipoSangre, BigDecimal pesoKg, BigDecimal estaturaM, String tipoSeguro,
			boolean tieneAlergias, String alergiasAlimentarias, String alergiasMedicamentosas, String fiebreMaxima,
			boolean haConvulsionado, boolean tieneEnfermedad, String enfermedadDetalle, boolean tomaMedicamento,
			String medicamentoDetalle, String observaciones) {
		this.tipoSangre = tipoSangre;
		this.pesoKg = pesoKg;
		this.estaturaM = estaturaM;
		this.tipoSeguro = tipoSeguro;
		this.tieneAlergias = tieneAlergias;
		this.alergiasAlimentarias = alergiasAlimentarias;
		this.alergiasMedicamentosas = alergiasMedicamentosas;
		this.fiebreMaxima = fiebreMaxima;
		this.haConvulsionado = haConvulsionado;
		this.tieneEnfermedad = tieneEnfermedad;
		this.enfermedadDetalle = enfermedadDetalle;
		this.tomaMedicamento = tomaMedicamento;
		this.medicamentoDetalle = medicamentoDetalle;
		this.observaciones = observaciones;
	}

	void asociar(Estudiante estudiante) {
		this.estudiante = estudiante;
	}

	public String getTipoSangre() {
		return tipoSangre;
	}
	public BigDecimal getPesoKg() {
		return pesoKg;
	}
	public BigDecimal getEstaturaM() {
		return estaturaM;
	}
	public String getTipoSeguro() {
		return tipoSeguro;
	}
	public boolean isTieneAlergias() {
		return tieneAlergias;
	}
	public String getAlergiasAlimentarias() {
		return alergiasAlimentarias;
	}
	public String getAlergiasMedicamentosas() {
		return alergiasMedicamentosas;
	}
	public String getFiebreMaxima() {
		return fiebreMaxima;
	}
	public boolean isHaConvulsionado() {
		return haConvulsionado;
	}
	public boolean isTieneEnfermedad() {
		return tieneEnfermedad;
	}
	public String getEnfermedadDetalle() {
		return enfermedadDetalle;
	}
	public boolean isTomaMedicamento() {
		return tomaMedicamento;
	}
	public String getMedicamentoDetalle() {
		return medicamentoDetalle;
	}
	public String getObservaciones() {
		return observaciones;
	}
}
