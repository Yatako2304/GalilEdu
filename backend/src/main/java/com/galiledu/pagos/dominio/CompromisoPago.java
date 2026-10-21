package com.galiledu.pagos.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.Formula;

/**
 * Compromiso de pago de un estudiante en un año escolar (tabla pagos.compromiso_pago, V5). HU-37.
 * Los datos de otros módulos (estudiante, apoderado, sección, año, tarifario) solo se leen con @Formula.
 */
@Entity
@Table(schema = "pagos", name = "compromiso_pago")
public class CompromisoPago {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "estudiante_id", nullable = false)
	private UUID estudianteId;
	@Column(name = "anio_escolar_id", nullable = false)
	private UUID anioEscolarId;
	@Column(name = "matricula_id")
	private UUID matriculaId;
	@Column(name = "tarifario_id", nullable = false)
	private UUID tarifarioId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoCompromiso estado;

	@OneToMany(mappedBy = "compromiso", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@OrderBy("fechaVencimiento DESC")
	private List<Cuota> cuotas = new ArrayList<>();

	@Formula("(SELECT a.anio FROM configuracion.anio_escolar a WHERE a.id = anio_escolar_id)")
	private Integer anio;

	@Formula("(SELECT t.dias_gracia FROM pagos.tarifario t WHERE t.id = tarifario_id)")
	private Integer diasGracia;

	@Formula("(SELECT t.monto_pension FROM pagos.tarifario t WHERE t.id = tarifario_id)")
	private BigDecimal montoPension;

	@Formula("""
			(SELECT concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno)
			 FROM estudiantes.estudiante e JOIN personas.persona p ON p.id = e.persona_id
			 WHERE e.id = estudiante_id)""")
	private String estudianteNombre;

	@Formula("""
			(SELECT p.numero_documento
			 FROM estudiantes.estudiante e JOIN personas.persona p ON p.id = e.persona_id
			 WHERE e.id = estudiante_id)""")
	private String estudianteDocumento;

	/** Apoderado responsable de la matrícula (estudiantes.vinculo_familiar.es_responsable_matricula). */
	@Formula("""
			(SELECT min(concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno))
			 FROM estudiantes.vinculo_familiar v JOIN personas.persona p ON p.id = v.persona_id
			 WHERE v.estudiante_id = estudiante_id AND v.es_responsable_matricula)""")
	private String apoderadoNombre;

	/** "4.° A": grado y sección de la matrícula del compromiso. */
	@Formula("""
			(SELECT g.numero || '.° ' || s.nombre
			 FROM matricula.matricula m
			 JOIN configuracion.seccion s ON s.id = m.seccion_id
			 JOIN configuracion.grado g ON g.codigo = s.grado_codigo
			 WHERE m.id = matricula_id)""")
	private String seccion;

	/** Descuento ACTIVO del estudiante (HU-38): solo puede haber uno (RF-87). */
	@Formula("""
			(SELECT d.porcentaje FROM pagos.descuento_aplicado d
			 WHERE d.estudiante_id = estudiante_id AND d.estado = 'ACTIVO')""")
	private BigDecimal descuentoPorcentaje;

	@Formula("""
			(SELECT d.nombre FROM pagos.descuento_aplicado d
			 WHERE d.estudiante_id = estudiante_id AND d.estado = 'ACTIVO')""")
	private String descuentoNombre;

	protected CompromisoPago() {
	}

	/** Nuevo compromiso VIGENTE de un estudiante para un año (RF-83: uno por estudiante y año). */
	public static CompromisoPago nuevo(UUID estudianteId, UUID anioEscolarId, UUID matriculaId, UUID tarifarioId) {
		CompromisoPago c = new CompromisoPago();
		c.estudianteId = estudianteId;
		c.anioEscolarId = anioEscolarId;
		c.matriculaId = matriculaId;
		c.tarifarioId = tarifarioId;
		c.estado = EstadoCompromiso.VIGENTE;
		return c;
	}

	public void agregarCuota(Cuota cuota) {
		cuotas.add(cuota);
	}

	/** RF-83: ¿ya tiene pensiones generadas (sin contar las anuladas)? */
	public boolean tienePensiones() {
		return cuotas.stream().anyMatch(q -> q.esPension() && q.estaActiva());
	}

	/** RF-79: ¿ya tiene una cuota de matrícula (sin contar las anuladas)? */
	public boolean tieneCuotaMatricula() {
		return cuotas.stream().anyMatch(q -> q.esMatricula() && q.estaActiva());
	}

	/** Vincula el compromiso a la matrícula si se creó antes sin ella. */
	public void vincularMatricula(UUID matriculaId) {
		if (this.matriculaId == null) {
			this.matriculaId = matriculaId;
		}
	}

	public UUID getId() {
		return id;
	}

	public UUID getEstudianteId() {
		return estudianteId;
	}

	public EstadoCompromiso getEstado() {
		return estado;
	}

	public List<Cuota> getCuotas() {
		return cuotas;
	}

	public int getAnio() {
		return anio == null ? 0 : anio;
	}

	public int getDiasGracia() {
		return diasGracia == null ? 0 : diasGracia;
	}

	public BigDecimal getMontoPension() {
		return montoPension;
	}

	public String getEstudianteNombre() {
		return estudianteNombre;
	}

	public String getEstudianteDocumento() {
		return estudianteDocumento;
	}

	public String getApoderadoNombre() {
		return apoderadoNombre;
	}

	public String getSeccion() {
		return seccion;
	}

	public BigDecimal getDescuentoPorcentaje() {
		return descuentoPorcentaje;
	}

	public String getDescuentoNombre() {
		return descuentoNombre;
	}
}
