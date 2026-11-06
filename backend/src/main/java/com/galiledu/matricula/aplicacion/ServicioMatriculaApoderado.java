package com.galiledu.matricula.aplicacion;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.ServicioPeriodoMatricula;
import com.galiledu.configuracion.dominio.EstadoVisiblePeriodoMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoGuardado;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoParaVer;
import com.galiledu.matricula.aplicacion.DtosMatricula.DocumentoApoderado;
import com.galiledu.matricula.aplicacion.DtosMatricula.DocumentosApoderado;
import com.galiledu.matricula.aplicacion.DtosMatricula.EstadoMatriculaApoderado;
import com.galiledu.matricula.infraestructura.ConsultasMatricula;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.DocumentoGuardadoFila;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.MatriculaFila;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.PeriodoFila;
import com.galiledu.matricula.infraestructura.EscriturasMatricula;

/**
 * HU-28 y HU-29 (Apoderado): descargar la plantilla de cada documento, subir el firmado y enviarlo a validación. El
 * apoderado solo actúa sobre sus hijos y con el periodo abierto.
 * <p>
 * Si el colegio no creó la matrícula ("Nueva matrícula"), el apoderado la inicia él mismo: elige los documentos en la
 * pantalla y, al «Enviar a validación», {@link #iniciar} crea la matrícula con la sección que asigna el sistema y la
 * deja EN_VALIDACION. Así solo llega a la bandeja del Personal administrativo lo que ya está listo para revisar. Si la
 * matrícula ya existe (la creó el colegio o el colegio observó un documento), se sube documento por documento.
 */
@Service
public class ServicioMatriculaApoderado {
	private final ConsultasMatricula consultas;
	private final EscriturasMatricula escrituras;
	private final ServicioPeriodoMatricula periodos;
	private final ServicioEstadoMatricula estadoMatricula;
	private final ServicioMatriculasPeriodo matriculas;
	private final Clock reloj;

	public ServicioMatriculaApoderado(ConsultasMatricula consultas, EscriturasMatricula escrituras,
			ServicioPeriodoMatricula periodos, ServicioEstadoMatricula estadoMatricula,
			ServicioMatriculasPeriodo matriculas, Clock reloj) {
		this.consultas = consultas;
		this.escrituras = escrituras;
		this.periodos = periodos;
		this.estadoMatricula = estadoMatricula;
		this.matriculas = matriculas;
		this.reloj = reloj;
	}

	/**
	 * Periodo vigente y matrícula del hijo en ese periodo, ya comprobado que el hijo es del apoderado. La matrícula es
	 * {@code null} mientras el apoderado no la inicie.
	 */
	private record Contexto(PeriodoFila periodo, MatriculaFila matricula) {
	}

	@Transactional(readOnly = true)
	public DocumentosApoderado documentos(UUID usuarioId, UUID estudianteId) {
		Contexto c = contexto(usuarioId, estudianteId);
		if (c.matricula() == null) {
			return sinMatricula(usuarioId, estudianteId, c);
		}
		return vista(c);
	}

	/** Plantilla que el apoderado descarga, firma y vuelve a subir. */
	@Transactional(readOnly = true)
	public ArchivoParaVer plantilla(UUID usuarioId, UUID estudianteId, UUID documentoId) {
		Contexto c = contexto(usuarioId, estudianteId);
		exigirDocumentoDelPeriodo(c, documentoId);
		var plantilla = periodos.descargarPlantilla(c.periodo().id(), documentoId);
		return new ArchivoParaVer(plantilla.nombreArchivo(), plantilla.tipoMime(), plantilla.contenido());
	}

