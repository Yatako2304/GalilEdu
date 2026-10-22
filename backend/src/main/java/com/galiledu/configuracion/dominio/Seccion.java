package com.galiledu.configuracion.dominio;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

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

import org.hibernate.annotations.Formula;

/**
 * Sección de un grado en un año escolar (tabla configuracion.seccion, V4). HU-75.
 * <p>
 * La capacidad máxima son las vacantes de la matrícula. Los alumnos de la sección no se guardan aquí: son las
 * matrículas de matricula.matricula que apuntan a ella, y solo se cuentan (solo lectura).
 */
@Entity
@Table(schema = "configuracion", name = "seccion")
public class Seccion {
	public static final int CAPACIDAD_MINIMA = 1;
	public static final int CAPACIDAD_MAXIMA = 45;
	public static final int LONGITUD_MAXIMA_NOMBRE = 5;

	/**
	 * Condición SQL de las matrículas que ocupan vacante (RF-49, RF-50): todas menos las RETIRADO y ANULADA. La
	 * matrícula es la "m" de matricula.matricula. Una sola definición para el conteo de la entidad y para las
	 * consultas del listado.
	 */
	public static final String SQL_OCUPA_VACANTE = "m.estado NOT IN ('RETIRADO', 'ANULADA')";

	private static final Pattern NOMBRE_VALIDO = Pattern.compile("[\\p{L}\\p{N}]+");

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "anio_escolar_id", nullable = false, updatable = false)
	private UUID anioEscolarId;
	@Column(name = "grado_codigo", nullable = false, updatable = false)
	private String gradoCodigo;
	@Column(nullable = false)
	private String nombre;
	@Column(name = "capacidad_maxima", nullable = false)
	private short capacidadMaxima;
	/** Usuarios de seguridad.usuario: solo se guarda el id, la entidad es del módulo de usuarios. */
	@Column(name = "tutor_id")
	private UUID tutorId;
	@Column(name = "cotutor_id")
	private UUID cotutorId;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoSeccion estado = EstadoSeccion.ACTIVA;
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	@Formula("(SELECT count(*) FROM matricula.matricula m WHERE m.seccion_id = id AND " + SQL_OCUPA_VACANTE + ")")
	private int alumnosOcupanVacante;

	protected Seccion() {
	}

	// Creación de una sección nueva a partir de una SolicitudSeccion y AnioAcademico
	/** Sección ACTIVA y sin tutores: el tutor se asigna después (Figma: "Modal - Nueva sección"). */
	public static Seccion crear(UUID anioEscolarId, String gradoCodigo, String nombre, int capacidadMaxima) {
		if (anioEscolarId == null) {
			throw new IllegalArgumentException("Elige el año escolar.");
		}
		if (gradoCodigo == null || gradoCodigo.isBlank()) {
			throw new IllegalArgumentException("Elige un grado.");
		}
		Seccion s = new Seccion();
		s.anioEscolarId = anioEscolarId;
		s.gradoCodigo = gradoCodigo;
		s.nombre = normalizarNombre(nombre);
		s.capacidadMaxima = validarCapacidad(capacidadMaxima);
		return s;
	}

	// Normalización del nombre de la seccción
	/** "a " y "A" son el mismo nombre: se guarda sin espacios y en mayúsculas. */
	public static String normalizarNombre(String nombre) {
		String limpio = nombre == null ? "" : nombre.trim().toUpperCase(Locale.ROOT);
		if (limpio.isEmpty()) {
			throw new IllegalArgumentException("Ingresa el nombre de la sección.");
		}
		if (limpio.length() > LONGITUD_MAXIMA_NOMBRE) {
			throw new IllegalArgumentException(
					"El nombre de la sección admite hasta " + LONGITUD_MAXIMA_NOMBRE + " caracteres.");
		}
		if (!NOMBRE_VALIDO.matcher(limpio).matches()) {
			throw new IllegalArgumentException("El nombre de la sección solo admite letras y números.");
		}
		return limpio;
	}

	// Validar la capacidad de la sección. Lanza excepción si no está entre CAPACIDAD_MINIMA y CAPACIDAD_MAXIMA. Devuelve la capacidad validada como short.
	public static short validarCapacidad(int capacidad) {
		if (capacidad < CAPACIDAD_MINIMA || capacidad > CAPACIDAD_MAXIMA) {
			throw new IllegalArgumentException("Entre " + CAPACIDAD_MINIMA + " y " + CAPACIDAD_MAXIMA + " alumnos.");
		}
		return (short) capacidad;
	}

	/**
	 * Una sección de un año CERRADO es un registro histórico: no se crea, edita ni elimina nada en él. Se valida antes
	 * de cualquier cambio.
	 */
	public static void validarAnioModificable(EstadoAnioEscolar estadoAnio) {
		if (estadoAnio == EstadoAnioEscolar.CERRADO) {
			throw new IllegalStateException("El año escolar está cerrado: no se pueden modificar sus secciones.");
		}
	}

	/**
	 * RF-50: la capacidad no baja de los alumnos que ya ocupan vacante. Se recibe el conteo como parámetro (el dominio
	 * no consulta la base); el servicio lo toma de {@link #getAlumnosOcupanVacante()}.
	 */
	public void cambiarCapacidad(int capacidadNueva, int alumnosOcupanVacante) {
		short validada = validarCapacidad(capacidadNueva);
		if (validada < alumnosOcupanVacante) {
			throw new IllegalStateException(
					"La capacidad no puede ser menor que los alumnos que ocupan vacante (" + alumnosOcupanVacante
							+ ").");
		}
		this.capacidadMaxima = validada;
	}

	/**
	 * Reemplaza al tutor y al cotutor (Figma: "Modal - Asignar tutor"). El tutor es obligatorio; el cotutor es opcional
	 * (null lo quita) y distinto del tutor. Que ambos sean usuarios activos con rol DOCENTE o TUTOR, y que el tutor no
	 * lo sea ya de otra sección del año, lo valida el servicio.
	 */
	public void asignarTutores(UUID tutorId, UUID cotutorId) {
		validarTutores(tutorId, cotutorId);
		this.tutorId = tutorId;
		this.cotutorId = cotutorId;
	}

	/** Las reglas de {@link #asignarTutores} sin modificar la sección, para validar antes de otras consultas. */
	public static void validarTutores(UUID tutorId, UUID cotutorId) {
		if (tutorId == null) {
			throw new IllegalArgumentException("Elige un tutor.");
		}
		if (tutorId.equals(cotutorId)) {
			throw new IllegalArgumentException("El cotutor debe ser distinto del tutor.");
		}
	}

	public void inactivar() {
		if (estado == EstadoSeccion.INACTIVA) {
			throw new IllegalStateException("La sección ya está inactiva.");
		}
		estado = EstadoSeccion.INACTIVA;
	}

	public void reactivar() {
		if (estado == EstadoSeccion.ACTIVA) {
			throw new IllegalStateException("La sección ya está activa.");
		}
		estado = EstadoSeccion.ACTIVA;
	}

	public boolean estaActiva() {
		return estado == EstadoSeccion.ACTIVA;
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
	public String getGradoCodigo() {
		return gradoCodigo;
	}
	public String getNombre() {
		return nombre;
	}
	public int getCapacidadMaxima() {
		return capacidadMaxima;
	}
	public UUID getTutorId() {
		return tutorId;
	}
	public UUID getCotutorId() {
		return cotutorId;
	}
	public EstadoSeccion getEstado() {
		return estado;
	}
	/** Alumnos con matrícula en esta sección que ocupan vacante, calculado al leerla (0 si aún no se guardó). */
	public int getAlumnosOcupanVacante() {
		return alumnosOcupanVacante;
	}
}
