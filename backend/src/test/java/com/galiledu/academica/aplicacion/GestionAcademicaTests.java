package com.galiledu.academica.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import com.galiledu.academica.aplicacion.CriteriosEvaluacion.DatosItem;
import com.galiledu.academica.dominio.Calificacion;
import com.galiledu.academica.dominio.ConfiguracionCompetencia;
import com.galiledu.academica.dominio.EscalaCalificacion;
import com.galiledu.academica.dominio.EstadoPeriodo;
import com.galiledu.academica.dominio.EvidenciaPedagogica;
import com.galiledu.academica.dominio.ItemEvaluacion;
import com.galiledu.academica.dominio.NivelCualitativo;
import com.galiledu.academica.dominio.RangoConversion;
import com.galiledu.academica.dominio.ReglaAgregacionItems;
import com.galiledu.academica.dominio.ReglaConsolidacionPeriodos;
import com.galiledu.academica.dominio.TablaConversion;
import com.galiledu.academica.dominio.TipoItemEvaluacion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestionAcademicaTests {
	private static final Instant AHORA = Instant.parse("2026-05-15T15:00:00Z");
	private final AcademicoEnMemoria memoria = new AcademicoEnMemoria();
	private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
	private final CriteriosEvaluacion criterios = new CriteriosEvaluacion(memoria, memoria);
	private final RegistroCalificaciones registro = new RegistroCalificaciones(memoria,
		memoria.repositorioCalificaciones, memoria, memoria, reloj);
	private final ConsolidacionNotas consolidacion = new ConsolidacionNotas(memoria.repositorioCalificaciones,
		memoria, memoria, reloj);
	private final ConsultaNotas consulta = new ConsultaNotas(memoria, memoria.repositorioCalificaciones,
		memoria, memoria);

	@Test
	void soloElDocenteDeLaCargaRegistraItemsDeSuCompetenciaYPeriodo() {
		ItemEvaluacion item = item("Examen 1", memoria.periodo1);
		assertThat(criterios.listarItems("docente", memoria.carga, memoria.periodo1)).containsExactly(item);

		assertThatThrownBy(() -> criterios.registrarItem("otro", memoria.carga, memoria.competencia,
			memoria.periodo1, datos("Práctica")))
			.isInstanceOf(AccesoAcademicoDenegadoException.class);
		assertThatThrownBy(() -> criterios.registrarItem("docente", memoria.carga, UUID.randomUUID(),
			memoria.periodo1, datos("Práctica")))
			.isInstanceOf(NoSuchElementException.class);
		memoria.periodo(memoria.periodo1, 1, EstadoPeriodo.CERRADO, LocalDateTime.of(2026, 6, 30, 23, 59));
		assertThatThrownBy(() -> criterios.registrarItem("docente", memoria.carga, memoria.competencia,
			memoria.periodo1, datos("Práctica")))
			.isInstanceOf(SolicitudAcademicaInvalidaException.class);
	}

	@Test
	void ponderadoExigePesoYNoSeRetiraUnItemConNotas() {
		memoria.configuracion = new ConfiguracionCompetencia(EscalaCalificacion.VIGESIMAL,
			ReglaAgregacionItems.PROMEDIO_PONDERADO, ReglaConsolidacionPeriodos.ULTIMO_PERIODO);
		assertThatThrownBy(() -> item("Sin peso", memoria.periodo1))
			.isInstanceOf(SolicitudAcademicaInvalidaException.class);
		ItemEvaluacion item = criterios.registrarItem("docente", memoria.carga, memoria.competencia,
			memoria.periodo1, new DatosItem("Con peso", TipoItemEvaluacion.EXAMEN, null, new BigDecimal("2")));
		registro.registrar("docente", item.id(), memoria.matricula1, vigesimal("15"));
		assertThatThrownBy(() -> criterios.desactivarItem("docente", item.id()))
			.isInstanceOf(SolicitudAcademicaInvalidaException.class);
	}

	@Test
	void registraYCorrigeConAuditoriaDelValorAnteriorYNuevo() {
		ItemEvaluacion item = item("Examen 1", memoria.periodo1);
		var primera = registro.registrar("docente", item.id(), memoria.matricula1, vigesimal("12"));
		var corregida = registro.registrar("docente", item.id(), memoria.matricula1, vigesimal("14.5"));

		assertThat(corregida.id()).isEqualTo(primera.id());
		assertThat(corregida.valor().vigesimal()).isEqualByComparingTo("14.5");
		assertThat(corregida.fechaModificacion()).isEqualTo(AHORA);
		assertThat(memoria.eventos).hasSize(2);
		var correccion = memoria.eventos.get(1);
		assertThat(correccion.operacion()).isEqualTo("CORREGIR_CALIFICACION");
		assertThat(correccion.usuarioResponsableId()).isEqualTo(memoria.actores.get("docente").usuarioId());
		assertThat(correccion.valorAnterior()).containsEntry("valor", "12");
		assertThat(correccion.valorNuevo()).containsEntry("valor", "14.5");

		registro.adjuntarEvidencia("docente", corregida.id(),
			new EvidenciaPedagogica("examen.pdf", "evidencias/examen.pdf", "application/pdf", 2048L));
		assertThat(memoria.evidencias).containsKey(corregida.id());
	}

	@Test
	void rechazaEscalaDistintaPeriodoNoAbiertoOMatriculaAjena() {
		ItemEvaluacion item = item("Examen 1", memoria.periodo1);
		assertThatThrownBy(() -> registro.registrar("docente", item.id(), memoria.matricula1,
			Calificacion.cualitativa(NivelCualitativo.A)))
			.isInstanceOf(SolicitudAcademicaInvalidaException.class);
		assertThatThrownBy(() -> registro.registrar("otro", item.id(), memoria.matricula1, vigesimal("15")))
			.isInstanceOf(AccesoAcademicoDenegadoException.class);
		assertThatThrownBy(() -> registro.registrar("docente", item.id(), UUID.randomUUID(), vigesimal("15")))
			.isInstanceOf(NoSuchElementException.class);

		memoria.periodo(memoria.periodo1, 1, EstadoPeriodo.EN_CURSO, LocalDateTime.of(2026, 5, 1, 0, 0));
		assertThatThrownBy(() -> registro.registrar("docente", item.id(), memoria.matricula1, vigesimal("15")))
			.isInstanceOf(SolicitudAcademicaInvalidaException.class);
		assertThat(memoria.calificaciones).isEmpty();
		assertThat(memoria.eventos).isEmpty();
	}

	@Test
	void consolidaPeriodoYRecalculaFinalConEquivalente() {
		memoria.tabla = new TablaConversion(List.of(
			new RangoConversion(new BigDecimal("18"), new BigDecimal("20"), NivelCualitativo.AD),
			new RangoConversion(new BigDecimal("14"), new BigDecimal("17.99"), NivelCualitativo.A),
			new RangoConversion(new BigDecimal("11"), new BigDecimal("13.99"), NivelCualitativo.B),
			new RangoConversion(new BigDecimal("0"), new BigDecimal("10.99"), NivelCualitativo.C)));
		ItemEvaluacion examen = item("Examen 1", memoria.periodo1);
		ItemEvaluacion practica = item("Práctica 1", memoria.periodo1);
		registro.registrar("docente", examen.id(), memoria.matricula1, vigesimal("16"));
		registro.registrar("docente", practica.id(), memoria.matricula1, vigesimal("19"));

		var resultado = consolidacion.consolidarPeriodo("docente", memoria.carga, memoria.competencia, memoria.periodo1);

		assertThat(resultado.calculadas()).isEqualTo(1);
		assertThat(resultado.sinNotas()).containsExactly(memoria.matricula2);
		var notaFinal = memoria.listarFinales(memoria.matricula1).getFirst().nota();
		assertThat(notaFinal.valor().vigesimal()).isEqualByComparingTo("17.50");
		assertThat(notaFinal.equivalenteCualitativo()).isEqualTo(NivelCualitativo.A);
		assertThat(memoria.listarPeriodos(memoria.matricula1, memoria.competencia)).hasSize(1);

		assertThatThrownBy(() -> consolidacion.consolidarPeriodo("otro", memoria.carga, memoria.competencia,
			memoria.periodo1)).isInstanceOf(AccesoAcademicoDenegadoException.class);
		assertThat(consolidacion.consolidarPeriodo("admin", memoria.carga, memoria.competencia, memoria.periodo1)
			.calculadas()).isEqualTo(1);
	}

	@Test
	void reportaMatriculasCuyaReglaNoProduceResultado() {
		memoria.configuracion = new ConfiguracionCompetencia(EscalaCalificacion.VIGESIMAL,
			ReglaAgregacionItems.MODA, ReglaConsolidacionPeriodos.ULTIMO_PERIODO);
		registro.registrar("docente", item("A", memoria.periodo1).id(), memoria.matricula1, vigesimal("12"));
		registro.registrar("docente", item("B", memoria.periodo1).id(), memoria.matricula1, vigesimal("15"));

		var resultado = consolidacion.consolidarPeriodo("docente", memoria.carga, memoria.competencia, memoria.periodo1);

		assertThat(resultado.calculadas()).isZero();
		assertThat(resultado.pendientes()).containsOnlyKeys(memoria.matricula1);
		assertThat(memoria.notasFinales).isEmpty();
	}

	@Test
	void consultaSoloPorPersonasRelacionadasConLaMatriculaOLaCarga() {
		ItemEvaluacion item = item("Examen 1", memoria.periodo1);
		registro.registrar("docente", item.id(), memoria.matricula1, vigesimal("13"));
		UUID apoderado = UUID.randomUUID();
		memoria.actores.put("apoderado", new ActorAcademico(UUID.randomUUID(), apoderado, Set.of("APODERADO")));
		memoria.lectoresMatricula.put(memoria.matricula1, Set.of(apoderado));

		assertThat(consulta.notasDeMatricula("apoderado", memoria.matricula1).calificaciones()).hasSize(1);
		assertThatThrownBy(() -> consulta.notasDeMatricula("apoderado", memoria.matricula2))
			.isInstanceOf(AccesoAcademicoDenegadoException.class);
		assertThat(consulta.calificacionesDeItem("docente", item.id())).hasSize(1);
		assertThatThrownBy(() -> consulta.calificacionesDeItem("otro", item.id()))
			.isInstanceOf(AccesoAcademicoDenegadoException.class);
		memoria.coordinadores.add(memoria.actores.get("otro").personaId());
		assertThat(consulta.calificacionesDeItem("otro", item.id())).hasSize(1);
		assertThatThrownBy(() -> consulta.notasDeMatricula("desconocido", memoria.matricula1))
			.isInstanceOf(AccesoAcademicoDenegadoException.class);
	}

	private ItemEvaluacion item(String nombre, UUID periodo) {
		return criterios.registrarItem("docente", memoria.carga, memoria.competencia, periodo, datos(nombre));
	}

	private static DatosItem datos(String nombre) {
		return new DatosItem(nombre, TipoItemEvaluacion.EXAMEN, LocalDate.of(2026, 5, 10), null);
	}

	private static Calificacion vigesimal(String valor) {
		return Calificacion.vigesimal(new BigDecimal(valor));
	}
}
