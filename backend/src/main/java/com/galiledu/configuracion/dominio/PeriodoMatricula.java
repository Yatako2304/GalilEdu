package com.galiledu.configuracion.dominio;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

import com.galiledu.compartido.RelojConfig;

/**
 * Periodo de matrícula regular de un año escolar (tabla matricula.periodo_matricula, V4), con sus documentos
 * requeridos. HU-23 (y HU-21/22) en la pantalla 13. Un periodo por año.
 *
 * <p>
 * Guardar la configuración lo deja HABILITADO. Lo que se ve (PROGRAMADO, ABIERTO, CERRADO) lo calcula
 * {@link #estadoEn} comparando instantes con "ahora"; no se guarda. Solo un periodo PROGRAMADO se edita o se elimina.
 * Las fechas se reciben como fecha y hora de Lima y se guardan como instante.
 *
 * <p>
 * Los flags {@code bloquear_por_morosidad} y {@code permitir_extemporanea} no tienen campo en el formulario: se guardan
 * en true de forma explícita, como muestra el resumen de Figma, sin depender de los valores por defecto de la BD.
 *
 * <p>
 * Las reglas de los datos lanzan {@link IllegalArgumentException} y las de estado {@link IllegalStateException}; el
 * servicio las traduce a 400 y 409. Lo que depende de otros datos (tarifario, secciones, matrículas, otros periodos)
 * lo valida el servicio.
 */
