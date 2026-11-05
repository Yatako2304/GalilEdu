package com.galiledu.pagos.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * HU-35: descuento del catálogo (Figma P21). Las reglas (porcentaje entre 1 y el límite, al menos un concepto, tramos
 * sin superponerse) se validan en {@code ServicioCatalogoDescuentos}; la BD repite las básicas.
 */
@Entity
@Table(schema = "pagos", name = "descuento")
public class Descuento {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String nombre;
	private String descripcion;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoDescuento tipo;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AplicacionDescuento aplicacion;
	/** Solo PORCENTAJE_UNICO; POR_HERMANOS usa sus tramos. */
	private BigDecimal porcentaje;
	private String condicion;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoDescuento estado;
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	@OneToMany(mappedBy = "descuento", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("hermanosDesde")
	private List<DescuentoTramo> tramos = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(schema = "pagos", name = "descuento_concepto", joinColumns = @JoinColumn(name = "descuento_id"))
	@Column(name = "concepto_codigo", nullable = false)
	private Set<String> conceptos = new LinkedHashSet<>();

	protected Descuento() {
	}

	public static Descuento nuevo(String nombre, String descripcion, TipoDescuento tipo,
			AplicacionDescuento aplicacion, BigDecimal porcentaje, String condicion, Set<String> conceptos) {
		Descuento d = new Descuento();
		d.estado = EstadoDescuento.ACTIVO;
		d.creadoEn = Instant.now();
		d.actualizar(nombre, descripcion, tipo, aplicacion, porcentaje, condicion, conceptos);
		return d;
	}

	/** Cambia los datos; los tramos se reemplazan aparte con {@link #agregarTramo} tras {@link #quitarTramos}. */
	public void actualizar(String nombre, String descripcion, TipoDescuento tipo, AplicacionDescuento aplicacion,
			BigDecimal porcentaje, String condicion, Set<String> conceptos) {
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.tipo = tipo;
		this.aplicacion = aplicacion;
		this.porcentaje = tipo == TipoDescuento.PORCENTAJE_UNICO ? porcentaje : null;
		this.condicion = condicion;
		this.conceptos.retainAll(conceptos);
		this.conceptos.addAll(conceptos);
		this.actualizadoEn = Instant.now();
	}

	public void quitarTramos() {
		tramos.clear();
	}

	public void agregarTramo(int desde, Integer hasta, BigDecimal porcentaje) {
		tramos.add(new DescuentoTramo(this, desde, hasta, porcentaje));
	}

	public void cambiarEstado(EstadoDescuento nuevo) {
		this.estado = nuevo;
		this.actualizadoEn = Instant.now();
	}

	public boolean estaActivo() {
		return estado == EstadoDescuento.ACTIVO;
	}

	public boolean aplicaA(String concepto) {
		return conceptos.contains(concepto);
	}

	/**
	 * Porcentaje que corresponde al estudiante: el fijo, o el del tramo que cubre a sus hermanos matriculados (contando
	 * al propio estudiante). Vacío si ningún tramo lo cubre.
	 */
	public Optional<BigDecimal> porcentajePara(int hermanosMatriculados) {
		if (tipo == TipoDescuento.PORCENTAJE_UNICO) {
			return Optional.ofNullable(porcentaje);
		}
		return tramos.stream().filter(t -> t.cubre(hermanosMatriculados)).map(DescuentoTramo::getPorcentaje)
				.findFirst();
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

	public TipoDescuento getTipo() {
		return tipo;
	}

	public AplicacionDescuento getAplicacion() {
		return aplicacion;
	}

	public BigDecimal getPorcentaje() {
		return porcentaje;
	}

	public String getCondicion() {
		return condicion;
	}

	public EstadoDescuento getEstado() {
		return estado;
	}

	public List<DescuentoTramo> getTramos() {
		return tramos;
	}

	public Set<String> getConceptos() {
		return conceptos;
	}
}
