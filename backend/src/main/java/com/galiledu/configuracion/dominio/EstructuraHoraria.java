package com.galiledu.configuracion.dominio;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.galiledu.configuracion.dominio.CalculadoraJornada.Jornada;

/**
 * Estructura horaria de un nivel en un año escolar, p. ej. EH-PRI-26 (tabla configuracion.estructura_horaria, V4 y
 * V7). HU-44.
 * <p>
 * Un año puede tener varias, pero solo la <b>principal</b> de cada nivel se usa (la BD impide dos principales del
 * mismo nivel y año). Los horarios de los bloques y recesos no se guardan: los calcula {@link CalculadoraJornada}.
 * <p>
 * Las reglas de forma (largos, rangos, la hora límite) son de esta clase y lanzan IllegalArgumentException. Las que
 * dependen de otras tablas (código repetido, planes de evaluación) son del servicio.
 */
@Entity
@Table(schema = "configuracion", name = "estructura_horaria")
public class EstructuraHoraria {
	public static final int LONGITUD_MAXIMA_CODIGO = 20;
	public static final int LONGITUD_MAXIMA_DESCRIPCION = 100;
	public static final int BLOQUES_MINIMOS_POR_DIA = 1;
	public static final int BLOQUES_MAXIMOS_POR_DIA = 12;
	public static final int DURACION_MINIMA_BLOQUE_MIN = 20;
	public static final int DURACION_MAXIMA_BLOQUE_MIN = 120;
	/** Hora límite de salida, 18:00, en minutos desde las 00:00. */
	public static final int LIMITE_SALIDA_MINUTOS = 18 * 60;
	public static final String NIVEL_PRIMARIA = "PRIMARIA";
	public static final String NIVEL_SECUNDARIA = "SECUNDARIA";
	public static final String MENSAJE_ANIO_NO_MODIFICABLE = "Solo se puede modificar la estructura horaria de un año planificado.";
	public static final String MENSAJE_JORNADA_MUY_TARDE = "La jornada debe terminar como máximo a las 18:00.";
	public static final String MENSAJE_TEXTO_CON_CARACTERES_DE_CONTROL = "El texto contiene caracteres no válidos.";
	public static final String MENSAJE_CODIGO_INVALIDO = "El código solo admite letras, números y guion.";
	public static final String MENSAJE_DIA_EN_BLANCO = "Elige solo días válidos: de LUN a SAB.";
	/** El código ya normalizado (en mayúsculas): letras A-Z sin tilde, números y guion, como EH-PRI-26. */
	private static final Pattern CODIGO_VALIDO = Pattern.compile("[A-Z0-9-]+");

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "anio_escolar_id", nullable = false, updatable = false)
	private UUID anioEscolarId;
	@Column(nullable = false)
	private String codigo;
	private String descripcion;
	/** El nivel no se cambia: para otro nivel se elimina la estructura y se crea de nuevo. */
	@Column(nullable = false, updatable = false)
	private String nivel;
	@Column(name = "es_principal", nullable = false)
	private boolean esPrincipal;
	/** Códigos LUN…SAB en el orden de la semana (columna VARCHAR(3)[]). */
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "dias_lectivos", nullable = false)
	private List<String> diasLectivos = new ArrayList<>();
	@Column(name = "hora_entrada", nullable = false)
	private LocalTime horaEntrada;
	@Column(name = "bloques_por_dia", nullable = false)
	private short bloquesPorDia;
	@Column(name = "duracion_bloque_min", nullable = false)
	private short duracionBloqueMin;
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "estructura_horaria_id", nullable = false)
	@OrderBy("despuesDeBloque")
	private List<Receso> recesos = new ArrayList<>();
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	protected EstructuraHoraria() {
	}

	/**
	 * Estructura nueva. Si es la primera del nivel en el año, el servicio la crea principal aunque el usuario no
	 * marque la casilla (Figma: "Modal - Nueva estructura horaria").
	 */
	public static EstructuraHoraria crear(UUID anioEscolarId, String nivel, boolean esPrincipal,
			DefinicionEstructura definicion) {
		if (anioEscolarId == null) {
			throw new IllegalArgumentException("Elige el año escolar.");
		}
		EstructuraHoraria estructura = new EstructuraHoraria();
		estructura.anioEscolarId = anioEscolarId;
		estructura.nivel = normalizarNivel(nivel);
		estructura.esPrincipal = esPrincipal;
		estructura.aplicar(definicion);
		return estructura;
	}

	/** Reemplaza los datos editables y regenera los recesos. No cambia el nivel ni si es principal. */
	public void editar(DefinicionEstructura definicion) {
		aplicar(definicion);
	}

	public void marcarComoPrincipal() {
		this.esPrincipal = true;
	}

	public void dejarDeSerPrincipal() {
		this.esPrincipal = false;
	}

	// ---------- Reglas ----------

	/** PRIMARIA o SECUNDARIA, sin importar mayúsculas ni espacios. */
	public static String normalizarNivel(String nivel) {
		String limpio = nivel == null ? "" : nivel.trim().toUpperCase(Locale.ROOT);
		if (!limpio.equals(NIVEL_PRIMARIA) && !limpio.equals(NIVEL_SECUNDARIA)) {
			throw new IllegalArgumentException("Elige el nivel: primaria o secundaria.");
		}
		return limpio;
	}

	/** Solo un año PLANIFICADO admite crear, editar o eliminar estructuras. Se valida antes de cualquier cambio. */
	public static void validarAnioModificable(EstadoAnioEscolar estadoAnio) {
		if (estadoAnio != EstadoAnioEscolar.PLANIFICADO) {
			throw new IllegalStateException(MENSAJE_ANIO_NO_MODIFICABLE);
		}
	}

	/**
	 * Todos los errores de la definición, en el orden del formulario; vacío si es válida. Sirve a la vista previa, que
	 * los muestra todos, y a {@link #crear} y {@link #editar}, que lanzan el primero.
	 */
	public static List<String> detectarErrores(DefinicionEstructura definicion) {
		DefinicionEstructura d = definicion.normalizada();
		List<String> errores = new ArrayList<>();
		errores.addAll(erroresDeIdentificacion(d));
		List<String> erroresDeJornada = erroresDeLaJornada(d);
		errores.addAll(erroresDeJornada);
		if (erroresDeJornada.isEmpty() && calcular(d).salidaMinutos() > LIMITE_SALIDA_MINUTOS) {
			errores.add(MENSAJE_JORNADA_MUY_TARDE);
		}
		return errores;
	}

	/**
	 * La jornada que resultaría, aunque pase de las 18:00, para que la vista previa muestre la hora real de salida.
	 * Vacío si faltan datos para calcularla (bloques, duración, entrada o recesos inválidos).
	 */
	public static Optional<Jornada> calcularJornada(DefinicionEstructura definicion) {
		DefinicionEstructura d = definicion.normalizada();
		if (!erroresDeLaJornada(d).isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(calcular(d));
	}

	private static Jornada calcular(DefinicionEstructura normalizada) {
		return CalculadoraJornada.calcular(normalizada.horaEntrada(), normalizada.bloquesPorDia(),
				normalizada.duracionBloqueMin(), normalizada.recesos());
	}

	private static List<String> erroresDeIdentificacion(DefinicionEstructura d) {
		List<String> errores = new ArrayList<>();
		if (d.codigo().isEmpty()) {
			errores.add("Ingresa el código de la estructura.");
		} else if (tieneCaracteresDeControl(d.codigo())) {
			errores.add(MENSAJE_TEXTO_CON_CARACTERES_DE_CONTROL);
		} else if (d.codigo().length() > LONGITUD_MAXIMA_CODIGO) {
			errores.add("El código admite hasta " + LONGITUD_MAXIMA_CODIGO + " caracteres.");
		} else if (!CODIGO_VALIDO.matcher(d.codigo()).matches()) {
			errores.add(MENSAJE_CODIGO_INVALIDO);
		}
		if (d.descripcion() != null) {
			if (tieneCaracteresDeControl(d.descripcion())) {
				if (!errores.contains(MENSAJE_TEXTO_CON_CARACTERES_DE_CONTROL)) {
					errores.add(MENSAJE_TEXTO_CON_CARACTERES_DE_CONTROL);
				}
			} else if (d.descripcion().length() > LONGITUD_MAXIMA_DESCRIPCION) {
				errores.add("La descripción admite hasta " + LONGITUD_MAXIMA_DESCRIPCION + " caracteres.");
			}
		}
		errores.addAll(erroresDeDias(d.diasLectivos()));
		return errores;
	}

	/**
	 * Si el código, ya normalizado, cumple todas las reglas del código (no vacío, sin controles, largo y caracteres).
	 */
	public static boolean esCodigoValido(String codigoNormalizado) {
		return !codigoNormalizado.isEmpty() && !tieneCaracteresDeControl(codigoNormalizado)
				&& codigoNormalizado.length() <= LONGITUD_MAXIMA_CODIGO
				&& CODIGO_VALIDO.matcher(codigoNormalizado).matches();
	}

	/**
	 * Caracteres de control (NUL, saltos de línea, tabulaciones, escape…). El recorte de los bordes ya los quitó
	 * cuando están al principio o al final; aquí se rechazan los que quedan en medio. PostgreSQL ni siquiera acepta el
	 * NUL en un texto.
	 */
	private static boolean tieneCaracteresDeControl(String texto) {
		for (int posicion = 0; posicion < texto.length(); posicion++) {
			if (Character.isISOControl(texto.charAt(posicion))) {
				return true;
			}
		}
		return false;
	}

	private static List<String> erroresDeDias(List<String> dias) {
		List<String> errores = new ArrayList<>();
		if (dias.isEmpty()) {
			errores.add("Elige al menos un día lectivo.");
			return errores;
		}
		Set<String> vistos = new HashSet<>();
		boolean hayDiaRepetido = false;
		boolean hayDiaEnBlanco = false;
		for (String dia : dias) {
			if (dia.isEmpty()) {
				hayDiaEnBlanco = true;
			} else if (!DiaSemana.esCodigoValido(dia)) {
				errores.add("Día lectivo no válido: " + dia + ".");
			}
			if (!dia.isEmpty() && !vistos.add(dia)) {
				hayDiaRepetido = true;
			}
		}
		if (hayDiaEnBlanco) {
			errores.add(MENSAJE_DIA_EN_BLANCO);
		}
		if (hayDiaRepetido) {
			errores.add("No repitas los días lectivos.");
		}
		return errores;
	}

	/** Entrada, bloques, duración y recesos: lo que hace falta para poder calcular la jornada. */
	private static List<String> erroresDeLaJornada(DefinicionEstructura d) {
		List<String> errores = new ArrayList<>();
		if (d.horaEntrada() == null) {
			errores.add("Ingresa la hora de entrada.");
		}
		boolean bloquesValidos = d.bloquesPorDia() >= BLOQUES_MINIMOS_POR_DIA
				&& d.bloquesPorDia() <= BLOQUES_MAXIMOS_POR_DIA;
		if (!bloquesValidos) {
			errores.add(
					"Los bloques por día van de " + BLOQUES_MINIMOS_POR_DIA + " a " + BLOQUES_MAXIMOS_POR_DIA + ".");
		}
		if (d.duracionBloqueMin() < DURACION_MINIMA_BLOQUE_MIN || d.duracionBloqueMin() > DURACION_MAXIMA_BLOQUE_MIN) {
			errores.add("La duración del bloque va de " + DURACION_MINIMA_BLOQUE_MIN + " a "
					+ DURACION_MAXIMA_BLOQUE_MIN + " minutos.");
		}
		errores.addAll(erroresDeRecesos(d.recesos(), bloquesValidos ? d.bloquesPorDia() : 0));
		return errores;
	}

	/** Con {@code bloquesPorDia} en 0 (bloques inválidos) solo se revisa lo que no depende de ellos. */
	private static List<String> erroresDeRecesos(List<DatosReceso> recesos, int bloquesPorDia) {
		List<String> errores = new ArrayList<>();
		Set<Integer> bloquesConReceso = new HashSet<>();
		boolean hayDuracionInvalida = false;
		boolean hayPosicionInvalida = false;
		boolean hayRecesoRepetido = false;
		for (DatosReceso receso : recesos) {
			if (receso.duracionMin() < Receso.DURACION_MINIMA_MIN
					|| receso.duracionMin() > Receso.DURACION_MAXIMA_MIN) {
				hayDuracionInvalida = true;
			}
			boolean despuesDelUltimoOFuera = bloquesPorDia > 0 && receso.despuesDeBloque() > bloquesPorDia - 1;
			if (receso.despuesDeBloque() < 1 || despuesDelUltimoOFuera) {
				hayPosicionInvalida = true;
			}
			if (!bloquesConReceso.add(receso.despuesDeBloque())) {
				hayRecesoRepetido = true;
			}
		}
		if (hayDuracionInvalida) {
			errores.add("El receso dura de " + Receso.DURACION_MINIMA_MIN + " a " + Receso.DURACION_MAXIMA_MIN
					+ " minutos.");
		}
		// Con bloques inválidos (0) no hay rango que mostrar: ese error ya sale por los bloques.
		if (hayPosicionInvalida && bloquesPorDia > 0) {
			errores.add(bloquesPorDia == 1
					? "Con un solo bloque por día no hay dónde poner un receso."
					: "Un receso va después de un bloque entre 1 y " + (bloquesPorDia - 1) + ".");
		}
		if (hayRecesoRepetido) {
			errores.add("No puede haber dos recesos después del mismo bloque.");
		}
		return errores;
	}

	private void aplicar(DefinicionEstructura definicion) {
		List<String> errores = detectarErrores(definicion);
		if (!errores.isEmpty()) {
			throw new IllegalArgumentException(errores.get(0));
		}
		DefinicionEstructura d = definicion.normalizada();
		this.codigo = d.codigo();
		this.descripcion = d.descripcion();
		this.diasLectivos = new ArrayList<>(d.diasLectivos());
		this.horaEntrada = d.horaEntrada();
		this.bloquesPorDia = (short) d.bloquesPorDia();
		this.duracionBloqueMin = (short) d.duracionBloqueMin();
		reemplazarRecesos(d.recesos());
		// Los recesos son otra tabla: si solo ellos cambian, @PreUpdate no se dispararía y la fecha no se movería.
		this.actualizadoEn = Instant.now();
	}

	/**
	 * Deja los recesos como los pidió el usuario conservando los que siguen en el mismo bloque (solo se actualiza su
	 * duración). Así no se borra y se vuelve a insertar un receso del mismo bloque, que chocaría con la restricción
	 * única (estructura, bloque): Hibernate inserta antes de borrar.
	 */
	private void reemplazarRecesos(List<DatosReceso> recesosPedidos) {
		Iterator<Receso> existentes = recesos.iterator();
		while (existentes.hasNext()) {
			Receso existente = existentes.next();
			DatosReceso pedido = recesoPedidoDespuesDe(recesosPedidos, existente.getDespuesDeBloque());
			if (pedido == null) {
				existentes.remove();
			} else {
				existente.cambiarDuracion(pedido.duracionMin());
			}
		}
		for (DatosReceso pedido : recesosPedidos) {
			if (!tieneRecesoDespuesDe(pedido.despuesDeBloque())) {
				recesos.add(Receso.desde(pedido));
			}
		}
	}

	private static DatosReceso recesoPedidoDespuesDe(List<DatosReceso> recesosPedidos, int despuesDeBloque) {
		for (DatosReceso pedido : recesosPedidos) {
			if (pedido.despuesDeBloque() == despuesDeBloque) {
				return pedido;
			}
		}
		return null;
	}

	private boolean tieneRecesoDespuesDe(int despuesDeBloque) {
		for (Receso existente : recesos) {
			if (existente.getDespuesDeBloque() == despuesDeBloque) {
				return true;
			}
		}
		return false;
	}

	// ---------- Consultas ----------

	/** Bloques de clase por semana: bloques por día por días lectivos. Los recesos no cuentan. */
	public int bloquesPorSemana() {
		return bloquesPorDia * diasLectivos.size();
	}

	/** La jornada de un día, con los horarios de cada bloque y receso. */
	public Jornada jornada() {
		List<DatosReceso> datosDeRecesos = new ArrayList<>();
		for (Receso receso : recesos) {
			datosDeRecesos.add(receso.datos());
		}
		return CalculadoraJornada.calcular(horaEntrada, bloquesPorDia, duracionBloqueMin, datosDeRecesos);
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

	public String getCodigo() {
		return codigo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public String getNivel() {
		return nivel;
	}

	public boolean esPrincipal() {
		return esPrincipal;
	}

	public Instant getActualizadoEn() {
		return actualizadoEn;
	}

	public List<String> getDiasLectivos() {
		return Collections.unmodifiableList(diasLectivos);
	}

	public LocalTime getHoraEntrada() {
		return horaEntrada;
	}

	public int getBloquesPorDia() {
		return bloquesPorDia;
	}

	public int getDuracionBloqueMin() {
		return duracionBloqueMin;
	}

	/**
	 * Los recesos por orden de bloque. Se ordenan aquí y no solo con @OrderBy porque esa anotación actúa al cargar de
	 * la base de datos, y tras {@link #editar} los recesos nuevos están al final de la lista en memoria.
	 */
	public List<Receso> getRecesos() {
		List<Receso> ordenados = new ArrayList<>(recesos);
		ordenados.sort(Comparator.comparingInt(Receso::getDespuesDeBloque));
		return Collections.unmodifiableList(ordenados);
	}
}
