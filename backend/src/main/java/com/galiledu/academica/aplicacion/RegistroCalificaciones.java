package com.galiledu.academica.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.CargaAcademica;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.CompetenciaConfigurada;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.MatriculaAcademica;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.PeriodoAcademico;
import com.galiledu.academica.aplicacion.puertos.RegistroAuditoriaAcademica;
import com.galiledu.academica.aplicacion.puertos.RegistroAuditoriaAcademica.EventoAuditoria;
import com.galiledu.academica.aplicacion.puertos.RepositorioCalificaciones;
import com.galiledu.academica.aplicacion.puertos.RepositorioItemsEvaluacion;
import com.galiledu.academica.dominio.Calificacion;
import com.galiledu.academica.dominio.CalificacionEstudiante;
import com.galiledu.academica.dominio.EscalaCalificacion;
import com.galiledu.academica.dominio.EvidenciaPedagogica;
import com.galiledu.academica.dominio.ItemEvaluacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Componente Registro de Calificaciones: registra o corrige la nota de un estudiante por ítem
 * mientras el periodo está abierto, y registra la auditoría en la misma transacción.
 */
@Service
public class RegistroCalificaciones {
	private static final ZoneId ZONA_INSTITUCION = ZoneId.of("America/Lima");
	private final RepositorioItemsEvaluacion items;
	private final RepositorioCalificaciones calificaciones;
	private final RegistroAuditoriaAcademica auditoria;
	private final ConsultaContextoAcademico contexto;
	private final VerificacionesAcademicas verificaciones;
	private final Clock reloj;

	public RegistroCalificaciones(RepositorioItemsEvaluacion items, RepositorioCalificaciones calificaciones,
		RegistroAuditoriaAcademica auditoria, ConsultaContextoAcademico contexto, Clock reloj) {
		this.items = Objects.requireNonNull(items);
		this.calificaciones = Objects.requireNonNull(calificaciones);
		this.auditoria = Objects.requireNonNull(auditoria);
		this.contexto = Objects.requireNonNull(contexto);
		this.verificaciones = new VerificacionesAcademicas(contexto);
		this.reloj = Objects.requireNonNull(reloj);
	}

	@Transactional
	public CalificacionEstudiante registrar(String nombreUsuario, UUID itemId, UUID matriculaId, Calificacion valor) {
		Objects.requireNonNull(valor, "La calificación es obligatoria");
		ActorAcademico actor = verificaciones.actor(nombreUsuario);
		ItemEvaluacion item = itemEditable(actor, itemId, valor.escala());
		exigirMatriculaDeLaCarga(matriculaId, verificaciones.cargaActiva(item.cargaAcademicaId()));
		Instant ahora = reloj.instant();
		Optional<CalificacionEstudiante> existente = calificaciones.buscarParaActualizar(item.id(), matriculaId);
		if (existente.isEmpty()) {
			CalificacionEstudiante nueva = CalificacionEstudiante.nueva(item.id(), matriculaId, valor, ahora);
			calificaciones.crear(nueva);
			auditar(actor, nueva, "REGISTRAR_CALIFICACION", null);
			return nueva;
		}
		CalificacionEstudiante anterior = existente.orElseThrow();
		CalificacionEstudiante corregida = anterior.corregir(valor, ahora);
		if (!calificaciones.actualizar(corregida)) {
			throw new IllegalStateException("No se pudo corregir la calificación");
		}
		auditar(actor, corregida, "CORREGIR_CALIFICACION", anterior.valor());
		return corregida;
	}

	@Transactional
	public void adjuntarEvidencia(String nombreUsuario, UUID calificacionId, EvidenciaPedagogica evidencia) {
		Objects.requireNonNull(evidencia, "La evidencia es obligatoria");
		ActorAcademico actor = verificaciones.actor(nombreUsuario);
		CalificacionEstudiante calificacion = calificaciones.buscar(
				Objects.requireNonNull(calificacionId, "La calificación es obligatoria"))
			.orElseThrow(() -> new NoSuchElementException("Calificación no encontrada"));
		itemEditable(actor, calificacion.itemEvaluacionId(), calificacion.valor().escala());
		calificaciones.guardarEvidencia(calificacion.id(), evidencia);
		auditoria.registrar(new EventoAuditoria(actor.usuarioId(), "EvidenciaPedagogica", calificacion.id(),
			"ADJUNTAR_EVIDENCIA", Map.of("calificacionId", calificacion.id().toString()), null,
			Map.of("nombreArchivo", evidencia.nombreArchivo())));
	}

	/** RF-133/134/136: docente de la carga, periodo abierto y escala configurada. */
	private ItemEvaluacion itemEditable(ActorAcademico actor, UUID itemId, EscalaCalificacion escala) {
		ItemEvaluacion item = items.buscar(Objects.requireNonNull(itemId, "El ítem es obligatorio"))
			.filter(ItemEvaluacion::activo)
			.orElseThrow(() -> new NoSuchElementException("Ítem de evaluación no encontrado"));
		CargaAcademica carga = verificaciones.cargaActiva(item.cargaAcademicaId());
		verificaciones.exigirDocenteDeCarga(actor, carga);
		PeriodoAcademico periodo = verificaciones.periodoDeCarga(item.periodoAcademicoId(), carga);
		if (!periodo.admiteNotasEn(LocalDateTime.ofInstant(reloj.instant(), ZONA_INSTITUCION))) {
			throw new SolicitudAcademicaInvalidaException("El periodo no admite registrar ni corregir notas");
		}
		CompetenciaConfigurada competencia = verificaciones.competenciaDeCarga(item.configuracionCompetenciaId(), carga);
		if (competencia.configuracion().escala() != escala) {
			throw new SolicitudAcademicaInvalidaException(
				"La competencia se califica en escala " + competencia.configuracion().escala());
		}
		return item;
	}

	private void exigirMatriculaDeLaCarga(UUID matriculaId, CargaAcademica carga) {
		MatriculaAcademica matricula = contexto.buscarMatricula(
				Objects.requireNonNull(matriculaId, "La matrícula es obligatoria"))
			.filter(MatriculaAcademica::vigente)
			.orElseThrow(() -> new NoSuchElementException("Matrícula vigente no encontrada"));
		if (!matricula.seccionId().equals(carga.seccionId()) || !matricula.anioId().equals(carga.anioId())) {
			throw new SolicitudAcademicaInvalidaException("El estudiante no pertenece a la sección de la carga");
		}
	}

	private void auditar(ActorAcademico actor, CalificacionEstudiante calificacion, String operacion,
		Calificacion anterior) {
		auditoria.registrar(new EventoAuditoria(actor.usuarioId(), "Calificacion", calificacion.id(), operacion,
			Map.of("itemEvaluacionId", calificacion.itemEvaluacionId().toString(),
				"matriculaId", calificacion.matriculaId().toString()),
			anterior == null ? null : valores(anterior), valores(calificacion.valor())));
	}

	private static Map<String, String> valores(Calificacion valor) {
		return Map.of("escala", valor.escala().name(), "valor", valor.escala() == EscalaCalificacion.VIGESIMAL
			? valor.vigesimal().stripTrailingZeros().toPlainString() : valor.cualitativa().name());
	}
}