	/** El archivo que el apoderado (o el colegio) ya subió, para verlo. */
	@Transactional(readOnly = true)
	public ArchivoParaVer archivo(UUID usuarioId, UUID estudianteId, UUID documentoId) {
		Contexto c = contexto(usuarioId, estudianteId);
		if (c.matricula() == null) {
			throw ExcepcionNegocio.noEncontrado("Ese documento todavía no se subió.");
		}
		return consultas.archivoDelDocumento(c.matricula().id(), documentoId)
				.map(a -> new ArchivoParaVer(a.nombre(), a.tipoMime(), a.contenido()))
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("Ese documento todavía no se subió."));
	}

	/** Sube o reemplaza un documento; queda EN_REVISION. */
	@Transactional
	public DocumentosApoderado subir(UUID usuarioId, UUID estudianteId, UUID documentoId, ArchivoMatricula archivo) {
		Contexto c = conMatricula(contexto(usuarioId, estudianteId));
		exigirEditable(c);
		exigirDocumentoDelPeriodo(c, documentoId);
		boolean aprobado = consultas.documentosGuardados(c.matricula().id()).stream()
				.anyMatch(d -> d.documentoRequeridoId().equals(documentoId) && d.estado().equals("APROBADO"));
		if (aprobado) {
			throw ExcepcionNegocio.conflicto("Ese documento ya fue aprobado por el colegio.");
		}
		PresentacionMatricula.validarArchivo(archivo);
		escrituras.guardarDocumento(c.matricula().id(), documentoId, archivo, usuarioId);
		return vista(c);
	}

	/**
	 * "Enviar a validación": con todos los documentos subidos y sin impedimentos (deuda, periodo) pasa a EN_VALIDACION.
	 */
	@Transactional
	public EstadoMatriculaApoderado enviarAValidacion(UUID usuarioId, UUID estudianteId) {
		Contexto c = conMatricula(contexto(usuarioId, estudianteId));
		exigirEditable(c);
		exigirSinImpedimentos(usuarioId, estudianteId);
		escrituras.marcarEnValidacion(c.matricula().id());
		return estadoMatricula.estado(usuarioId, estudianteId);
	}

	/**
	 * «Enviar a validación» cuando el hijo aún no tiene matrícula: con todos los documentos firmados la crea, con la
	 * sección que asigna el sistema (la primera del grado con cupo, igual que «Nueva matrícula»), y la deja
	 * EN_VALIDACION para que el Personal administrativo la revise. Nada se guarda si falta un documento.
	 */
	@Transactional
	public EstadoMatriculaApoderado iniciar(UUID usuarioId, UUID estudianteId, Map<UUID, ArchivoMatricula> archivos) {
		Contexto c = contexto(usuarioId, estudianteId);
		if (c.matricula() != null) {
			throw ExcepcionNegocio.conflicto("La matrícula de este estudiante ya fue iniciada.");
		}
		exigirSinImpedimentos(usuarioId, estudianteId);
		Set<UUID> pedidos = consultas.documentosRequeridosDe(c.periodo().id()).stream().map(d -> d.id())
				.collect(Collectors.toSet());
		if (!archivos.keySet().containsAll(pedidos)) {
			throw ExcepcionNegocio.invalido(
					"Sube los " + pedidos.size() + " documentos firmados antes de enviarlos a validación.");
		}
		var creada = matriculas.crear(c.periodo().id(), estudianteId, null, archivos, usuarioId);
		if (!"EN_VALIDACION".equals(creada.estado())) {
			// Un periodo sin documentos pedidos: no hay nada que completar.
			escrituras.marcarEnValidacion(creada.id());
		}
		return estadoMatricula.estado(usuarioId, estudianteId);
	}

	private void exigirSinImpedimentos(UUID usuarioId, UUID estudianteId) {
		EstadoMatriculaApoderado estado = estadoMatricula.estado(usuarioId, estudianteId);
		if (!estado.puedeContinuar()) {
			throw ExcepcionNegocio.conflicto(estado.impedimentos().isEmpty()
					? "No se puede enviar la matrícula a validación."
					: estado.impedimentos().getFirst().mensaje());
		}
	}

	/** Documentos pedidos, sin archivos: el apoderado los elige en la pantalla y los envía todos juntos. */
	private DocumentosApoderado sinMatricula(UUID usuarioId, UUID estudianteId, Contexto c) {
		EstadoMatriculaApoderado estado = estadoMatricula.estado(usuarioId, estudianteId);
		List<DocumentoApoderado> documentos = consultas.documentosRequeridosDe(c.periodo().id()).stream()
				.map(d -> new DocumentoApoderado(d.id(), d.nombre(), d.descripcion(), d.tienePlantilla(), null))
				.toList();
		return new DocumentosApoderado(null, estado.puedeContinuar(), documentos);
	}

	private DocumentosApoderado vista(Contexto c) {
		Map<UUID, DocumentoGuardadoFila> guardados = consultas.documentosGuardados(c.matricula().id()).stream()
				.collect(Collectors.toMap(DocumentoGuardadoFila::documentoRequeridoId, Function.identity()));
		List<DocumentoApoderado> documentos = consultas.documentosRequeridosDe(c.periodo().id()).stream()
				.map(d -> new DocumentoApoderado(d.id(), d.nombre(), d.descripcion(), d.tienePlantilla(),
						archivoGuardado(guardados.get(d.id()))))
				.toList();
		return new DocumentosApoderado(c.matricula().estado(), editable(c), documentos);
	}

	private Contexto contexto(UUID usuarioId, UUID estudianteId) {
		UUID persona = consultas.personaDeUsuario(usuarioId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe el usuario."));
		consultas.hijosDe(persona).stream().filter(h -> h.estudianteId().equals(estudianteId)).findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No tienes a este estudiante a tu cargo."));
		PeriodoFila periodo = consultas.periodoVigente()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("Aún no hay un periodo de matrícula configurado."));
		MatriculaFila matricula = consultas.matricula(estudianteId, periodo.anioEscolarId()).orElse(null);
		return new Contexto(periodo, matricula);
	}

	/** Subir uno por uno y «enviar a validación» por separado solo existen cuando la matrícula ya fue creada. */
	private static Contexto conMatricula(Contexto c) {
		if (c.matricula() == null) {
			throw ExcepcionNegocio.conflicto(
					"Primero sube todos los documentos y envíalos a validación para iniciar la matrícula.");
		}
		return c;
	}

	private boolean editable(Contexto c) {
		return "SECCION_RESERVADA".equals(c.matricula().estado())
				&& periodos.estadoEn(c.periodo().id(), reloj.instant()) == EstadoVisiblePeriodoMatricula.ABIERTO;
	}

	private void exigirEditable(Contexto c) {
		if (!editable(c)) {
			throw ExcepcionNegocio.conflicto(
					"Solo puedes subir documentos mientras la matrícula espera tus documentos y el periodo está abierto.");
		}
	}

	private void exigirDocumentoDelPeriodo(Contexto c, UUID documentoId) {
		if (consultas.documentosRequeridosDe(c.periodo().id()).stream().noneMatch(d -> d.id().equals(documentoId))) {
			throw ExcepcionNegocio.invalido("Ese documento no lo pide este periodo de matrícula.");
		}
	}

	private static ArchivoGuardado archivoGuardado(DocumentoGuardadoFila f) {
		return f == null
				? null
				: new ArchivoGuardado(f.nombreArchivo(), f.tipoMime(), f.tamanoBytes(), f.estado(), f.observacion(),
						f.subidoEn());
	}
}
