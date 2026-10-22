package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.configuracion.dominio.DatosReceso;
import com.galiledu.configuracion.dominio.DefinicionEstructura;
import com.galiledu.configuracion.dominio.EstructuraHoraria;
import com.galiledu.configuracion.dominio.Receso;
import com.galiledu.configuracion.infraestructura.EstructuraHorariaRepositorio;

/**
 * HU-44: el mapeo de la estructura horaria contra PostgreSQL real (el arreglo de días, los recesos hijos y el índice
 * de la principal) y las consultas del repositorio. 2026 es el año ACTIVO con EH-PRI-26 y EH-SEC-26; 2027 está
 * planificado y sin estructuras.
 */
class EstructuraHorariaRepositorioIT extends PruebaIntegracion {
	private static final UUID ANIO_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");
	private static final UUID ANIO_2027 = UUID.fromString("30000000-0000-0000-0000-000000002027");
	private static final UUID EH_PRI_26 = UUID.fromString("31000000-0000-0000-0000-000000000001");
	private static final UUID EH_SEC_26 = UUID.fromString("31000000-0000-0000-0000-000000000002");

	@Autowired
	private EstructuraHorariaRepositorio repositorio;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private JdbcClient jdbc;

	private static DefinicionEstructura definicion(String codigo, List<String> dias, List<DatosReceso> recesos) {
		return new DefinicionEstructura(codigo, "Prueba", dias, LocalTime.of(8, 0), 6, 45, recesos);
	}

	private EstructuraHoraria guardar(String codigo, String nivel, boolean esPrincipal, List<DatosReceso> recesos) {
		return repositorio.saveAndFlush(EstructuraHoraria.crear(ANIO_2027, nivel, esPrincipal,
				definicion(codigo, List.of("LUN", "MAR", "MIE", "JUE", "VIE"), recesos)));
	}

	private void recargarDeLaBase() {
		entityManager.flush();
		entityManager.clear();
	}

	private List<String> codigosPrincipalesDe2027(String nivel) {
		entityManager.flush();
		return jdbc.sql("""
				SELECT codigo FROM configuracion.estructura_horaria
				WHERE anio_escolar_id = :anioId AND nivel = :nivel AND es_principal""")
				.param("anioId", ANIO_2027).param("nivel", nivel).query(String.class).list();
	}

	private List<Integer> bloquesConRecesoEnLaBase(UUID estructuraId) {
		entityManager.flush();
		return jdbc.sql("""
				SELECT despues_de_bloque FROM configuracion.receso
				WHERE estructura_horaria_id = :id ORDER BY despues_de_bloque""")
				.param("id", estructuraId).query(Integer.class).list();
	}

	// ---------- Mapeo con la base real ----------

	@Test
	void guardaLosDiasLectivosComoArregloDePostgreSQL() {
		EstructuraHoraria guardada = repositorio.saveAndFlush(EstructuraHoraria.crear(ANIO_2027, "PRIMARIA", true,
				definicion("EH-TMP", List.of("VIE", "LUN", "MIE"), List.of())));

		String enLaBase = jdbc.sql("SELECT dias_lectivos::text FROM configuracion.estructura_horaria WHERE id = :id")
				.param("id", guardada.getId()).query(String.class).single();

		// Quedan en el orden de la semana, no en el que llegaron.
		assertThat(enLaBase).isEqualTo("{LUN,MIE,VIE}");
	}

