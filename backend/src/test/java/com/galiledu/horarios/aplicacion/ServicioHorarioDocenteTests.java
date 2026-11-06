package com.galiledu.horarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.AniosYSecciones.AnioLeido;
import com.galiledu.horarios.aplicacion.ClasesDelDocente.AnioConClases;
import com.galiledu.horarios.aplicacion.ClasesDelDocente.ClaseDelDocente;
import com.galiledu.horarios.aplicacion.DtosHorarioDocente.HorarioDocente;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.BloqueDeJornada;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.JornadaEstructura;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.RecesoDeJornada;

/** HU-49: el horario de dictado del docente, sin Spring ni BD. */
class ServicioHorarioDocenteTests {
	private static final UUID DOCENTE = UUID.randomUUID();
	private static final UUID ANIO_2026 = UUID.randomUUID();
	private static final UUID ANIO_2027 = UUID.randomUUID();
	private static final UUID EST_PRI = UUID.randomUUID();
	private static final UUID EST_SEC = UUID.randomUUID();
	private static final UUID EST_SEC_2 = UUID.randomUUID();

	private final ClasesDelDocente clases = mock(ClasesDelDocente.class);
	private final JornadaDeSeccion jornadas = mock(JornadaDeSeccion.class);
	private final AniosYSecciones anios = mock(AniosYSecciones.class);
	private final ServicioHorarioDocente servicio = new ServicioHorarioDocente(clases, jornadas, anios);

	private static ClaseDelDocente clase(String nivel, UUID estructura, DiaSemana dia, int bloque, String curso,
			String seccion) {
		return new ClaseDelDocente(UUID.randomUUID(), seccion, nivel, estructura, dia, bloque, UUID.randomUUID(),
				curso, UUID.randomUUID(), "Aula 1");
	}

	private void jornada(UUID estructura, String codigo, LocalTime entrada) {
		when(jornadas.deEstructura(estructura)).thenReturn(new JornadaEstructura(estructura, codigo, true,
				List.of(DiaSemana.LUN, DiaSemana.MAR),
				List.of(new BloqueDeJornada(1, entrada, entrada.plusMinutes(45))),
				List.of(new RecesoDeJornada(1, entrada.plusMinutes(45), entrada.plusMinutes(65)))));
	}

	@Test
	void agrupaLasClasesPorEstructuraConLaJornadaDeCadaUna() {
		var anio = new AnioConClases(ANIO_2027, 2027, "ACTIVO");
		when(clases.aniosDe(DOCENTE)).thenReturn(List.of(anio));
		when(clases.clasesDe(DOCENTE, ANIO_2027)).thenReturn(List.of(
				clase("SECUNDARIA", EST_SEC, DiaSemana.LUN, 1, "Álgebra", "1.° A"),
				clase("PRIMARIA", EST_PRI, DiaSemana.LUN, 2, "Aritmética", "3.° B"),
				clase("SECUNDARIA", EST_SEC, DiaSemana.MAR, 1, "Álgebra", "1.° B")));
		jornada(EST_PRI, "EH-PRI", LocalTime.of(8, 30));
		jornada(EST_SEC, "EH-SEC", LocalTime.of(8, 0));

		HorarioDocente h = servicio.horarioDelDocente(DOCENTE, null);

		assertThat(h.anio().id()).isEqualTo(ANIO_2027);
		assertThat(h.horarios()).hasSize(2);
		assertThat(h.horarios().get(1).nivel()).isEqualTo("SECUNDARIA");
		assertThat(h.horarios().get(1).titulo()).isEqualTo("Secundaria");
		assertThat(h.horarios().get(1).jornada().bloques().get(0).inicio()).isEqualTo("08:00");
		assertThat(h.horarios().get(1).jornada().bloques().get(0).fin()).isEqualTo("08:45");
		assertThat(h.horarios().get(1).jornada().recesos().get(0).despuesDeBloque()).isEqualTo(1);
		assertThat(h.horarios().get(1).jornada().dias()).containsExactly(DiaSemana.LUN, DiaSemana.MAR);
		assertThat(h.horarios().get(1).clases()).extracting(c -> c.seccion()).containsExactly("1.° A", "1.° B");
		assertThat(h.horarios().get(0).clases()).hasSize(1);
		assertThat(h.horarios().get(0).clases().get(0).ambiente()).isEqualTo("Aula 1");
	}

	@Test
	void primariaVaAntesQueSecundariaAunqueLaPrimeraClaseSeaDeSecundaria() {
		when(clases.aniosDe(DOCENTE)).thenReturn(List.of(new AnioConClases(ANIO_2027, 2027, "ACTIVO")));
		when(clases.clasesDe(DOCENTE, ANIO_2027)).thenReturn(List.of(
				clase("SECUNDARIA", EST_SEC, DiaSemana.LUN, 1, "Álgebra", "1.° A"),
				clase("PRIMARIA", EST_PRI, DiaSemana.LUN, 2, "Aritmética", "3.° B")));
		jornada(EST_PRI, "EH-PRI", LocalTime.of(8, 30));
		jornada(EST_SEC, "EH-SEC", LocalTime.of(8, 0));

		assertThat(servicio.horarioDelDocente(DOCENTE, null).horarios()).extracting(x -> x.nivel())
				.containsExactly("PRIMARIA", "SECUNDARIA");
	}

