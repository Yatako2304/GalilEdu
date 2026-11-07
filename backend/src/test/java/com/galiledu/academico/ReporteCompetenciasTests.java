package com.galiledu.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.galiledu.academico.aplicacion.ServicioCalificaciones;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.CompetenciaFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.DetalleReporteFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.EstudianteFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.ItemCompetenciaFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.ItemFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.PeriodoContexto;
import com.galiledu.compartido.web.ExcepcionNegocio;

class ReporteCompetenciasTests {

	private static final UUID DOCENTE = UUID.randomUUID();
	private static final UUID PLAN = UUID.randomUUID();
	private static final UUID SECCION = UUID.randomUUID();
	private static final UUID PERIODO = UUID.randomUUID();
	private static final UUID COMPETENCIA_1 = UUID.randomUUID();
	private static final UUID COMPETENCIA_2 = UUID.randomUUID();
	private static final UUID ITEM_1 = UUID.randomUUID();
	private static final UUID ITEM_2 = UUID.randomUUID();
	private static final UUID ITEM_3 = UUID.randomUUID();
	private static final UUID MATRICULA = UUID.randomUUID();
	private static final UUID SIN_NOTAS = UUID.randomUUID();
	private static final Instant INICIO = Instant.parse("2026-11-01T10:00:00Z");
	private static final List<UUID> COMPETENCIAS = List.of(COMPETENCIA_1, COMPETENCIA_2);
	private RepositorioCalificaciones repositorio;
	private ServicioCalificaciones servicio;

	@BeforeEach
	void preparar() {
		repositorio = mock(RepositorioCalificaciones.class);
		servicio = new ServicioCalificaciones(repositorio, Clock.systemUTC());
		when(repositorio.tieneAcceso(DOCENTE, PLAN, SECCION)).thenReturn(true);
		when(repositorio.competencias(PLAN)).thenReturn(List.of(new CompetenciaFila(COMPETENCIA_1, "Cantidad"),
				new CompetenciaFila(COMPETENCIA_2, "Regularidad")));
		when(repositorio.items(PLAN, SECCION, PERIODO)).thenReturn(List.of(
				new ItemFila(ITEM_1, "Práctica compartida", "Criterio", "PRACTICA", INICIO),
				new ItemFila(ITEM_2, "Examen", "Criterio", "EXAMEN", INICIO.plusSeconds(1)),
				new ItemFila(ITEM_3, "Proyecto", "Criterio", "PROYECTO", INICIO.plusSeconds(2))));
		when(repositorio.competenciasDeItems(List.of(ITEM_1, ITEM_2, ITEM_3))).thenReturn(List.of(
				new ItemCompetenciaFila(ITEM_1, COMPETENCIA_1, "Cantidad", BigDecimal.valueOf(40)),
				new ItemCompetenciaFila(ITEM_2, COMPETENCIA_1, "Cantidad", BigDecimal.valueOf(60)),
				new ItemCompetenciaFila(ITEM_1, COMPETENCIA_2, "Regularidad", BigDecimal.valueOf(20)),
				new ItemCompetenciaFila(ITEM_3, COMPETENCIA_2, "Regularidad", BigDecimal.valueOf(80))));
		when(repositorio.estudiantes(SECCION)).thenReturn(List.of(
				new EstudianteFila(MATRICULA, UUID.randomUUID(), "Ana Pérez"),
				new EstudianteFila(SIN_NOTAS, UUID.randomUUID(), "Luis Gómez")));
		configurarRegla("VIGESIMAL", "PROMEDIO_PONDERADO");
		notas("10", "20", "14", false);
	}

