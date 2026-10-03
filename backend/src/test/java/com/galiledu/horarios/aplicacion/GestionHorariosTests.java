package com.galiledu.horarios.aplicacion;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import com.galiledu.horarios.aplicacion.GestionHorarios.SolicitudAsignacion;
import com.galiledu.horarios.aplicacion.puertos.ConsultaReferenciasHorario.BloqueReferencia;
import com.galiledu.horarios.aplicacion.puertos.ConsultaReferenciasHorario.CargaAcademicaReferencia;
import com.galiledu.horarios.aplicacion.puertos.ConsultaReferenciasHorario.MatriculaReferencia;
import com.galiledu.horarios.aplicacion.puertos.ConsultaReferenciasHorario.PeriodoReferencia;
import com.galiledu.horarios.aplicacion.puertos.ConsultaReferenciasHorario.SeccionReferencia;
import com.galiledu.horarios.dominio.DiaSemana;
import com.galiledu.horarios.dominio.TipoCruceHorario;
import com.galiledu.horarios.dominio.ValidadorCrucesHorario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestionHorariosTests {
	private final HorariosEnMemoria datos = new HorariosEnMemoria();
	private final GestionHorarios gestion = new GestionHorarios(datos, datos,
		new VerificarDisponibilidadHorario(datos, new ValidadorCrucesHorario()));

	private final UUID anio = UUID.randomUUID();
	private final UUID seccion5A = UUID.randomUUID();
	private final UUID seccion6B = UUID.randomUUID();
	private final UUID docente1 = UUID.randomUUID();
	private final UUID docente2 = UUID.randomUUID();
	private final UUID carga5A = UUID.randomUUID();
	private final UUID carga6B = UUID.randomUUID();
	private final UUID bloque = UUID.randomUUID();
	private final UUID periodo = UUID.randomUUID();
	private final UUID aula = UUID.randomUUID();

	GestionHorariosTests() {
		datos.secciones.put(seccion5A, new SeccionReferencia(seccion5A, anio, "Quinto A", true));
		datos.secciones.put(seccion6B, new SeccionReferencia(seccion6B, anio, "Sexto B", true));
		datos.cargas.put(carga5A, new CargaAcademicaReferencia(carga5A, seccion5A, docente1, "Historia", true));
		datos.cargas.put(carga6B, new CargaAcademicaReferencia(carga6B, seccion6B, docente1, "Lengua", true));
		datos.bloques.put(bloque, new BloqueReferencia(bloque, anio,
			Set.of(DiaSemana.LUNES, DiaSemana.MARTES), true));
		datos.periodos.put(periodo, new PeriodoReferencia(periodo, anio, true));
		datos.espaciosActivos.add(aula);
	}

	@Test
	void creaBorradoresConVersionesConsecutivas() {
		var primero = gestion.crearBorrador(seccion5A);
		var segundo = gestion.crearBorrador(seccion5A);

		assertThat(primero.version()).isEqualTo(1);
		assertThat(segundo.version()).isEqualTo(2);
		assertThat(gestion.listarHorarios(seccion5A)).extracting(h -> h.version()).containsExactly(2, 1);
	}

	@Test
	void noCreaHorarioParaSeccionInexistenteOInactiva() {
		datos.secciones.put(seccion6B, new SeccionReferencia(seccion6B, anio, "Sexto B", false));

		assertThatThrownBy(() -> gestion.crearBorrador(seccion6B)).isInstanceOf(HorarioInvalidoException.class);
		assertThatThrownBy(() -> gestion.crearBorrador(UUID.randomUUID())).isInstanceOf(HorarioInvalidoException.class);
	}

	@Test
	void registraUnaAsignacionValidaSerializandoElBloque() {
		var horario = gestion.crearBorrador(seccion5A);

		var asignacion = gestion.registrarAsignacion(horario.id(), solicitud(carga5A, DiaSemana.LUNES, aula, null));

		assertThat(gestion.listarAsignaciones(horario.id())).extracting(a -> a.id()).containsExactly(asignacion.id());
		assertThat(datos.serializados).containsExactly(bloque + ":LUNES");
	}

	@Test
	void rechazaElCruceDeDocenteConOtraSeccionYNoGuardaNada() {
		var horario5A = gestion.crearBorrador(seccion5A);
		var horario6B = gestion.crearBorrador(seccion6B);
		gestion.registrarAsignacion(horario6B.id(), solicitud(carga6B, DiaSemana.LUNES, null, null));

		assertThatThrownBy(() -> gestion.registrarAsignacion(horario5A.id(),
			solicitud(carga5A, DiaSemana.LUNES, null, null)))
			.isInstanceOfSatisfying(CruceHorarioException.class, error ->
				assertThat(error.cruces()).singleElement().satisfies(cruce -> {
					assertThat(cruce.tipo()).isEqualTo(TipoCruceHorario.DOCENTE);
					assertThat(cruce.seccion()).isEqualTo("Sexto B");
					assertThat(cruce.curso()).isEqualTo("Lengua");
				}));
		assertThat(gestion.listarAsignaciones(horario5A.id())).isEmpty();
	}

	@Test
	void rechazaElCruceDeAulaConOtraSeccion() {
		var cargaOtroDocente = UUID.randomUUID();
		datos.cargas.put(cargaOtroDocente,
			new CargaAcademicaReferencia(cargaOtroDocente, seccion6B, docente2, "Lengua", true));
		var horario5A = gestion.crearBorrador(seccion5A);
		var horario6B = gestion.crearBorrador(seccion6B);
		gestion.registrarAsignacion(horario6B.id(), solicitud(cargaOtroDocente, DiaSemana.LUNES, aula, null));

		assertThatThrownBy(() -> gestion.registrarAsignacion(horario5A.id(),
			solicitud(carga5A, DiaSemana.LUNES, aula, null)))
			.isInstanceOfSatisfying(CruceHorarioException.class, error ->
				assertThat(error.cruces()).extracting(c -> c.tipo()).containsExactly(TipoCruceHorario.AULA));
	}

	@Test
	void rechazaOcuparDosVecesElMismoBloqueDeLaSeccion() {
		var horario = gestion.crearBorrador(seccion5A);
		gestion.registrarAsignacion(horario.id(), solicitud(carga5A, DiaSemana.LUNES, null, null));

		assertThatThrownBy(() -> gestion.registrarAsignacion(horario.id(),
			solicitud(carga5A, DiaSemana.LUNES, null, null)))
			.isInstanceOfSatisfying(CruceHorarioException.class, error ->
				assertThat(error.cruces()).extracting(c -> c.tipo()).containsExactly(TipoCruceHorario.SECCION));
	}

	@Test
	void verificarNoGuardaYAvisaDeLosCruces() {
		var horario5A = gestion.crearBorrador(seccion5A);
		var horario6B = gestion.crearBorrador(seccion6B);
		gestion.registrarAsignacion(horario6B.id(), solicitud(carga6B, DiaSemana.LUNES, null, null));

		var resultado = gestion.verificarAsignacion(horario5A.id(), solicitud(carga5A, DiaSemana.LUNES, null, null));

		assertThat(resultado.disponible()).isFalse();
		assertThat(gestion.verificarAsignacion(horario5A.id(), solicitud(carga5A, DiaSemana.MARTES, null, null))
			.disponible()).isTrue();
		assertThat(gestion.listarAsignaciones(horario5A.id())).isEmpty();
	}

	@Test
	void modificarUnaAsignacionNoLaCruzaConsigoMisma() {
		var horario = gestion.crearBorrador(seccion5A);
		var original = gestion.registrarAsignacion(horario.id(), solicitud(carga5A, DiaSemana.LUNES, aula, null));

		var cambiada = gestion.modificarAsignacion(original.id(), solicitud(carga5A, DiaSemana.MARTES, aula, null));

		assertThat(cambiada.id()).isEqualTo(original.id());
		assertThat(gestion.listarAsignaciones(horario.id())).singleElement()
			.satisfies(a -> assertThat(a.dia()).isEqualTo(DiaSemana.MARTES));
	}

	@Test
	void retirarUnaAsignacionLiberaElBloque() {
		var horario5A = gestion.crearBorrador(seccion5A);
		var horario6B = gestion.crearBorrador(seccion6B);
		var ocupada = gestion.registrarAsignacion(horario6B.id(), solicitud(carga6B, DiaSemana.LUNES, null, null));

		gestion.retirarAsignacion(ocupada.id());

		assertThat(gestion.registrarAsignacion(horario5A.id(), solicitud(carga5A, DiaSemana.LUNES, null, null)))
			.isNotNull();
		assertThatThrownBy(() -> gestion.retirarAsignacion(ocupada.id())).isInstanceOf(NoSuchElementException.class);
	}

	@Test
	void validaLoQueLasClavesForaneasNoGarantizan() {
		var horario = gestion.crearBorrador(seccion5A);

		assertInvalida(horario.id(), solicitud(carga6B, DiaSemana.LUNES, null, null), "otra sección");
		assertInvalida(horario.id(), solicitud(carga5A, DiaSemana.MIERCOLES, null, null), "no es lectivo");
		assertInvalida(horario.id(), solicitud(carga5A, DiaSemana.LUNES, UUID.randomUUID(), null), "espacio");
		assertInvalida(horario.id(), solicitud(UUID.randomUUID(), DiaSemana.LUNES, null, null), "carga académica");

		var otroAnio = UUID.randomUUID();
		datos.bloques.put(bloque, new BloqueReferencia(bloque, otroAnio, Set.of(DiaSemana.LUNES), true));
		assertInvalida(horario.id(), solicitud(carga5A, DiaSemana.LUNES, null, null), "otro año");
	}

	@Test
	void validaLosPeriodos() {
		var horario = gestion.crearBorrador(seccion5A);
		var inexistente = UUID.randomUUID();
		var ajeno = UUID.randomUUID();
		var inactivo = UUID.randomUUID();
		datos.periodos.put(ajeno, new PeriodoReferencia(ajeno, UUID.randomUUID(), true));
		datos.periodos.put(inactivo, new PeriodoReferencia(inactivo, anio, false));

		for (UUID invalido : new UUID[] {inexistente, ajeno, inactivo}) {
			assertThatThrownBy(() -> gestion.registrarAsignacion(horario.id(),
				new SolicitudAsignacion(carga5A, bloque, null, null, null, DiaSemana.LUNES,
					Set.of(periodo, invalido)))).isInstanceOf(HorarioInvalidoException.class);
		}
		assertThatThrownBy(() -> new SolicitudAsignacion(carga5A, bloque, null, null, null,
			DiaSemana.LUNES, Set.of())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void noSePuedeEditarUnHorarioDesactivado() {
		var horario = gestion.crearBorrador(seccion5A);
		gestion.desactivarHorario(horario.id());

		assertThatThrownBy(() -> gestion.registrarAsignacion(horario.id(),
			solicitud(carga5A, DiaSemana.LUNES, null, null))).isInstanceOf(NoSuchElementException.class);
		assertThatThrownBy(() -> gestion.desactivarHorario(horario.id())).isInstanceOf(NoSuchElementException.class);
	}

	@Test
	void subgruposDeUnaSeccionPuedenCompartirBloque() {
		var horario = gestion.crearBorrador(seccion5A);
		var grupo1 = gestion.crearSubgrupo(seccion5A, "Grupo 1");
		var grupo2 = gestion.crearSubgrupo(seccion5A, "Grupo 2");
		var cargaTaller = UUID.randomUUID();
		datos.cargas.put(cargaTaller, new CargaAcademicaReferencia(cargaTaller, seccion5A, docente2, "Taller", true));

		gestion.registrarAsignacion(horario.id(), solicitud(carga5A, DiaSemana.LUNES, null, grupo1.id()));
		gestion.registrarAsignacion(horario.id(), solicitud(cargaTaller, DiaSemana.LUNES, null, grupo2.id()));

		assertThat(gestion.listarAsignaciones(horario.id())).hasSize(2);
	}

	@Test
	void unSubgrupoAjenoOInactivoSeRechaza() {
		var horario = gestion.crearBorrador(seccion5A);
		var ajeno = gestion.crearSubgrupo(seccion6B, "Grupo 1");
		var inactivo = gestion.crearSubgrupo(seccion5A, "Grupo 2");
		gestion.desactivarSubgrupo(inactivo.id());

		assertInvalida(horario.id(), solicitud(carga5A, DiaSemana.LUNES, null, ajeno.id()), "otra sección");
		assertInvalida(horario.id(), solicitud(carga5A, DiaSemana.LUNES, null, inactivo.id()), "inactivo");
	}

	@Test
	void noRepiteElNombreDeSubgrupoNiDesactivaUnoEnUso() {
		var horario = gestion.crearBorrador(seccion5A);
		var grupo = gestion.crearSubgrupo(seccion5A, " Grupo 1 ");
		gestion.crearSubgrupo(seccion6B, "Grupo 1");

		assertThatThrownBy(() -> gestion.crearSubgrupo(seccion5A, "Grupo 1"))
			.isInstanceOf(HorarioInvalidoException.class);
		gestion.registrarAsignacion(horario.id(), solicitud(carga5A, DiaSemana.LUNES, null, grupo.id()));
		assertThatThrownBy(() -> gestion.desactivarSubgrupo(grupo.id()))
			.isInstanceOf(HorarioInvalidoException.class);
	}

	@Test
	void gestionaLosMiembrosDeUnSubgrupo() {
		var grupo = gestion.crearSubgrupo(seccion5A, "Grupo 1");
		var matricula = UUID.randomUUID();
		var deOtraSeccion = UUID.randomUUID();
		var inactiva = UUID.randomUUID();
		datos.matriculas.put(matricula, new MatriculaReferencia(matricula, seccion5A, true));
		datos.matriculas.put(deOtraSeccion, new MatriculaReferencia(deOtraSeccion, seccion6B, true));
		datos.matriculas.put(inactiva, new MatriculaReferencia(inactiva, seccion5A, false));

		gestion.agregarMiembro(grupo.id(), matricula);
		assertThat(gestion.listarMiembros(grupo.id())).containsExactly(matricula);
		assertThatThrownBy(() -> gestion.agregarMiembro(grupo.id(), deOtraSeccion))
			.isInstanceOf(HorarioInvalidoException.class);
		assertThatThrownBy(() -> gestion.agregarMiembro(grupo.id(), inactiva))
			.isInstanceOf(HorarioInvalidoException.class);
		assertThatThrownBy(() -> gestion.agregarMiembro(grupo.id(), UUID.randomUUID()))
			.isInstanceOf(HorarioInvalidoException.class);

		gestion.retirarMiembro(grupo.id(), matricula);
		assertThat(gestion.listarMiembros(grupo.id())).isEmpty();
		assertThatThrownBy(() -> gestion.retirarMiembro(grupo.id(), matricula))
			.isInstanceOf(NoSuchElementException.class);
	}

	private SolicitudAsignacion solicitud(UUID carga, DiaSemana dia, UUID espacio, UUID subgrupo) {
		return new SolicitudAsignacion(carga, bloque, espacio, subgrupo, "TEORIA", dia, Set.of(periodo));
	}

	private void assertInvalida(UUID horarioId, SolicitudAsignacion solicitud, String fragmento) {
		assertThatThrownBy(() -> gestion.registrarAsignacion(horarioId, solicitud))
			.isInstanceOf(HorarioInvalidoException.class).hasMessageContaining(fragmento);
	}
}