	@Test
	void conDosEstructurasDelMismoNivelElTituloLlevaElCodigo() {
		when(clases.aniosDe(DOCENTE)).thenReturn(List.of(new AnioConClases(ANIO_2027, 2027, "ACTIVO")));
		when(clases.clasesDe(DOCENTE, ANIO_2027)).thenReturn(List.of(
				clase("SECUNDARIA", EST_SEC_2, DiaSemana.LUN, 1, "Álgebra", "2.° A"),
				clase("SECUNDARIA", EST_SEC, DiaSemana.LUN, 2, "Álgebra", "1.° A"),
				clase("PRIMARIA", EST_PRI, DiaSemana.LUN, 3, "Aritmética", "3.° B")));
		jornada(EST_SEC, "EH-SEC-26", LocalTime.of(8, 0));
		jornada(EST_SEC_2, "EH-SEC-27", LocalTime.of(8, 15));
		jornada(EST_PRI, "EH-PRI", LocalTime.of(8, 30));

		assertThat(servicio.horarioDelDocente(DOCENTE, null).horarios()).extracting(x -> x.titulo())
				.containsExactly("Primaria", "Secundaria · EH-SEC-26", "Secundaria · EH-SEC-27");
	}

	@Test
	void sinAnioUsaElActivoSiTieneClasesAunqueHayaUnoMasReciente() {
		var planificado = new AnioConClases(UUID.randomUUID(), 2028, "PLANIFICADO");
		when(clases.aniosDe(DOCENTE)).thenReturn(List.of(planificado, new AnioConClases(ANIO_2027, 2027, "ACTIVO"),
				new AnioConClases(ANIO_2026, 2026, "CERRADO")));

		HorarioDocente h = servicio.horarioDelDocente(DOCENTE, null);

		assertThat(h.anio().id()).isEqualTo(ANIO_2027);
		verify(clases).clasesDe(DOCENTE, ANIO_2027);
	}

	@Test
	void sinAnioYSinActivoConClasesUsaElMasReciente() {
		var reciente = new AnioConClases(ANIO_2027, 2027, "CERRADO");
		when(clases.aniosDe(DOCENTE)).thenReturn(List.of(reciente, new AnioConClases(ANIO_2026, 2026, "CERRADO")));

		assertThat(servicio.horarioDelDocente(DOCENTE, null).anio().anio()).isEqualTo(2027);
	}

	@Test
	void sinClasesEnNingunAnioResponderAnioNullYHorariosVacio() {
		when(clases.aniosDe(DOCENTE)).thenReturn(List.of());

		HorarioDocente h = servicio.horarioDelDocente(DOCENTE, null);

		assertThat(h.anio()).isNull();
		assertThat(h.horarios()).isEmpty();
		verifyNoInteractions(jornadas);
	}

	@Test
	void unAnioInexistenteResponde404() {
		when(anios.anios()).thenReturn(List.of(new AnioLeido(ANIO_2027, 2027, "ACTIVO")));

		assertThatThrownBy(() -> servicio.horarioDelDocente(DOCENTE, UUID.randomUUID()))
				.isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
					assertThat(e.estado()).isEqualTo(HttpStatus.NOT_FOUND);
					assertThat(e.getMessage()).isEqualTo("El año escolar no existe.");
				});
	}

	@Test
	void unAnioSinClasesDevuelveHorariosVacioConLosDatosDelAnio() {
		when(anios.anios()).thenReturn(List.of(new AnioLeido(ANIO_2026, 2026, "CERRADO")));
		when(clases.clasesDe(DOCENTE, ANIO_2026)).thenReturn(List.of());

		HorarioDocente h = servicio.horarioDelDocente(DOCENTE, ANIO_2026);

		assertThat(h.anio().id()).isEqualTo(ANIO_2026);
		assertThat(h.anio().anio()).isEqualTo(2026);
		assertThat(h.anio().estado()).isEqualTo("CERRADO");
		assertThat(h.horarios()).isEmpty();
	}

	@Test
	void dosClasesEnLaMismaCeldaSeConservan() {
		when(clases.aniosDe(DOCENTE)).thenReturn(List.of(new AnioConClases(ANIO_2027, 2027, "ACTIVO")));
		when(clases.clasesDe(DOCENTE, ANIO_2027)).thenReturn(List.of(
				clase("SECUNDARIA", EST_SEC, DiaSemana.LUN, 1, "Álgebra", "1.° A"),
				clase("SECUNDARIA", EST_SEC, DiaSemana.LUN, 1, "Álgebra", "1.° B")));
		jornada(EST_SEC, "EH-SEC", LocalTime.of(8, 0));

		var cl = servicio.horarioDelDocente(DOCENTE, null).horarios().get(0).clases();

		assertThat(cl).extracting(c -> c.seccion()).containsExactly("1.° A", "1.° B");
		assertThat(cl).extracting(c -> c.numeroBloque()).containsOnly(1);
	}

	@Test
	void aniosDelDocenteDelegaEnElPuerto() {
		when(clases.aniosDe(DOCENTE)).thenReturn(List.of(new AnioConClases(ANIO_2027, 2027, "ACTIVO")));

		assertThat(servicio.aniosDelDocente(DOCENTE)).extracting(a -> a.anio()).containsExactly(2027);
	}
}
