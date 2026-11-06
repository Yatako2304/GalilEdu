package com.galiledu.matricula.aplicacion;

import static com.galiledu.matricula.aplicacion.PresentacionMatricula.etiqueta;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SeccionConVacantes;
import com.galiledu.configuracion.aplicacion.ServicioPeriodoMatricula;
import com.galiledu.configuracion.aplicacion.ServicioSecciones;
import com.galiledu.configuracion.dominio.EstadoVisiblePeriodoMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoGuardado;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoParaVer;
import com.galiledu.matricula.aplicacion.DtosMatricula.DetalleMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.DocumentoDeMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.SeccionAsignada;
import com.galiledu.matricula.aplicacion.DtosMatricula.SeccionesDelGrado;
import com.galiledu.matricula.infraestructura.ConsultasMatricula;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.DocumentoGuardadoFila;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.MatriculaDetalleFila;
import com.galiledu.matricula.infraestructura.EscriturasMatricula;

/**
 * HU-24: detalle de una matrícula (A5, B6, B7), cambio de sección (A5 modal) y documentos del Personal administrativo.
 */
@Service
public class ServicioMatriculaAdministrativa {
	private final ConsultasMatricula consultas;
	private final EscriturasMatricula escrituras;
	private final ServicioPeriodoMatricula periodos;
	private final ServicioSecciones secciones;
	private final Clock reloj;

	public ServicioMatriculaAdministrativa(ConsultasMatricula consultas, EscriturasMatricula escrituras,
			ServicioPeriodoMatricula periodos, ServicioSecciones secciones, Clock reloj) {
		this.consultas = consultas;
		this.escrituras = escrituras;
		this.periodos = periodos;
		this.secciones = secciones;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public DetalleMatricula detalle(UUID matriculaId) {
		MatriculaDetalleFila m = buscar(matriculaId);
		SeccionAsignada seccion = delAnio(m).stream().filter(s -> s.id().equals(m.seccionId())).findFirst()
				.map(s -> new SeccionAsignada(s.id(), etiqueta(s), s.tutor(), s.capacidadMaxima(),
						s.alumnosOcupanVacante()))
				.orElse(null);
		var apoderado = consultas.responsableDe(m.estudianteId());
		var emergencia = consultas.contactoDeEmergencia(m.estudianteId());
		Map<UUID, DocumentoGuardadoFila> guardados = consultas.documentosGuardados(matriculaId).stream()
				.collect(Collectors.toMap(DocumentoGuardadoFila::documentoRequeridoId, Function.identity()));
		List<DocumentoDeMatricula> documentos = consultas.documentosRequeridosDe(m.periodoId()).stream()
				.map(d -> new DocumentoDeMatricula(d.id(), d.nombre(), d.descripcion(), d.tienePlantilla(),
						archivoGuardado(guardados.get(d.id()))))
				.toList();
		return new DetalleMatricula(m.id(), m.periodoId(), m.anio(), m.estudianteId(), m.nombre(), m.documento(),
				m.tipo(), m.gradoCodigo(), m.grado(), seccion, m.estado(), m.codigo(), paso(m.estado()),
				apoderado.map(a -> a.nombre()).orElse(null),
				apoderado.map(a -> PresentacionMatricula.unir(a.celular(), a.correo())).orElse(null),
				emergencia.map(c -> c.nombre() + " (" + c.parentesco() + ")"
						+ (c.celular() == null ? "" : " · " + c.celular())).orElse(null),
				PresentacionMatricula.resumenMedico(consultas.fichaMedica(m.estudianteId())),
				puedeCambiarSeccion(m), puedeEditarDocumentos(m), documentos);
	}

	/** Secciones del grado de la matrícula; la actual va marcada como seleccionada. */
	@Transactional(readOnly = true)
	public SeccionesDelGrado secciones(UUID matriculaId) {
		MatriculaDetalleFila m = buscar(matriculaId);
		return new SeccionesDelGrado(m.grado(), m.seccionId(),
				delGrado(m).stream().map(PresentacionMatricula::opcion).toList());
	}

	/** Mueve la matrícula a otra sección del mismo grado con cupo, liberando la vacante de la anterior. */
	@Transactional
	public void cambiarSeccion(UUID matriculaId, UUID destinoId, UUID usuarioId) {
		MatriculaDetalleFila m = buscar(matriculaId);
		if (!puedeCambiarSeccion(m)) {
			throw ExcepcionNegocio.conflicto("No se puede cambiar la sección de esta matrícula.");
		}
		if (destinoId.equals(m.seccionId())) {
			throw ExcepcionNegocio.invalido("La matrícula ya está en esa sección.");
		}
		escrituras.bloquearSeccionesDelGrado(m.anioEscolarId(), m.gradoCodigo());
		SeccionConVacantes destino = delGrado(m).stream().filter(s -> s.id().equals(destinoId)).findFirst()
				.orElseThrow(() -> ExcepcionNegocio.invalido("La sección elegida no es de " + m.grado() + "."));
		if (destino.alumnosOcupanVacante() >= destino.capacidadMaxima()) {
			throw ExcepcionNegocio.conflicto("La sección " + etiqueta(destino) + " no tiene vacantes.");
		}
		escrituras.cambiarSeccion(matriculaId, m.seccionId(), destinoId, usuarioId);
	}

	/** El archivo guardado de un documento, para verlo en el navegador. */
	@Transactional(readOnly = true)
	public ArchivoParaVer archivo(UUID matriculaId, UUID documentoId) {
		buscar(matriculaId);
		return consultas.archivoDelDocumento(matriculaId, documentoId)
				.map(a -> new ArchivoParaVer(a.nombre(), a.tipoMime(), a.contenido()))
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("Ese documento todavía no se subió."));
	}

