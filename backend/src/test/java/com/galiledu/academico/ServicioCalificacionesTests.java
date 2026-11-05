package com.galiledu.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.galiledu.academico.aplicacion.DtosCalificaciones.RegistroCalificacion;
import com.galiledu.academico.aplicacion.DtosCalificaciones.SolicitudCalificaciones;
import com.galiledu.academico.aplicacion.DtosCalificaciones.SolicitudCompetenciaItem;
import com.galiledu.academico.aplicacion.DtosCalificaciones.SolicitudItem;
import com.galiledu.academico.aplicacion.ServicioCalificaciones;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.DetalleReporteFila;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.ItemContexto;
import com.galiledu.academico.infraestructura.RepositorioCalificaciones.PeriodoContexto;
import com.galiledu.compartido.web.ExcepcionNegocio;

class ServicioCalificacionesTests {
	private static final UUID DOCENTE = UUID.fromString("10000000-0000-0000-0000-000000000004");
	private static final UUID PLAN = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID SECCION = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID PERIODO = UUID.fromString("40000000-0000-0000-0000-000000000004");
	private static final UUID ITEM = UUID.fromString("50000000-0000-0000-0000-000000000001");
	private static final UUID MATRICULA = UUID.fromString("60000000-0000-0000-0000-000000000001");
	private static final UUID COMPETENCIA = UUID.fromString("70000000-0000-0000-0000-000000000001");

	private RepositorioCalificaciones repositorio;
	private ServicioCalificaciones servicio;

	@BeforeEach
	void configurar() {
		repositorio = mock(RepositorioCalificaciones.class);
		Clock reloj = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneId.of("America/Lima"));
		servicio = new ServicioCalificaciones(repositorio, reloj);
		when(repositorio.tieneAcceso(DOCENTE, PLAN, SECCION)).thenReturn(true);
	}

	@Test
	void noPermiteModificarUnItemFueraDelPlazoDeNotas() {
		when(repositorio.item(ITEM)).thenReturn(Optional.of(item("VIGESIMAL", LocalDate.of(2026, 10, 31))));
		var solicitud = new SolicitudItem("Examen", "Resuelve problemas", "EXAMEN",
				List.of(new SolicitudCompetenciaItem(COMPETENCIA, BigDecimal.valueOf(100))));

		assertThatThrownBy(() -> servicio.editarItem(DOCENTE, ITEM, solicitud))
				.isInstanceOf(ExcepcionNegocio.class)
				.hasMessage("El periodo no está habilitado para registrar calificaciones.");
	}

	@Test
	void guardaUnaNotaVigesimalDelEstudianteDeLaSeccion() {
		when(repositorio.item(ITEM)).thenReturn(Optional.of(item("VIGESIMAL", LocalDate.of(2026, 12, 23))));
		when(repositorio.matriculaPertenece(MATRICULA, SECCION)).thenReturn(true);
		when(repositorio.calificaciones(ITEM, SECCION)).thenReturn(List.of());

		servicio.guardarCalificaciones(DOCENTE, ITEM,
				new SolicitudCalificaciones(
						List.of(new RegistroCalificacion(MATRICULA, BigDecimal.valueOf(18), null))));

		verify(repositorio).guardarCalificacion(ITEM, MATRICULA, BigDecimal.valueOf(18), null, DOCENTE);
	}

	@Test
	void laEscalaVigesimalRechazaUnNivelCualitativo() {
		when(repositorio.item(ITEM)).thenReturn(Optional.of(item("VIGESIMAL", LocalDate.of(2026, 12, 23))));
		when(repositorio.matriculaPertenece(MATRICULA, SECCION)).thenReturn(true);

		assertThatThrownBy(() -> servicio.guardarCalificaciones(DOCENTE, ITEM,
				new SolicitudCalificaciones(List.of(new RegistroCalificacion(MATRICULA, null, "AD")))))
				.isInstanceOf(ExcepcionNegocio.class)
				.hasMessage("La escala vigesimal requiere una calificación numérica de 0 a 20.");
	}

	@Test
	void calculaElResultadoPonderadoDeUnaCompetencia() {
		when(repositorio.periodo(PLAN, PERIODO)).thenReturn(Optional.of(new PeriodoContexto(PERIODO, UUID.randomUUID(),
				2026, "ACTIVO", "VIGESIMAL", "PROMEDIO_PONDERADO", "PROMEDIO_PERIODOS",
				LocalDate.of(2026, 10, 19), LocalDate.of(2026, 12, 18), LocalDate.of(2026, 12, 23))));
		UUID item2 = UUID.randomUUID();
		when(repositorio.detallesReporte(PLAN, SECCION, PERIODO)).thenReturn(List.of(
				new DetalleReporteFila(MATRICULA, UUID.randomUUID(), "Ana Pérez", ITEM, "Práctica", COMPETENCIA,
						"Resuelve problemas", BigDecimal.valueOf(40), BigDecimal.valueOf(10), null, null,
						Instant.parse("2026-11-01T10:00:00Z")),
				new DetalleReporteFila(MATRICULA, UUID.randomUUID(), "Ana Pérez", item2, "Examen", COMPETENCIA,
						"Resuelve problemas", BigDecimal.valueOf(60), BigDecimal.valueOf(20), null, null,
						Instant.parse("2026-11-02T10:00:00Z"))));

		var reporte = servicio.reporte(DOCENTE, "COMPETENCIA", PLAN, SECCION, PERIODO, COMPETENCIA);

		assertThat(reporte.filas()).hasSize(1);
		assertThat(reporte.filas().get(0).valores()).containsEntry("resultado", "16");
	}

	@Test
	void ocultaCursosQueNoPertenecenAlDocente() {
		when(repositorio.tieneAcceso(DOCENTE, PLAN, SECCION)).thenReturn(false);

		assertThatThrownBy(() -> servicio.competencias(DOCENTE, PLAN, SECCION))
				.isInstanceOf(ExcepcionNegocio.class).hasMessage("El curso no está asignado al docente.");
		verify(repositorio).tieneAcceso(DOCENTE, PLAN, SECCION);
	}

	private static ItemContexto item(String escala, LocalDate limite) {
		return new ItemContexto(ITEM, PLAN, SECCION, PERIODO, UUID.randomUUID(), 2026, "ACTIVO", escala,
				"PROMEDIO_PONDERADO", "PROMEDIO_PERIODOS", LocalDate.of(2026, 10, 19),
				LocalDate.of(2026, 12, 18), limite);
	}
}