	@Test
	void leeLosDiasLectivosDeUnaFilaInsertadaPorSql() {
		UUID id = jdbc.sql("""
				INSERT INTO configuracion.estructura_horaria
				    (anio_escolar_id, codigo, dias_lectivos, hora_entrada, bloques_por_dia, duracion_bloque_min, nivel)
				VALUES (:anioId, 'EH-SQL', ARRAY['MAR', 'JUE'], '09:15', 4, 50, 'SECUNDARIA')
				RETURNING id""")
				.param("anioId", ANIO_2027).query(UUID.class).single();
		entityManager.clear();

		EstructuraHoraria leida = repositorio.findById(id).orElseThrow();

		assertThat(leida.getDiasLectivos()).containsExactly("MAR", "JUE");
		assertThat(leida.getHoraEntrada()).isEqualTo(LocalTime.of(9, 15));
		assertThat(leida.getBloquesPorDia()).isEqualTo(4);
		assertThat(leida.getDuracionBloqueMin()).isEqualTo(50);
		assertThat(leida.bloquesPorSemana()).isEqualTo(8);
	}

	@Test
	void leeLaEstructuraDeFigmaConSusRecesosEnOrden() {
		EstructuraHoraria primaria = repositorio.findById(EH_PRI_26).orElseThrow();

		assertThat(primaria.getDiasLectivos()).containsExactly("LUN", "MAR", "MIE", "JUE", "VIE");
		assertThat(primaria.getRecesos()).extracting(Receso::getDespuesDeBloque).containsExactly(3, 6);
		assertThat(primaria.getRecesos()).extracting(Receso::getDuracionMin).containsExactly(20, 15);
		assertThat(primaria.jornada().salidaMinutos()).isEqualTo(14 * 60 + 20);
	}

	// ---------- Recesos hijos ----------

	@Test
	void guardaLosRecesosJuntoConLaEstructura() {
		EstructuraHoraria guardada = guardar("EH-TMP", "PRIMARIA", true,
				List.of(new DatosReceso(2, 15), new DatosReceso(4, 20)));

		assertThat(bloquesConRecesoEnLaBase(guardada.getId())).containsExactly(2, 4);
	}

	@Test
	void unRecesoQueSigueEnElMismoBloqueSoloCambiaDeDuracion() {
		EstructuraHoraria guardada = guardar("EH-TMP", "PRIMARIA", true,
				List.of(new DatosReceso(3, 20), new DatosReceso(5, 10)));
		recargarDeLaBase();
		EstructuraHoraria cargada = repositorio.findById(guardada.getId()).orElseThrow();
		UUID recesoDelBloque3 = cargada.getRecesos().get(0).getId();

		cargada.editar(definicion("EH-TMP", List.of("LUN", "MAR"),
				List.of(new DatosReceso(3, 40), new DatosReceso(5, 10))));
		recargarDeLaBase();

		EstructuraHoraria despues = repositorio.findById(guardada.getId()).orElseThrow();
		assertThat(despues.getRecesos()).extracting(Receso::getDespuesDeBloque).containsExactly(3, 5);
		assertThat(despues.getRecesos()).extracting(Receso::getDuracionMin).containsExactly(40, 10);
		assertThat(despues.getRecesos().get(0).getId()).isEqualTo(recesoDelBloque3);
	}

	@Test
	void sePuedeQuitarUnRecesoYVolverAPonerloEnElMismoBloque() {
		EstructuraHoraria guardada = guardar("EH-TMP", "PRIMARIA", true,
				List.of(new DatosReceso(3, 20), new DatosReceso(5, 10)));
		recargarDeLaBase();
		EstructuraHoraria cargada = repositorio.findById(guardada.getId()).orElseThrow();

		// Quita el receso del bloque 5...
		cargada.editar(definicion("EH-TMP", List.of("LUN"), List.of(new DatosReceso(3, 20))));
		recargarDeLaBase();
		assertThat(bloquesConRecesoEnLaBase(guardada.getId())).containsExactly(3);

		// ...y lo vuelve a crear en el mismo bloque: la restricción única (estructura, bloque) no debe chocar.
		EstructuraHoraria otraVez = repositorio.findById(guardada.getId()).orElseThrow();
		otraVez.editar(definicion("EH-TMP", List.of("LUN"), List.of(new DatosReceso(3, 20), new DatosReceso(5, 25))));
		recargarDeLaBase();

		assertThat(bloquesConRecesoEnLaBase(guardada.getId())).containsExactly(3, 5);
		assertThat(repositorio.findById(guardada.getId()).orElseThrow().getRecesos())
				.extracting(Receso::getDuracionMin).containsExactly(20, 25);
	}