@Entity
@Table(schema = "matricula", name = "periodo_matricula")
public class PeriodoMatricula {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "anio_escolar_id", nullable = false, updatable = false)
	private UUID anioEscolarId;

	@Column(nullable = false)
	private Instant inicio;
	@Column(nullable = false)
	private Instant cierre;

	@Column(name = "bloquear_por_morosidad", nullable = false)
	private boolean bloquearPorMorosidad;
	@Column(name = "permitir_extemporanea", nullable = false)
	private boolean permitirExtemporanea;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoPeriodoMatricula estado = EstadoPeriodoMatricula.EN_CONFIGURACION;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	@OneToMany(mappedBy = "periodo", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orden")
	private List<DocumentoRequerido> documentos = new ArrayList<>();

	protected PeriodoMatricula() {
	}

	/** Periodo HABILITADO del año, con sus documentos. Si algún dato es inválido no crea nada. */
	public static PeriodoMatricula habilitar(UUID anioEscolarId, LocalDateTime inicio, LocalDateTime cierre,
			List<DatosDocumento> documentos, Instant ahora) {
		validar(inicio, cierre, documentos, ahora);
		PeriodoMatricula p = new PeriodoMatricula();
		p.anioEscolarId = anioEscolarId;
		p.inicio = aInstante(inicio);
		p.cierre = aInstante(cierre);
		p.bloquearPorMorosidad = true;
		p.permitirExtemporanea = true;
		p.estado = EstadoPeriodoMatricula.HABILITADO;
		p.aplicarDocumentos(documentos);
		return p;
	}

	/**
	 * Cambia las fechas y los documentos de un periodo PROGRAMADO. Quien lo llame debe haber hecho {@link #validar} y
	 * {@link #retirarDocumentosAusentes} con flush: Hibernate inserta antes de borrar y un documento nuevo con el
	 * nombre
	 * de uno retirado chocaría con uq_documento_requerido_nombre.
	 */
	public void editar(LocalDateTime inicio, LocalDateTime cierre, List<DatosDocumento> documentos, Instant ahora) {
		exigirEditable(ahora);
		validar(inicio, cierre, documentos, ahora);
		this.inicio = aInstante(inicio);
		this.cierre = aInstante(cierre);
		aplicarDocumentos(documentos);
	}

	/**
	 * Valida lo que {@link #habilitar} o {@link #editar} guardarían, sin modificar nada: fechas (cierre posterior al
	 * inicio, inicio no anterior a ahora) y documentos (nombre obligatorio, único y de hasta 100 caracteres;
	 * descripción
	 * de hasta 100).
	 */
	public static void validar(LocalDateTime inicio, LocalDateTime cierre, List<DatosDocumento> documentos,
			Instant ahora) {
		if (inicio == null) {
			throw new IllegalArgumentException("Ingresa la fecha y la hora de inicio.");
		}
		if (cierre == null) {
			throw new IllegalArgumentException("Ingresa la fecha y la hora de cierre.");
		}
		if (!aInstante(cierre).isAfter(aInstante(inicio))) {
			throw new IllegalArgumentException("La fecha de cierre debe ser posterior a la de inicio.");
		}
		if (aInstante(inicio).isBefore(ahora)) {
			throw new IllegalArgumentException("La fecha de inicio no puede ser anterior a la fecha y hora actuales.");
		}
		validarDocumentos(documentos);
	}

	public static void validarDocumentos(List<DatosDocumento> documentos) {
		Set<String> nombres = new HashSet<>();
		for (DatosDocumento d : documentos) {
			if (d.nombre() == null || d.nombre().isBlank()) {
				throw new IllegalArgumentException("Ingresa el nombre del documento.");
			}
			String nombre = d.nombre().strip();
			if (nombre.length() > DocumentoRequerido.LONGITUD_MAXIMA_NOMBRE) {
				throw new IllegalArgumentException("El nombre del documento no puede pasar de "
						+ DocumentoRequerido.LONGITUD_MAXIMA_NOMBRE + " caracteres.");
			}
			if (d.descripcion() != null
					&& d.descripcion().strip().length() > DocumentoRequerido.LONGITUD_MAXIMA_DESCRIPCION) {
				throw new IllegalArgumentException("La descripción del documento no puede pasar de "
						+ DocumentoRequerido.LONGITUD_MAXIMA_DESCRIPCION + " caracteres.");
			}
			if (!nombres.add(nombre.toLowerCase(Locale.ROOT))) {
				throw new IllegalArgumentException("Ya hay un documento llamado \"" + nombre + "\" en este periodo.");
			}
		}
	}

	/** Quita los documentos del periodo que no vienen en la lista (por id). Hacer flush antes de {@link #editar}. */
	public void retirarDocumentosAusentes(List<DatosDocumento> lista) {
		Set<UUID> conservados = new HashSet<>();
		lista.stream().map(DatosDocumento::id).filter(Objects::nonNull).forEach(conservados::add);
		documentos.removeIf(d -> !conservados.contains(d.getId()));
	}

	private void aplicarDocumentos(List<DatosDocumento> lista) {
		Map<UUID, DocumentoRequerido> actuales = new HashMap<>();
		documentos.forEach(d -> actuales.put(d.getId(), d));
		for (DatosDocumento d : lista) {
			if (d.id() != null && !actuales.containsKey(d.id())) {
				throw new IllegalArgumentException("El documento no pertenece a este periodo.");
			}
		}
		retirarDocumentosAusentes(lista);
		for (int i = 0; i < lista.size(); i++) {
			DatosDocumento d = lista.get(i);
			if (d.id() == null) {
				documentos.add(new DocumentoRequerido(this, d.nombre(), d.descripcion(), i + 1));
			} else {
				actuales.get(d.id()).actualizar(d.nombre(), d.descripcion(), i + 1);
			}
		}
		// JPA ordena por "orden" solo al leer; en memoria hay que dejar la lista como la pidió el formulario.
		documentos.sort(Comparator.comparingInt(DocumentoRequerido::getOrden));
	}

	/**
	 * Estado visible: CERRADO si ya pasó el cierre o si está guardado como CERRADO; PROGRAMADO si aún no empieza (o si
	 * nunca se habilitó); ABIERTO si está habilitado y ahora cae entre el inicio y el cierre, ambos incluidos.
	 */
	public EstadoVisiblePeriodoMatricula estadoEn(Instant ahora) {
		if (estado == EstadoPeriodoMatricula.CERRADO || ahora.isAfter(cierre)) {
			return EstadoVisiblePeriodoMatricula.CERRADO;
		}
		if (estado != EstadoPeriodoMatricula.HABILITADO || ahora.isBefore(inicio)) {
			return EstadoVisiblePeriodoMatricula.PROGRAMADO;
		}
		return EstadoVisiblePeriodoMatricula.ABIERTO;
	}

	/** Solo un periodo PROGRAMADO se edita. El texto del periodo abierto es el del modal de Figma. */
	public void exigirEditable(Instant ahora) {
		switch (estadoEn(ahora)) {
			case ABIERTO -> throw new IllegalStateException(
					"Una vez habilitado, el periodo no se puede editar; solo deshabilitar.");
			case CERRADO -> throw new IllegalStateException(
					"El periodo de matrícula ya cerró: no se puede editar.");
			case PROGRAMADO -> {
			}
		}
	}

	/** Solo un periodo PROGRAMADO se elimina. Que no tenga matrículas ni entregas lo comprueba el servicio. */
	public void exigirEliminable(Instant ahora) {
		if (estadoEn(ahora) != EstadoVisiblePeriodoMatricula.PROGRAMADO) {
			throw new IllegalStateException("Solo se puede eliminar un periodo de matrícula programado.");
		}
	}

	/**
	 * ¿Sigue guardado como HABILITADO aunque ya pasó su cierre? El servicio lo marca CERRADO al guardar otro periodo.
	 */
	public boolean estaHabilitadoYVencido(Instant ahora) {
		return estado == EstadoPeriodoMatricula.HABILITADO && ahora.isAfter(cierre);
	}

	public void cerrar() {
		estado = EstadoPeriodoMatricula.CERRADO;
	}

	/** La fecha y la hora local (Lima) con que el formulario muestra un instante guardado. */
	public static LocalDateTime aHoraLocal(Instant instante) {
		return LocalDateTime.ofInstant(instante, RelojConfig.LIMA);
	}

	private static Instant aInstante(LocalDateTime local) {
		return local.atZone(RelojConfig.LIMA).toInstant();
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
	public Instant getInicio() {
		return inicio;
	}
	public Instant getCierre() {
		return cierre;
	}
	public boolean isBloquearPorMorosidad() {
		return bloquearPorMorosidad;
	}
	public boolean isPermitirExtemporanea() {
		return permitirExtemporanea;
	}
	public EstadoPeriodoMatricula getEstado() {
		return estado;
	}
	public List<DocumentoRequerido> getDocumentos() {
		return Collections.unmodifiableList(documentos);
	}
}
