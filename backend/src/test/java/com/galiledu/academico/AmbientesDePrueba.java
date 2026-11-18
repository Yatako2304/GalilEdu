package com.galiledu.academico;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Limpieza de las pruebas de HU-43 que <b>confirman de verdad</b> filas de {@code configuracion.ambiente} (las que
 * llaman a {@code crear}, que usa una transacción por intento): {@code ServicioAmbientesCrearIT} y
 * {@code AmbienteCrearIT}. Ninguna otra prueba la necesita: las demás se deshacen solas.
 * <p>
 * Reglas, para que la base compartida quede como estaba:
 * <ul>
 * <li>Solo se borran las filas que la prueba creó (las que no estaban al empezar): nunca las de la semilla.</li>
 * <li>Al empezar se borran los restos de una ejecución interrumpida, por el prefijo de nombre {@link #PREFIJO}, para
 * que A-206 y E-001 estén libres.</li>
 * <li>Antes y después se comprueba que sigan las 8 filas de la semilla.</li>
 * </ul>
 */
final class AmbientesDePrueba {
	/** Todos los nombres que crean esas pruebas empiezan así. */
	static final String PREFIJO = "ZZ-IT ";
	static final List<String> CODIGOS_SEMILLA = List.of("A-101", "A-102", "A-201", "A-205", "LAB-01", "LAB-02",
			"BIB-01",
			"AUD-01");

	private AmbientesDePrueba() {
	}

	/** Borra los restos de una ejecución interrumpida y devuelve los ids que hay: la foto "de partida". */
	static Set<UUID> empezarLimpio(JdbcClient jdbc) {
		jdbc.sql("DELETE FROM configuracion.ambiente WHERE nombre LIKE :prefijo").param("prefijo", PREFIJO + "%")
				.update();
		exigirSemillaIntacta(jdbc);
		return Set.copyOf(jdbc.sql("SELECT id FROM configuracion.ambiente").query(UUID.class).list());
	}

	/** Borra todo lo que no estaba en la foto de partida: nunca una fila de la semilla, porque todas estaban. */
	static void borrarLoCreadoDesde(JdbcClient jdbc, Set<UUID> idsAlEmpezar) {
		jdbc.sql("DELETE FROM configuracion.ambiente WHERE id NOT IN (:ids)").param("ids", idsAlEmpezar).update();
		exigirSemillaIntacta(jdbc);
	}

	/**
	 * Iniciar sesión sin transacción de prueba <b>confirma</b> {@code ultimo_acceso} (y {@code actualizado_en}) del
	 * usuario. Son los valores que se guardan al empezar para restaurarlos al terminar.
	 */
	record Acceso(String usuario, OffsetDateTime ultimoAcceso, OffsetDateTime actualizadoEn) {
	}

	static List<Acceso> accesos(JdbcClient jdbc, String... usuarios) {
		return jdbc.sql("""
				SELECT nombre_usuario, ultimo_acceso, actualizado_en FROM seguridad.usuario
				WHERE nombre_usuario IN (:usuarios)""").param("usuarios", List.of(usuarios))
				.query((rs, fila) -> new Acceso(rs.getString("nombre_usuario"),
						rs.getObject("ultimo_acceso", OffsetDateTime.class),
						rs.getObject("actualizado_en", OffsetDateTime.class)))
				.list();
	}

	/** Deja el acceso de esos usuarios como estaba al empezar, y lo comprueba releyéndolo de la base. */
	static void restaurarAccesos(JdbcClient jdbc, List<Acceso> alEmpezar, String... usuarios) {
		for (Acceso acceso : alEmpezar) {
			int filas = jdbc.sql("""
					UPDATE seguridad.usuario SET ultimo_acceso = :ultimoAcceso, actualizado_en = :actualizadoEn
					WHERE nombre_usuario = :usuario""").param("ultimoAcceso", acceso.ultimoAcceso())
					.param("actualizadoEn", acceso.actualizadoEn()).param("usuario", acceso.usuario()).update();
			assertThat(filas).as("restaurar el acceso de " + acceso.usuario()).isEqualTo(1);
		}
		assertThat(accesos(jdbc, usuarios)).containsExactlyInAnyOrderElementsOf(alEmpezar);
	}

	static void exigirSemillaIntacta(JdbcClient jdbc) {
		int presentes = jdbc.sql("SELECT count(*)::int FROM configuracion.ambiente WHERE codigo IN (:codigos)")
				.param("codigos", CODIGOS_SEMILLA).query(Integer.class).single();
		assertThat(presentes).as("las 8 filas de la semilla").isEqualTo(CODIGOS_SEMILLA.size());
	}
}