	@Test
	void unaEdicionPuedeQuitarTodosLosRecesosYPedirOtroEnUnBloqueQueTenia() {
		EstructuraHoraria guardada = guardar("EH-TMP", "PRIMARIA", true,
				List.of(new DatosReceso(2, 15), new DatosReceso(4, 20)));
		recargarDeLaBase();
		EstructuraHoraria cargada = repositorio.findById(guardada.getId()).orElseThrow();

		cargada.editar(definicion("EH-TMP", List.of("LUN"), List.of(new DatosReceso(4, 30), new DatosReceso(3, 10))));
		recargarDeLaBase();

		assertThat(bloquesConRecesoEnLaBase(guardada.getId())).containsExactly(3, 4);
		assertThat(repositorio.findById(guardada.getId()).orElseThrow().getRecesos())
				.extracting(Receso::getDuracionMin).containsExactly(10, 30);
	}

	@Test
	void editarSoloLosRecesosMueveLaFechaDeActualizacionEnLaBase() {
		EstructuraHoraria guardada = guardar("EH-TMP", "PRIMARIA", true, List.of(new DatosReceso(3, 20)));
		entityManager.flush();
		// Una fecha muy antigua, puesta por SQL, hace la comparación independiente del reloj.
		jdbc.sql("""
				UPDATE configuracion.estructura_horaria SET actualizado_en = TIMESTAMPTZ '2000-01-01 00:00:00+00'
				WHERE id = :id""").param("id", guardada.getId()).update();
		entityManager.clear();

		EstructuraHoraria cargada = repositorio.findById(guardada.getId()).orElseThrow();
		cargada.editar(definicion("EH-TMP", List.of("LUN", "MAR", "MIE", "JUE", "VIE"),
				List.of(new DatosReceso(3, 20), new DatosReceso(4, 10))));
		entityManager.flush();

		boolean esPosterior = jdbc.sql("""
				SELECT actualizado_en > TIMESTAMPTZ '2000-01-02 00:00:00+00'
				FROM configuracion.estructura_horaria WHERE id = :id""")
				.param("id", guardada.getId()).query(Boolean.class).single();
		assertThat(esPosterior).isTrue();
	}

	// ---------- La principal del nivel ----------

	@Test
	void cambiarLaPrincipalDeUnNivelConFlushEntreDegradarYPromover() {
		EstructuraHoraria actual = guardar("EH-A", "PRIMARIA", true, List.of());
		EstructuraHoraria nueva = guardar("EH-B", "PRIMARIA", false, List.of());
		assertThat(codigosPrincipalesDe2027("PRIMARIA")).containsExactly("EH-A");

		actual.dejarDeSerPrincipal();
		entityManager.flush();
		nueva.marcarComoPrincipal();
		entityManager.flush();

		assertThat(codigosPrincipalesDe2027("PRIMARIA")).containsExactly("EH-B");
	}

	@Test
	void laBaseRechazaDosPrincipalesDelMismoNivelYAnio() {
		guardar("EH-A", "PRIMARIA", true, List.of());

		rechazaPor("ux_estructura_horaria_principal", () -> guardar("EH-B", "PRIMARIA", true, List.of()));
	}

	@Test
	void laBaseAdmiteUnaPrincipalPorCadaNivel() {
		guardar("EH-PRI", "PRIMARIA", true, List.of());
		guardar("EH-SEC", "SECUNDARIA", true, List.of());

		assertThat(codigosPrincipalesDe2027("PRIMARIA")).containsExactly("EH-PRI");
		assertThat(codigosPrincipalesDe2027("SECUNDARIA")).containsExactly("EH-SEC");
	}