	@ParameterizedTest
	@CsvSource({"PROMEDIO_PONDERADO,16,13.2,14.6", "PROMEDIO_SIMPLE,15,12,14.67"})
	void aplicaLaReglaAnualPorCompetenciaYAlGeneralSinDuplicarItems(String regla, String primera, String segunda,
			String general) {
		configurarRegla("VIGESIMAL", regla);
		var reporte = servicio.reporte(DOCENTE, "COMPETENCIA", PLAN, SECCION, PERIODO, null, List.of(), true,
				COMPETENCIAS);

		assertThat(reporte.columnas()).hasSize(7);
		assertThat(reporte.columnas().get(0).competenciaId()).isEqualTo(COMPETENCIA_1);
		assertThat(reporte.columnas().get(3).competenciaId()).isEqualTo(COMPETENCIA_2);
		assertThat(reporte.columnas().get(6).promedio()).isTrue();
		assertThat(reporte.filas().get(0).valores())
				.containsEntry(COMPETENCIA_1 + ":resultado", primera)
				.containsEntry(COMPETENCIA_2 + ":resultado", segunda)
				.containsEntry("resultado_general", general)
				.containsEntry("resultado_general_estado", "Aprobado");
		assertThat(reporte.filas().get(1).valores()).containsEntry("resultado_general", "—")
				.containsEntry(COMPETENCIA_1 + ":resultado_estado", "Pendiente");
	}

	@ParameterizedTest
	@CsvSource({"NIVEL_MAS_FRECUENTE,A,A,B", "ULTIMO_NIVEL,B,B,B"})
	void aplicaLasReglasCualitativasAnuales(String regla, String primera, String segunda, String general) {
		configurarRegla("CUALITATIVA", regla);
		notas("A", "B", "B", true);
		var reporte = servicio.reporte(DOCENTE, "COMPETENCIA", PLAN, SECCION, PERIODO, null, List.of(), true,
				COMPETENCIAS);
		assertThat(reporte.filas().get(0).valores())
				.containsEntry(COMPETENCIA_1 + ":resultado", primera)
				.containsEntry(COMPETENCIA_2 + ":resultado", segunda)
				.containsEntry("resultado_general", general)
				.containsEntry("resultado_general_estado", "Desaprobado");
	}

	@Test
	void respetaLosItemsSeleccionadosYNormalizaSusPesos() {
		var reporte = servicio.reporte(DOCENTE, "COMPETENCIA", PLAN, SECCION, PERIODO, null,
				List.of(ITEM_3, ITEM_1), true, COMPETENCIAS);
		assertThat(reporte.columnas()).extracting(c -> c.clave()).containsExactly(
				COMPETENCIA_1 + ":" + ITEM_1, COMPETENCIA_1 + ":resultado", COMPETENCIA_2 + ":" + ITEM_3,
				COMPETENCIA_2 + ":" + ITEM_1, COMPETENCIA_2 + ":resultado", "resultado_general");
		assertThat(reporte.filas().get(0).valores()).containsEntry("resultado_general", "12.29");
	}

	@Test
	void cuentaLaNotaCeroYOmiteLosItemsSinCalificar() {
		notas("0", null, "20", false);
		var reporte = servicio.reporte(DOCENTE, "COMPETENCIA", PLAN, SECCION, PERIODO, null, List.of(), true,
				COMPETENCIAS);
		assertThat(reporte.filas().get(0).valores()).containsEntry(COMPETENCIA_1 + ":resultado", "0")
				.containsEntry("resultado_general", "11.43");
	}

	@Test
	void ocultaTodosLosPromediosCuandoSeDesactivaElInterruptor() {
		var reporte = servicio.reporte(DOCENTE, "COMPETENCIA", PLAN, SECCION, PERIODO, null, List.of(), false,
				COMPETENCIAS);
		assertThat(reporte.columnas()).hasSize(4).allMatch(c -> !c.promedio());
		assertThat(reporte.filas().get(0).valores()).doesNotContainKeys("resultado_general",
				COMPETENCIA_1 + ":resultado", COMPETENCIA_2 + ":resultado");
	}

