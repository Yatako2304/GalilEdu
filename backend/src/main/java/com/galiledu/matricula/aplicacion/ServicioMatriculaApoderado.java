package com.galiledu.matricula.aplicacion;

import java.time.Clock;
import java.util.List;
import java.util.Map;
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
 * apoderado solo actúa sobre sus hijos y mientras la matrícula está en SECCION_RESERVADA con el periodo abierto.
 */
@Service
public class ServicioMatriculaApoderado {
	private final ConsultasMatricula consultas;
	private final EscriturasMatricula escrituras;
	private final ServicioPeriodoMatricula periodos;
	private final ServicioEstadoMatricula estadoMatricula;
	private final Clock reloj;

	public ServicioMatriculaApoderado(ConsultasMatricula consultas, EscriturasMatricula escrituras,
			ServicioPeriodoMatricula periodos, ServicioEstadoMatricula estadoMatricula, Clock reloj) {
		this.consultas = consultas;
		this.escrituras = escrituras;
		this.periodos = periodos;
		this.estadoMatricula = estadoMatricula;
		this.reloj = reloj;
	}

	/** Matrícula del hijo en el periodo vigente, ya comprobado que el hijo es del apoderado. */
	private record Contexto(PeriodoFila periodo, MatriculaFila matricula) {
	}

	@Transactional(readOnly = true)
	public DocumentosApoderado documentos(UUID usuarioId, UUID estudianteId) {
		Contexto c = contexto(usuarioId, estudianteId);
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
		return consultas.archivoDelDocumento(c.matricula().id(), documentoId)
				.map(a -> new ArchivoParaVer(a.nombre(), a.tipoMime(), a.contenido()))
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("Ese documento todavía no se subió."));
	}

	/** Sube o reemplaza un documento; queda EN_REVISION. */
	@Transactional
	public DocumentosApoderado subir(UUID usuarioId, UUID estudianteId, UUID documentoId, ArchivoMatricula archivo) {
		Contexto c = contexto(usuarioId, estudianteId);
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
		Contexto c = contexto(usuarioId, estudianteId);
		exigirEditable(c);
		EstadoMatriculaApoderado estado = estadoMatricula.estado(usuarioId, estudianteId);
		if (!estado.puedeContinuar()) {
			throw ExcepcionNegocio.conflicto(estado.impedimentos().isEmpty()
					? "No se puede enviar la matrícula a validación."
					: estado.impedimentos().getFirst().mensaje());
		}
		escrituras.marcarEnValidacion(c.matricula().id());
		return estadoMatricula.estado(usuarioId, estudianteId);
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
		MatriculaFila matricula = consultas.matricula(estudianteId, periodo.anioEscolarId()).orElseThrow(
				() -> ExcepcionNegocio.noEncontrado("El colegio aún no reserva una sección para este estudiante."));
		return new Contexto(periodo, matricula);
	}

	private boolean editable(Contexto c) {
		return "SECCION_RESERVADA".equals(c.matricula().estado())
				&& periodos.estadoEn(c.periodo().id(), reloj.instant()) == EstadoVisiblePeriodoMatricula.ABIERTO;
	}

	private void exigirEditable(Contexto c) {
		if (!editable(c)) {
			throw ExcepcionNegocio.conflicto(
					"Solo puedes subir documentos mientras la sección está reservada y el periodo de matrícula está abierto.");
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