	/** Sube o reemplaza un documento: queda EN_REVISION para que el colegio lo vuelva a revisar. */
	@Transactional
	public void subirDocumento(UUID matriculaId, UUID documentoId, ArchivoMatricula archivo, UUID usuarioId) {
		MatriculaDetalleFila m = buscar(matriculaId);
		if (!puedeEditarDocumentos(m)) {
			throw ExcepcionNegocio.conflicto("No se pueden modificar los documentos de esta matrícula.");
		}
		if (consultas.documentosRequeridosDe(m.periodoId()).stream().noneMatch(d -> d.id().equals(documentoId))) {
			throw ExcepcionNegocio.invalido("Ese documento no lo pide este periodo de matrícula.");
		}
		PresentacionMatricula.validarArchivo(archivo);
		escrituras.guardarDocumento(matriculaId, documentoId, archivo, usuarioId);
		// Si el colegio completa los documentos por el apoderado, la matrícula pasa sola a validación.
		int requeridos = consultas.documentosRequeridos(m.periodoId());
		if (m.estado().equals("SECCION_RESERVADA") && requeridos > 0
				&& consultas.documentosEntregados(matriculaId) >= requeridos) {
			escrituras.marcarEnValidacion(matriculaId);
		}
	}

	private static ArchivoGuardado archivoGuardado(DocumentoGuardadoFila f) {
		return f == null
				? null
				: new ArchivoGuardado(f.nombreArchivo(), f.tipoMime(), f.tamanoBytes(), f.estado(), f.observacion(),
						f.subidoEn());
	}

	private static int paso(String estado) {
		return switch (estado) {
			case "PENDIENTE_PAGO" -> 3;
			case "MATRICULADO" -> 4;
			default -> 2;
		};
	}

	private boolean periodoAbierto(MatriculaDetalleFila m) {
		return periodos.estadoEn(m.periodoId(), reloj.instant()) == EstadoVisiblePeriodoMatricula.ABIERTO;
	}

	private boolean vigente(MatriculaDetalleFila m) {
		return !m.estado().equals("RETIRADO") && !m.estado().equals("ANULADA");
	}

	/** Con el periodo cerrado solo se mueve a quienes ya están matriculados. */
	private boolean puedeCambiarSeccion(MatriculaDetalleFila m) {
		return vigente(m) && (periodoAbierto(m) || m.estado().equals("MATRICULADO"));
	}

	private boolean puedeEditarDocumentos(MatriculaDetalleFila m) {
		return vigente(m);
	}

	private List<SeccionConVacantes> delAnio(MatriculaDetalleFila m) {
		return secciones.listarSeccionesActivasConVacantes(m.anioEscolarId());
	}

	private List<SeccionConVacantes> delGrado(MatriculaDetalleFila m) {
		return delAnio(m).stream().filter(s -> s.gradoCodigo().equals(m.gradoCodigo()))
				.sorted(Comparator.comparing(SeccionConVacantes::nombre)).toList();
	}

	private MatriculaDetalleFila buscar(UUID matriculaId) {
		return consultas.matriculaDetalle(matriculaId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe la matrícula."));
	}
}
