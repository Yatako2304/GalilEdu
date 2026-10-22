package com.galiledu.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.galiledu.compartido.RelojConfig;

class PeriodoMatriculaTests {
	private static final UUID ANIO = UUID.randomUUID();
	private static final LocalDateTime INICIO = LocalDateTime.of(2026, 12, 1, 10, 0);
	private static final LocalDateTime CIERRE = LocalDateTime.of(2027, 2, 15, 10, 0);
	/** Hoy es el 22/10/2026 a las 09:00 en Lima: el periodo aún no empieza. */
	private static final Instant AHORA = instante(LocalDateTime.of(2026, 10, 22, 9, 0));

	private static Instant instante(LocalDateTime local) {
		return local.atZone(RelojConfig.LIMA).toInstant();
	}

	private static DatosDocumento doc(String nombre) {
		return new DatosDocumento(null, nombre, null);
	}

	private static PeriodoMatricula periodo(DatosDocumento... documentos) {
		PeriodoMatricula p = PeriodoMatricula.habilitar(ANIO, INICIO, CIERRE, List.of(documentos), AHORA);
		p.getDocumentos().forEach(d -> ReflectionTestUtils.setField(d, "id", UUID.randomUUID()));
		return p;
	}

	private static void rechazaDatos(String mensaje, ThrowingCallable accion) {
		assertThatThrownBy(accion).isInstanceOf(IllegalArgumentException.class).hasMessage(mensaje);
	}

	private static void rechazaEstado(String mensaje, ThrowingCallable accion) {
		assertThatThrownBy(accion).isInstanceOf(IllegalStateException.class).hasMessage(mensaje);
	}

	// --- Habilitar ---

	@Test
	void guardarLaConfiguracionDejaElPeriodoHabilitadoConLosFlagsExplicitos() {
		PeriodoMatricula p = periodo(doc("Ficha de matrícula"), doc("DNI del estudiante"));

		assertThat(p.getEstado()).isEqualTo(EstadoPeriodoMatricula.HABILITADO);
		assertThat(p.getAnioEscolarId()).isEqualTo(ANIO);
		assertThat(p.isBloquearPorMorosidad()).isTrue();
		assertThat(p.isPermitirExtemporanea()).isTrue();
		assertThat(p.getInicio()).isEqualTo(instante(INICIO));
		assertThat(p.getCierre()).isEqualTo(instante(CIERRE));
		assertThat(PeriodoMatricula.aHoraLocal(p.getInicio())).isEqualTo(INICIO);
		assertThat(p.getDocumentos()).extracting(DocumentoRequerido::getNombre).containsExactly("Ficha de matrícula",
				"DNI del estudiante");
		assertThat(p.getDocumentos()).extracting(DocumentoRequerido::getOrden).containsExactly(1, 2);
	}

	@Test
	void lasFechasSeGuardanEnHoraDeLima() {
		// 10:00 en Lima (UTC-5) es 15:00 UTC
		assertThat(instante(INICIO)).isEqualTo(Instant.parse("2026-12-01T15:00:00Z"));
	}

	@Test
	void unPeriodoSinDocumentosEsValido() {
		assertThat(periodo().getDocumentos()).isEmpty();
	}

	// --- Estado calculado ---

	@Test
	void esProgramadoAntesDelInicio() {
		assertThat(periodo().estadoEn(instante(INICIO).minusSeconds(1)))
				.isEqualTo(EstadoVisiblePeriodoMatricula.PROGRAMADO);
		assertThat(periodo().estadoEn(AHORA)).isEqualTo(EstadoVisiblePeriodoMatricula.PROGRAMADO);
	}

	@Test
	void esAbiertoDesdeElInicioHastaElCierreAmbosIncluidos() {
		PeriodoMatricula p = periodo();

		assertThat(p.estadoEn(instante(INICIO))).isEqualTo(EstadoVisiblePeriodoMatricula.ABIERTO);
		assertThat(p.estadoEn(instante(LocalDateTime.of(2027, 1, 10, 15, 0))))
				.isEqualTo(EstadoVisiblePeriodoMatricula.ABIERTO);
		assertThat(p.estadoEn(instante(CIERRE))).isEqualTo(EstadoVisiblePeriodoMatricula.ABIERTO);
	}

	@Test
	void esCerradoDespuesDelCierre() {
		assertThat(periodo().estadoEn(instante(CIERRE).plusSeconds(1)))
				.isEqualTo(EstadoVisiblePeriodoMatricula.CERRADO);
	}

