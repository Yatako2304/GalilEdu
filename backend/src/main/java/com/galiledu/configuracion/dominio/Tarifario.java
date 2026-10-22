package com.galiledu.configuracion.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.galiledu.configuracion.dominio.CalendarioCuotas.Cuota;

/**
 * Tarifario de un año escolar (HU-34, P19): tabla maestra de la que P4 genera los compromisos de pago. Vive en
 * {@code pagos.tarifario} (V4), pero el módulo dueño es configuración. Se crea al confirmar, ya CONFIRMADO; si se
 * vuelve a confirmar, guarda los cambios y regenera el calendario.
 *
 * <p>
 * Que sea de solo lectura (año cerrado o con compromisos) lo decide el servicio, porque depende de otros datos. Las
 * reglas de los datos (RF-71, RF-72) están aquí y lanzan {@link IllegalArgumentException}.
 */
@Entity
@Table(schema = "pagos", name = "tarifario")
public class Tarifario {
	/** NUMERIC(10, 2). */
	public static final BigDecimal MONTO_MAXIMO = new BigDecimal("99999999.99");
	public static final int CUOTAS_MAXIMAS = 12;
	public static final int DIAS_GRACIA_MAXIMOS = 60;

	private static final BigDecimal CIEN = BigDecimal.valueOf(100);
	private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "anio_escolar_id", nullable = false, updatable = false)
	private UUID anioEscolarId;

	@Column(name = "monto_cuota_ingreso")
	private BigDecimal montoCuotaIngreso;
	@Column(name = "monto_matricula", nullable = false)
	private BigDecimal montoMatricula;
	@Column(name = "vencimiento_matricula", nullable = false)
	private LocalDate vencimientoMatricula;
	@Column(name = "monto_pension", nullable = false)
	private BigDecimal montoPension;
	@Column(name = "cantidad_cuotas", nullable = false)
	private short cantidadCuotas;
	@Column(name = "dias_gracia", nullable = false)
	private short diasGracia;
	@Column(name = "recargo_porcentaje", nullable = false)
	private BigDecimal recargoPorcentaje;
	@Column(name = "tope_recargo_porcentaje", nullable = false)
	private BigDecimal topeRecargoPorcentaje;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoTarifario estado = EstadoTarifario.EN_CONFIGURACION;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	@OneToMany(mappedBy = "tarifario", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("numero")
	private List<CuotaTarifario> cuotas = new ArrayList<>();

	protected Tarifario() {
	}

	/** Tarifario vacío del año. Queda CONFIRMADO al llamar a {@link #confirmar}; no se guarda antes. */
	public static Tarifario nuevo(UUID anioEscolarId) {
		Tarifario t = new Tarifario();
		t.anioEscolarId = anioEscolarId;
		return t;
	}

	/**
	 * Quita todas las cuotas del calendario. Quien regenere debe hacer flush antes: Hibernate inserta antes de borrar y
	 * chocaría con uq_cuota_tarifario_numero y uq_cuota_tarifario_vencimiento.
	 */
	public void quitarCuotas() {
		cuotas.clear();
	}

	/** Montos y morosidad ya validados y con 2 decimales, listos para guardar. */
	private record Validados(BigDecimal ingreso, BigDecimal matricula, BigDecimal pension, int gracia,
			BigDecimal recargo, BigDecimal tope) {
	}

	/**
	 * Valida lo que {@link #confirmar} guardaría, sin modificar nada (RF-71, RF-72). Se llama antes de quitar las
	 * cuotas
	 * para que un dato inválido no deje el calendario a medias.
	 */
	public static void validar(DatosTarifario datos, LocalDate inicioAnio, LocalDate finAnio) {
		validarDatos(datos);
		validarCalendario(inicioAnio, finAnio);
	}

	/** Valida que el calendario que salga de estas fechas sea válido (RF-71), sin modificar nada. */
	public static void validarCalendario(LocalDate inicioAnio, LocalDate finAnio) {
		validarCalendario(CalendarioCuotas.generar(inicioAnio, finAnio), inicioAnio, finAnio);
	}

	/**
	 * Guarda los montos y la morosidad, regenera el calendario y el vencimiento de la matrícula con las fechas del año
	 * y lo deja CONFIRMADO. Si algún dato es inválido (RF-71, RF-72) no cambia nada; aun así, quien lo llame debe haber
	 * hecho {@link #validar} antes de {@link #quitarCuotas}.
	 */
	public void confirmar(DatosTarifario datos, LocalDate inicioAnio, LocalDate finAnio) {
		Validados v = validarDatos(datos);
		List<Cuota> calendario = CalendarioCuotas.generar(inicioAnio, finAnio);
		validarCalendario(calendario, inicioAnio, finAnio);

		this.montoCuotaIngreso = v.ingreso();
		this.montoMatricula = v.matricula();
		this.montoPension = v.pension();
		this.diasGracia = (short) v.gracia();
		this.recargoPorcentaje = v.recargo();
		this.topeRecargoPorcentaje = v.tope();
		aplicarCalendario(calendario, inicioAnio);
		this.estado = EstadoTarifario.CONFIRMADO;
	}

	private static Validados validarDatos(DatosTarifario datos) {
		BigDecimal ingreso = datos.montoCuotaIngreso() == null
				? null
				: monto(datos.montoCuotaIngreso(), "la cuota de ingreso");
		BigDecimal matricula = monto(obligatorio(datos.montoMatricula(), "Ingresa el monto de la matrícula."),
				"la matrícula");
		BigDecimal pension = monto(obligatorio(datos.montoPension(), "Ingresa el monto de la pensión."), "la pensión");
		if (pension.signum() == 0) {
			throw new IllegalArgumentException("El monto de la pensión debe ser mayor que cero.");
		}
		Integer gracia = obligatorio(datos.diasGracia(), "Ingresa los días de gracia.");
		if (gracia < 0 || gracia > DIAS_GRACIA_MAXIMOS) {
			throw new IllegalArgumentException("Los días de gracia deben estar entre 0 y " + DIAS_GRACIA_MAXIMOS + ".");
		}
		BigDecimal recargo = porcentaje(obligatorio(datos.recargoPorcentaje(), "Ingresa el recargo por cada 30 días."),
				"El recargo");
		BigDecimal tope = porcentaje(obligatorio(datos.topeRecargoPorcentaje(), "Ingresa el tope de recargo."),
				"El tope de recargo");
		if (recargo.compareTo(tope) > 0) {
			throw new IllegalArgumentException("El recargo no puede ser mayor que el tope de recargo.");
		}
		return new Validados(ingreso, matricula, pension, gracia, recargo, tope);
	}

	/**
	 * Regenera solo el calendario y el vencimiento de la matrícula con las fechas nuevas del año (HU-74, editar año).
	 * Los montos, la morosidad y el estado no cambian. Quien lo llame debe haber hecho {@link #validarCalendario}, y
	 * después {@link #quitarCuotas} y flush.
	 */
	public void regenerarCalendario(LocalDate inicioAnio, LocalDate finAnio) {
		List<Cuota> calendario = CalendarioCuotas.generar(inicioAnio, finAnio);
		validarCalendario(calendario, inicioAnio, finAnio);
		aplicarCalendario(calendario, inicioAnio);
	}

	/**
	 * RF-71: entre 1 y 12 cuotas, todas dentro del año y sin dos con el mismo vencimiento. La cantidad declarada
	 * ({@code cantidad_cuotas}) es la del calendario, así que siempre coinciden.
	 */
	static void validarCalendario(List<Cuota> calendario, LocalDate inicioAnio, LocalDate finAnio) {
		if (calendario.isEmpty() || calendario.size() > CUOTAS_MAXIMAS) {
			throw new IllegalArgumentException("El calendario debe tener entre 1 y " + CUOTAS_MAXIMAS
					+ " cuotas, pero el año escolar abarca " + calendario.size() + " meses.");
		}
		Set<LocalDate> vistas = new HashSet<>();
		for (Cuota cuota : calendario) {
			LocalDate fecha = cuota.fechaVencimiento();
			if (fecha.isBefore(inicioAnio) || fecha.isAfter(finAnio)) {
				throw new IllegalArgumentException("Las cuotas deben vencer dentro del año escolar ("
						+ FORMATO_FECHA.format(inicioAnio) + " al " + FORMATO_FECHA.format(finAnio) + ").");
			}
			if (!vistas.add(fecha)) {
				throw new IllegalArgumentException("No puede haber dos cuotas con la misma fecha de vencimiento.");
			}
		}
	}

	private void aplicarCalendario(List<Cuota> calendario, LocalDate inicioAnio) {
		cuotas.clear();
		for (Cuota c : calendario) {
			cuotas.add(new CuotaTarifario(this, c.numero(), c.nombre(), c.fechaVencimiento()));
		}
		cantidadCuotas = (short) calendario.size();
		vencimientoMatricula = CalendarioCuotas.vencimientoMatricula(inicioAnio);
	}

	private static <T> T obligatorio(T valor, String mensaje) {
		if (valor == null) {
			throw new IllegalArgumentException(mensaje);
		}
		return valor;
	}

	/** Un monto en soles: no negativo, hasta {@link #MONTO_MAXIMO} y con 2 decimales como máximo. */
	private static BigDecimal monto(BigDecimal valor, String de) {
		if (valor.signum() < 0) {
			throw new IllegalArgumentException("El monto de " + de + " no puede ser negativo.");
		}
		if (valor.stripTrailingZeros().scale() > 2) {
			throw new IllegalArgumentException("El monto de " + de + " admite hasta 2 decimales.");
		}
		if (valor.compareTo(MONTO_MAXIMO) > 0) {
			throw new IllegalArgumentException("El monto de " + de + " no puede superar S/ " + MONTO_MAXIMO + ".");
		}
		return valor.setScale(2);
	}

	private static BigDecimal porcentaje(BigDecimal valor, String de) {
		if (valor.signum() < 0 || valor.compareTo(CIEN) > 0) {
			throw new IllegalArgumentException(de + " debe estar entre 0 y 100 %.");
		}
		if (valor.stripTrailingZeros().scale() > 2) {
			throw new IllegalArgumentException(de + " admite hasta 2 decimales.");
		}
		return valor.setScale(2);
	}

	public boolean estaConfirmado() {
		return estado == EstadoTarifario.CONFIRMADO;
	}

	/** Total de las pensiones del calendario (pie de la tabla de P19). */
	public BigDecimal totalPensiones() {
		return montoPension.multiply(BigDecimal.valueOf(cuotas.size()));
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
	public UUID getAnioEscolarId() {
		return anioEscolarId;
	}
	public BigDecimal getMontoCuotaIngreso() {
		return montoCuotaIngreso;
	}
	public BigDecimal getMontoMatricula() {
		return montoMatricula;
	}
	public LocalDate getVencimientoMatricula() {
		return vencimientoMatricula;
	}
	public BigDecimal getMontoPension() {
		return montoPension;
	}
	public int getCantidadCuotas() {
		return cantidadCuotas;
	}
	public int getDiasGracia() {
		return diasGracia;
	}
	public BigDecimal getRecargoPorcentaje() {
		return recargoPorcentaje;
	}
	public BigDecimal getTopeRecargoPorcentaje() {
		return topeRecargoPorcentaje;
	}
	public EstadoTarifario getEstado() {
		return estado;
	}
	public List<CuotaTarifario> getCuotas() {
		return Collections.unmodifiableList(cuotas);
	}
}