	@ParameterizedTest
	@CsvSource({"PROMEDIO_SIMPLE,14.67", "NIVEL_MAS_FRECUENTE,B", "ULTIMO_NIVEL,B"})
	void losResultadosNoPonderadosAdmitenAsociacionesSinPeso(String regla, String esperado) {
		boolean cualitativa = !"PROMEDIO_SIMPLE".equals(regla);
		configurarRegla(cualitativa ? "CUALITATIVA" : "VIGESIMAL", regla);
		if (cualitativa)
			notas("A", "B", "B", true);
		List<DetalleReporteFila> sinPesos = repositorio.detallesReporte(PLAN, SECCION, PERIODO).stream()
				.map(d -> new DetalleReporteFila(d.matriculaId(), d.estudianteId(), d.estudiante(), d.itemId(),
						d.item(),
						d.competenciaId(), d.competencia(), null, d.notaNumerica(), d.nivelLogro(), d.evidenciaNombre(),
						d.ordenItem()))
				.toList();
		when(repositorio.detallesReporte(PLAN, SECCION, PERIODO)).thenReturn(sinPesos);
		var reporte = servicio.reporte(DOCENTE, "COMPETENCIA", PLAN, SECCION, PERIODO, null, List.of(), true,
				COMPETENCIAS);
		assertThat(reporte.filas().get(0).valores()).containsEntry("resultado_general", esperado);
	}

	@Test
	void admiteUnaSolaCompetenciaSinRepetirElPromedio() {
		var reporte = servicio.reporte(DOCENTE, "COMPETENCIA", PLAN, SECCION, PERIODO, null, List.of(), true,
				List.of(COMPETENCIA_1, COMPETENCIA_1));
		assertThat(reporte.columnas()).hasSize(3);
		assertThat(reporte.filas().get(0).valores()).containsEntry(COMPETENCIA_1 + ":resultado", "16")
				.doesNotContainKey("resultado_general");
	}

	@Test
	void rechazaCompetenciasAjenasAlCurso() {
		assertThatThrownBy(() -> servicio.reporte(DOCENTE, "COMPETENCIA", PLAN, SECCION, PERIODO, null,
				List.of(), true, List.of(UUID.randomUUID())))
				.isInstanceOf(ExcepcionNegocio.class).hasMessage("Una competencia no pertenece al plan del curso.");
	}

	@Test
	void rechazaItemsAjenosALasCompetenciasSeleccionadas() {
		assertThatThrownBy(() -> servicio.reporte(DOCENTE, "COMPETENCIA", PLAN, SECCION, PERIODO, null,
				List.of(ITEM_3), true, List.of(COMPETENCIA_1)))
				.isInstanceOf(ExcepcionNegocio.class)
				.hasMessage("Uno de los ítems no pertenece a las competencias seleccionadas.");
	}

	private void configurarRegla(String escala, String regla) {
		when(repositorio.periodo(PLAN, PERIODO)).thenReturn(Optional.of(new PeriodoContexto(PERIODO,
				UUID.randomUUID(), 2026, "ACTIVO", escala, regla, "ULTIMO_PERIODO", LocalDate.of(2026, 10, 19),
				LocalDate.of(2026, 12, 18), LocalDate.of(2026, 12, 23))));
	}

	private void notas(String primera, String segunda, String tercera, boolean cualitativa) {
		when(repositorio.detallesReporte(PLAN, SECCION, PERIODO)).thenReturn(List.of(
				detalle(ITEM_1, COMPETENCIA_1, 40, primera, cualitativa, 0),
				detalle(ITEM_2, COMPETENCIA_1, 60, segunda, cualitativa, 1),
				detalle(ITEM_1, COMPETENCIA_2, 20, primera, cualitativa, 0),
				detalle(ITEM_3, COMPETENCIA_2, 80, tercera, cualitativa, 2)));
	}

	private static DetalleReporteFila detalle(UUID item, UUID competencia, int peso, String nota,
			boolean cualitativa, int orden) {
		return new DetalleReporteFila(MATRICULA, UUID.randomUUID(), "Ana Pérez", item, "Ítem", competencia,
				"Competencia", BigDecimal.valueOf(peso), nota == null || cualitativa ? null : new BigDecimal(nota),
				cualitativa ? nota : null, null, INICIO.plusSeconds(orden));
	}
}