	// ---------- Consultas del repositorio ----------

	@Test
	void listaLasEstructurasDelAnioPorCodigo() {
		assertThat(repositorio.findByAnioEscolarIdOrderByCodigo(ANIO_2026)).extracting(EstructuraHoraria::getCodigo)
				.containsExactly("EH-PRI-26", "EH-SEC-26");
		assertThat(repositorio.findByAnioEscolarIdOrderByCodigo(ANIO_2027)).isEmpty();
	}

	@Test
	void sabeSiUnCodigoYaExisteEnElAnio() {
		assertThat(repositorio.existsByAnioEscolarIdAndCodigo(ANIO_2026, "EH-PRI-26")).isTrue();
		// En otro año el mismo código es libre.
		assertThat(repositorio.existsByAnioEscolarIdAndCodigo(ANIO_2027, "EH-PRI-26")).isFalse();
		assertThat(repositorio.existsByAnioEscolarIdAndCodigo(ANIO_2026, "EH-NUEVA")).isFalse();
		// La BD distingue mayúsculas: por eso el servicio busca con el código normalizado.
		assertThat(repositorio.existsByAnioEscolarIdAndCodigo(ANIO_2026, "eh-pri-26")).isFalse();
	}

	@Test
	void alEditarElCodigoNoChocaConLaPropiaEstructura() {
		assertThat(repositorio.existsByAnioEscolarIdAndCodigoAndIdNot(ANIO_2026, "EH-PRI-26", EH_PRI_26)).isFalse();
		assertThat(repositorio.existsByAnioEscolarIdAndCodigoAndIdNot(ANIO_2026, "EH-PRI-26", EH_SEC_26)).isTrue();
		assertThat(repositorio.existsByAnioEscolarIdAndCodigoAndIdNot(ANIO_2026, "EH-NUEVA", EH_PRI_26)).isFalse();
	}

	@Test
	void encuentraLaPrincipalDeCadaNivelYAnio() {
		assertThat(repositorio.findByAnioEscolarIdAndNivelAndEsPrincipalTrue(ANIO_2026, "PRIMARIA"))
				.map(EstructuraHoraria::getCodigo).contains("EH-PRI-26");
		assertThat(repositorio.findByAnioEscolarIdAndNivelAndEsPrincipalTrue(ANIO_2026, "SECUNDARIA"))
				.map(EstructuraHoraria::getCodigo).contains("EH-SEC-26");
		assertThat(repositorio.findByAnioEscolarIdAndNivelAndEsPrincipalTrue(ANIO_2027, "PRIMARIA")).isEmpty();
	}

	@Test
	void unaEstructuraQueNoEsPrincipalNoCuentaComoPrincipalDelNivel() {
		guardar("EH-TAR", "SECUNDARIA", false, List.of());

		assertThat(repositorio.findByAnioEscolarIdAndNivelAndEsPrincipalTrue(ANIO_2027, "SECUNDARIA")).isEmpty();
	}

	@Test
	void cuentaLasEstructurasDeUnNivelEnElAnio() {
		assertThat(repositorio.countByAnioEscolarIdAndNivel(ANIO_2026, "PRIMARIA")).isEqualTo(1);
		assertThat(repositorio.countByAnioEscolarIdAndNivel(ANIO_2027, "PRIMARIA")).isZero();

		guardar("EH-A", "PRIMARIA", true, List.of());
		guardar("EH-B", "PRIMARIA", false, List.of());
		guardar("EH-C", "SECUNDARIA", true, List.of());

		assertThat(repositorio.countByAnioEscolarIdAndNivel(ANIO_2027, "PRIMARIA")).isEqualTo(2);
		assertThat(repositorio.countByAnioEscolarIdAndNivel(ANIO_2027, "SECUNDARIA")).isEqualTo(1);
	}
}