	@Test
	void unPeriodoGuardadoComoCerradoEsCerradoAunqueLaFechaEsteDentroDelRango() {
		PeriodoMatricula p = periodo();
		p.cerrar();

		assertThat(p.getEstado()).isEqualTo(EstadoPeriodoMatricula.CERRADO);
		assertThat(p.estadoEn(instante(LocalDateTime.of(2027, 1, 10, 15, 0))))
				.isEqualTo(EstadoVisiblePeriodoMatricula.CERRADO);
	}

	@Test
	void unPeriodoQueNuncaSeHabilitoNuncaEstaAbierto() {
		PeriodoMatricula p = periodo();
		ReflectionTestUtils.setField(p, "estado", EstadoPeriodoMatricula.EN_CONFIGURACION);

		assertThat(p.estadoEn(instante(LocalDateTime.of(2027, 1, 10, 15, 0))))
				.isEqualTo(EstadoVisiblePeriodoMatricula.PROGRAMADO);
		assertThat(p.estadoEn(instante(CIERRE).plusSeconds(1))).isEqualTo(EstadoVisiblePeriodoMatricula.CERRADO);
	}

	@Test
	void unHabilitadoQueYaVencioSeDetectaParaMarcarloCerrado() {
		PeriodoMatricula p = periodo();

		assertThat(p.estaHabilitadoYVencido(instante(CIERRE))).isFalse();
		assertThat(p.estaHabilitadoYVencido(instante(CIERRE).plusSeconds(1))).isTrue();
		p.cerrar();
		assertThat(p.estaHabilitadoYVencido(instante(CIERRE).plusSeconds(1))).isFalse();
	}

	// --- Fechas ---

	@Test
	void laFechaYLaHoraDeInicioYCierreSonObligatorias() {
		rechazaDatos("Ingresa la fecha y la hora de inicio.",
				() -> PeriodoMatricula.validar(null, CIERRE, List.of(), AHORA));
		rechazaDatos("Ingresa la fecha y la hora de cierre.",
				() -> PeriodoMatricula.validar(INICIO, null, List.of(), AHORA));
	}

	@Test
	void elCierreDebeSerPosteriorAlInicio() {
		rechazaDatos("La fecha de cierre debe ser posterior a la de inicio.",
				() -> PeriodoMatricula.validar(INICIO, INICIO, List.of(), AHORA));
		rechazaDatos("La fecha de cierre debe ser posterior a la de inicio.",
				() -> PeriodoMatricula.validar(INICIO, INICIO.minusDays(1), List.of(), AHORA));
		PeriodoMatricula.validar(INICIO, INICIO.plusMinutes(1), List.of(), AHORA);
	}

	@Test
	void elInicioNoPuedeSerAnteriorAAhora() {
		rechazaDatos("La fecha de inicio no puede ser anterior a la fecha y hora actuales.",
				() -> PeriodoMatricula.validar(LocalDateTime.of(2026, 10, 22, 8, 59), INICIO.plusYears(1), List.of(),
						AHORA));
		PeriodoMatricula.validar(LocalDateTime.of(2026, 10, 22, 9, 0), INICIO, List.of(), AHORA);
	}

	// --- Documentos ---

	@Test
	void elNombreDelDocumentoEsObligatorio() {
		rechazaDatos("Ingresa el nombre del documento.", () -> periodo(doc("  ")));
		rechazaDatos("Ingresa el nombre del documento.", () -> periodo(doc(null)));
	}

	@Test
	void elNombreAdmiteHasta100Caracteres() {
		periodo(doc("x".repeat(100)));
		rechazaDatos("El nombre del documento no puede pasar de 100 caracteres.", () -> periodo(doc("x".repeat(101))));
	}

	@Test
	void laDescripcionEsOpcionalYAdmiteHasta100Caracteres() {
		periodo(new DatosDocumento(null, "Ficha", "d".repeat(100)));
		periodo(new DatosDocumento(null, "Ficha", "   "));
		rechazaDatos("La descripción del documento no puede pasar de 100 caracteres.",
				() -> periodo(new DatosDocumento(null, "Ficha", "d".repeat(101))));
	}

	@Test
	void elNombreEsUnicoEnElPeriodoSinDistinguirMayusculasNiEspacios() {
		rechazaDatos("Ya hay un documento llamado \"dni\" en este periodo.", () -> periodo(doc("DNI"), doc(" dni ")));
	}

	@Test
	void unaDescripcionVaciaSeGuardaComoNula() {
		PeriodoMatricula p = periodo(new DatosDocumento(null, "  Ficha  ", "  "));

		assertThat(p.getDocumentos().getFirst().getNombre()).isEqualTo("Ficha");
		assertThat(p.getDocumentos().getFirst().getDescripcion()).isNull();
	}

	// --- Editar ---

