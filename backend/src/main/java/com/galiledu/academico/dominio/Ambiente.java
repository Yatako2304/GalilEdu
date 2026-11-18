package com.galiledu.academico.dominio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Ambiente físico: aula regular o espacio especial (tabla configuracion.ambiente, V6). HU-43. No se borra: se
 * desactiva.
 * <p>
 * Reglas de forma de esta clase, que lanzan IllegalArgumentException (el servicio las traduce a 400): nombre
 * obligatorio, aforo mayor que 0 y de un aula regular hasta 45, y recursos normalizados. Las que dependen de otras
 * tablas (nombre repetido, matrícula de las secciones que usan el aula) son del servicio.
 * <p>
 * El código y el tipo son inmutables: no hay forma de cambiarlos y la columna no se actualiza
 * ({@code updatable = false}).
 */
@Entity
@Table(schema = "configuracion", name = "ambiente")
public class Ambiente {
	public static final int LONGITUD_MAXIMA_NOMBRE = 80;
	public static final int LONGITUD_MAXIMA_RECURSO = 60;
	/** La columna es SMALLINT. */
	public static final int AFORO_MAXIMO_COLUMNA = Short.MAX_VALUE;
	public static final String MENSAJE_TIPO_OBLIGATORIO = "Elige el tipo de ambiente.";
	public static final String MENSAJE_NOMBRE_OBLIGATORIO = "Ingresa el nombre del ambiente.";
	public static final String MENSAJE_NOMBRE_LARGO = "El nombre admite como máximo 80 caracteres.";
	public static final String MENSAJE_AFORO_INVALIDO = "El aforo debe ser mayor que 0.";
	public static final String MENSAJE_AFORO_AULA_REGULAR = "Un aula regular admite como máximo 45 estudiantes.";
	public static final String MENSAJE_AFORO_GRANDE = "El aforo no puede ser mayor que 32767.";
	public static final String MENSAJE_RECURSO_LARGO = "Cada recurso admite como máximo 60 caracteres.";
	public static final String MENSAJE_CARACTERES_NO_VALIDOS = "El texto contiene caracteres no válidos.";

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, updatable = false)
	private String codigo;
	@Column(nullable = false)
	private String nombre;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private TipoAmbiente tipo;
	@Column(name = "aforo_maximo", nullable = false)
	private short aforoMaximo;
	/** Columna VARCHAR(60)[]; siempre se reemplaza la lista entera, nunca se modifica por dentro. */
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(nullable = false)
	private List<String> recursos = new ArrayList<>();
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoAmbiente estado = EstadoAmbiente.ACTIVO;
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	protected Ambiente() {
	}

	/** Ambiente nuevo y activo. Todo se valida antes de armarlo. */
	public static Ambiente crear(TipoAmbiente tipo, CodigoAmbiente codigo, String nombre, int aforoMaximo,
			List<String> recursos) {
		if (tipo == null) {
			throw new IllegalArgumentException(MENSAJE_TIPO_OBLIGATORIO);
		}
		if (codigo == null) {
			throw new IllegalArgumentException("El código del ambiente es obligatorio.");
		}
		String nombreNormalizado = normalizarNombre(nombre);
		validarAforo(tipo, aforoMaximo);
		List<String> recursosNormalizados = normalizarRecursos(recursos);

		Ambiente ambiente = new Ambiente();
		ambiente.tipo = tipo;
		ambiente.codigo = codigo.valor();
		ambiente.nombre = nombreNormalizado;
		ambiente.aforoMaximo = (short) aforoMaximo;
		ambiente.recursos = recursosNormalizados;
		return ambiente;
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

	/**
	 * Cambia nombre, aforo y recursos. Si algo no es válido, no cambia nada: Hibernate hace flush del estado de la
	 * entidad, así que se valida todo antes de asignar.
	 */
	public void editar(String nombre, int aforoMaximo, List<String> recursos) {
		String nombreNormalizado = normalizarNombre(nombre);
		validarAforo(tipo, aforoMaximo);
		List<String> recursosNormalizados = normalizarRecursos(recursos);

		this.nombre = nombreNormalizado;
		this.aforoMaximo = (short) aforoMaximo;
		this.recursos = recursosNormalizados;
	}

	/** Si ya está inactivo, no hace nada. */
	public void desactivar() {
		estado = EstadoAmbiente.INACTIVO;
	}

	/** Si ya está activo, no hace nada. */
	public void reactivar() {
		estado = EstadoAmbiente.ACTIVO;
	}

	public boolean estaActivo() {
		return estado == EstadoAmbiente.ACTIVO;
	}

	// ---------- Reglas ----------

	/**
	 * El nombre recortado y con los espacios repetidos reducidos a uno ("Aula 101 " es "Aula 101"). No cambia
	 * mayúsculas ni tildes.
	 */
	public static String normalizarNombre(String nombre) {
		String normalizado = compactar(nombre);
		if (normalizado.isEmpty()) {
			throw new IllegalArgumentException(MENSAJE_NOMBRE_OBLIGATORIO);
		}
		validarCaracteres(normalizado);
		if (longitud(normalizado) > LONGITUD_MAXIMA_NOMBRE) {
			throw new IllegalArgumentException(MENSAJE_NOMBRE_LARGO);
		}
		return normalizado;
	}

	/** El aforo es mayor que 0, cabe en la columna y, en un aula regular, no pasa de 45. */
	public static void validarAforo(TipoAmbiente tipo, int aforoMaximo) {
		if (tipo == null) {
			throw new IllegalArgumentException(MENSAJE_TIPO_OBLIGATORIO);
		}
		if (aforoMaximo <= 0) {
			throw new IllegalArgumentException(MENSAJE_AFORO_INVALIDO);
		}
		if (tipo.esAulaRegular() && aforoMaximo > TipoAmbiente.AFORO_MAXIMO_AULA_REGULAR) {
			throw new IllegalArgumentException(MENSAJE_AFORO_AULA_REGULAR);
		}
		if (aforoMaximo > AFORO_MAXIMO_COLUMNA) {
			throw new IllegalArgumentException(MENSAJE_AFORO_GRANDE);
		}
	}

	/**
	 * Recursos recortados, sin vacíos y sin repetidos (sin distinguir mayúsculas; queda el primero), en el orden en
	 * que llegaron. Una lista nula equivale a ninguno. Cada uno admite hasta 60 caracteres.
	 */
	public static List<String> normalizarRecursos(List<String> recursos) {
		Map<String, String> sinRepetir = new LinkedHashMap<>();
		if (recursos != null) {
			for (String recurso : recursos) {
				String normalizado = compactar(recurso);
				if (normalizado.isEmpty()) {
					continue;
				}
				validarCaracteres(normalizado);
				if (longitud(normalizado) > LONGITUD_MAXIMA_RECURSO) {
					throw new IllegalArgumentException(MENSAJE_RECURSO_LARGO);
				}
				sinRepetir.putIfAbsent(normalizado.toLowerCase(Locale.ROOT), normalizado);
			}
		}
		return new ArrayList<>(sinRepetir.values());
	}

	/** Espacios (también los no separables y los saltos de línea) repetidos a uno, y sin espacios en los extremos. */
	private static String compactar(String texto) {
		return texto == null ? "" : texto.replaceAll("[\\p{Z}\\s]+", " ").strip();
	}

	/** PostgreSQL rechaza el carácter nulo, y los de control no tienen sentido en un nombre ni en un recurso. */
	private static void validarCaracteres(String texto) {
		if (texto.chars().anyMatch(Character::isISOControl)) {
			throw new IllegalArgumentException(MENSAJE_CARACTERES_NO_VALIDOS);
		}
	}

	/** Cuenta caracteres como VARCHAR(n) de PostgreSQL (puntos de código), no unidades de UTF-16. */
	private static int longitud(String texto) {
		return texto.codePointCount(0, texto.length());
	}

	public UUID getId() {
		return id;
	}
	public String getCodigo() {
		return codigo;
	}
	public String getNombre() {
		return nombre;
	}
	public TipoAmbiente getTipo() {
		return tipo;
	}
	public int getAforoMaximo() {
		return aforoMaximo;
	}
	/** Copia de solo lectura: la lista de la entidad no se modifica por fuera. */
	public List<String> getRecursos() {
		return List.copyOf(recursos);
	}
	public EstadoAmbiente getEstado() {
		return estado;
	}
}
