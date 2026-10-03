package com.galiledu.academica.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.CargaAcademica;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.CompetenciaConfigurada;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.PeriodoAcademico;
import com.galiledu.academica.aplicacion.puertos.RepositorioItemsEvaluacion;
import com.galiledu.academica.dominio.EstadoPeriodo;
import com.galiledu.academica.dominio.ItemEvaluacion;
import com.galiledu.academica.dominio.ReglaAgregacionItems;
import com.galiledu.academica.dominio.TipoItemEvaluacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Componente Criterios de Evaluación: ítems de una carga asociados a una competencia y periodo. */
@Service
public class CriteriosEvaluacion {
	private final RepositorioItemsEvaluacion items;
	private final VerificacionesAcademicas verificaciones;

	public CriteriosEvaluacion(RepositorioItemsEvaluacion items, ConsultaContextoAcademico contexto) {
		this.items = Objects.requireNonNull(items);
		this.verificaciones = new VerificacionesAcademicas(contexto);
	}

	@Transactional
	public ItemEvaluacion registrarItem(String nombreUsuario, UUID cargaAcademicaId,
		UUID configuracionCompetenciaId, UUID periodoAcademicoId, DatosItem datos) {
		Objects.requireNonNull(datos, "Los datos del ítem son obligatorios");
		ActorAcademico actor = verificaciones.actor(nombreUsuario);
		CargaAcademica carga = verificaciones.cargaActiva(cargaAcademicaId);
		verificaciones.exigirDocenteDeCarga(actor, carga);
		CompetenciaConfigurada competencia = verificaciones.competenciaDeCarga(configuracionCompetenciaId, carga);
		exigirPeriodoAbierto(verificaciones.periodoDeCarga(periodoAcademicoId, carga));
		exigirPesoSiPondera(competencia, datos.peso());
		ItemEvaluacion item = ItemEvaluacion.nuevo(carga.id(), competencia.id(), periodoAcademicoId,
			datos.nombre(), datos.tipo(), datos.fechaEvaluacion(), datos.peso());
		items.crear(item);
		return item;
	}

	@Transactional
	public ItemEvaluacion modificarItem(String nombreUsuario, UUID itemId, DatosItem datos) {
		Objects.requireNonNull(datos, "Los datos del ítem son obligatorios");
		ActorAcademico actor = verificaciones.actor(nombreUsuario);
		ItemEvaluacion item = itemActivo(itemId);
		CargaAcademica carga = verificaciones.cargaActiva(item.cargaAcademicaId());
		verificaciones.exigirDocenteDeCarga(actor, carga);
		CompetenciaConfigurada competencia = verificaciones.competenciaDeCarga(
			item.configuracionCompetenciaId(), carga);
		exigirPeriodoAbierto(verificaciones.periodoDeCarga(item.periodoAcademicoId(), carga));
		exigirPesoSiPondera(competencia, datos.peso());
		ItemEvaluacion modificado = item.modificar(datos.nombre(), datos.tipo(), datos.fechaEvaluacion(),
			datos.peso());
		if (!items.actualizar(modificado)) throw new NoSuchElementException("Ítem de evaluación no encontrado");
		return modificado;
	}

	@Transactional
	public void desactivarItem(String nombreUsuario, UUID itemId) {
		ActorAcademico actor = verificaciones.actor(nombreUsuario);
		ItemEvaluacion item = itemActivo(itemId);
		CargaAcademica carga = verificaciones.cargaActiva(item.cargaAcademicaId());
		verificaciones.exigirDocenteDeCarga(actor, carga);
		exigirPeriodoAbierto(verificaciones.periodoDeCarga(item.periodoAcademicoId(), carga));
		if (items.tieneCalificaciones(item.id())) {
			throw new SolicitudAcademicaInvalidaException("No se puede retirar un ítem que ya tiene calificaciones");
		}
		if (!items.actualizar(item.desactivar())) throw new NoSuchElementException("Ítem de evaluación no encontrado");
	}

	@Transactional(readOnly = true)
	public List<ItemEvaluacion> listarItems(String nombreUsuario, UUID cargaAcademicaId, UUID periodoAcademicoId) {
		ActorAcademico actor = verificaciones.actor(nombreUsuario);
		CargaAcademica carga = verificaciones.cargaActiva(cargaAcademicaId);
		verificaciones.exigirLecturaDeCarga(actor, carga);
		if (periodoAcademicoId != null) verificaciones.periodoDeCarga(periodoAcademicoId, carga);
		return items.listarActivos(carga.id(), periodoAcademicoId);
	}

	private ItemEvaluacion itemActivo(UUID itemId) {
		return items.buscar(Objects.requireNonNull(itemId, "El ítem es obligatorio"))
			.filter(ItemEvaluacion::activo)
			.orElseThrow(() -> new NoSuchElementException("Ítem de evaluación no encontrado"));
	}

	private static void exigirPeriodoAbierto(PeriodoAcademico periodo) {
		if (periodo.estado() == EstadoPeriodo.CERRADO) {
			throw new SolicitudAcademicaInvalidaException("El periodo académico está cerrado");
		}
	}

	private static void exigirPesoSiPondera(CompetenciaConfigurada competencia, BigDecimal peso) {
		if (competencia.configuracion().reglaAgregacion() == ReglaAgregacionItems.PROMEDIO_PONDERADO && peso == null) {
			throw new SolicitudAcademicaInvalidaException("La competencia usa promedio ponderado: el peso es obligatorio");
		}
	}

	public record DatosItem(String nombre, TipoItemEvaluacion tipo, LocalDate fechaEvaluacion, BigDecimal peso) {}
}