	@Test
	void unPeriodoProgramadoCambiaSusFechasYSusDocumentos() {
		PeriodoMatricula p = periodo(doc("Ficha"), doc("DNI"));
		UUID ficha = p.getDocumentos().get(0).getId();
		UUID dni = p.getDocumentos().get(1).getId();

		p.editar(INICIO.plusDays(1), CIERRE.plusDays(2),
				List.of(new DatosDocumento(dni, "DNI del estudiante", "Ambas caras"),
						new DatosDocumento(ficha, "Ficha", null), doc("Declaración jurada")),
				AHORA);

		assertThat(p.getInicio()).isEqualTo(instante(INICIO.plusDays(1)));
		assertThat(p.getCierre()).isEqualTo(instante(CIERRE.plusDays(2)));
		assertThat(p.getDocumentos()).extracting(DocumentoRequerido::getNombre).containsExactly("DNI del estudiante",
				"Ficha", "Declaración jurada");
		assertThat(p.getDocumentos()).extracting(DocumentoRequerido::getOrden).containsExactly(1, 2, 3);
		assertThat(p.getDocumentos().get(0).getId()).isEqualTo(dni); // el mismo documento, no uno nuevo
		assertThat(p.getDocumentos().get(0).getDescripcion()).isEqualTo("Ambas caras");
		assertThat(p.getDocumentos().get(2).getId()).isNull(); // nuevo: JPA le asigna el id
	}

	@Test
	void editarQuitaLosDocumentosQueNoVienen() {
		PeriodoMatricula p = periodo(doc("Ficha"), doc("DNI"));
		UUID dni = p.getDocumentos().get(1).getId();

		p.editar(INICIO, CIERRE, List.of(new DatosDocumento(dni, "DNI", null)), AHORA);

		assertThat(p.getDocumentos()).extracting(DocumentoRequerido::getNombre).containsExactly("DNI");
	}

	@Test
	void retirarLosAusentesDejaSoloLosQueVienenPorId() {
		PeriodoMatricula p = periodo(doc("Ficha"), doc("DNI"));
		UUID ficha = p.getDocumentos().get(0).getId();

		p.retirarDocumentosAusentes(List.of(new DatosDocumento(ficha, "Ficha", null), doc("Nuevo")));

		assertThat(p.getDocumentos()).extracting(DocumentoRequerido::getNombre).containsExactly("Ficha");
	}

	@Test
	void unDocumentoQueNoEsDelPeriodoSeRechaza() {
		PeriodoMatricula p = periodo(doc("Ficha"));

		rechazaDatos("El documento no pertenece a este periodo.",
				() -> p.editar(INICIO, CIERRE, List.of(new DatosDocumento(UUID.randomUUID(), "Otro", null)), AHORA));
	}

	@Test
	void editarConDatosInvalidosNoCambiaNada() {
		PeriodoMatricula p = periodo(doc("Ficha"));

		rechazaDatos("La fecha de cierre debe ser posterior a la de inicio.",
				() -> p.editar(INICIO, INICIO.minusDays(1), List.of(doc("Otro")), AHORA));

		assertThat(p.getInicio()).isEqualTo(instante(INICIO));
		assertThat(p.getDocumentos()).extracting(DocumentoRequerido::getNombre).containsExactly("Ficha");
	}

	@Test
	void unPeriodoAbiertoNoSeEditaNiSeElimina() {
		PeriodoMatricula p = periodo();
		Instant abierto = instante(LocalDateTime.of(2027, 1, 10, 15, 0));

		rechazaEstado("Una vez habilitado, el periodo no se puede editar; solo deshabilitar.",
				() -> p.exigirEditable(abierto));
		rechazaEstado("Una vez habilitado, el periodo no se puede editar; solo deshabilitar.",
				() -> p.editar(INICIO, CIERRE, List.of(), abierto));
		rechazaEstado("Solo se puede eliminar un periodo de matrícula programado.", () -> p.exigirEliminable(abierto));
	}

	@Test
	void unPeriodoCerradoNoSeEditaNiSeElimina() {
		PeriodoMatricula p = periodo();
		Instant cerrado = instante(CIERRE).plusSeconds(1);

		rechazaEstado("El periodo de matrícula ya cerró: no se puede editar.", () -> p.exigirEditable(cerrado));
		rechazaEstado("Solo se puede eliminar un periodo de matrícula programado.", () -> p.exigirEliminable(cerrado));
	}

	@Test
	void unPeriodoProgramadoSeEditaYSeElimina() {
		PeriodoMatricula p = periodo();

		p.exigirEditable(AHORA);
		p.exigirEliminable(AHORA);
	}
}
