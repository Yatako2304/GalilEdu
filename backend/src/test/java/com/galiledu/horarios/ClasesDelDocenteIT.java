package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.ClasesDelDocente;
import com.galiledu.horarios.aplicacion.ClasesDelDocente.ClaseDelDocente;

/**
 * HU-49: las clases que dicta un docente, leídas de la BD real. El docente es María Fernández Ríos (U20260004); "otro
 * docente" es el Coordinador, porque solo importa que sea otro usuario.
 */
class ClasesDelDocenteIT extends PruebaHorarios {
	@Autowired
	private ClasesDelDocente clasesDelDocente;

	private UUID estructuraSecundaria;
	private UUID estructuraPrimaria;
	private UUID aulaUno;
	private UUID aulaDos;
	private UUID algebra;
	private UUID algebraPrimaria;
	private UUID t1;
	private UUID t2Primaria;
	private UUID docente;

	/** 1.° T1 (secundaria, 08:00) y 1.° T2 (primaria, 08:30), las dos con Álgebra y el docente como titular. */
	private void preparar() {
		estructuraSecundaria = estructura("EH-T-27", "SECUNDARIA", true, 6);
		estructuraPrimaria = estructura("EH-P-27", "PRIMARIA", true, 6, "08:30");
		aulaUno = ambiente("TMP-A1", "ACTIVO");
		aulaDos = ambiente("TMP-A2", "ACTIVO");
		docente = docente();

		algebra = plan("Álgebra", 5);
		t1 = seccion("SEC-1", "T1", aulaUno, null);
		asignarDocente(algebra, t1, docente);

		jdbc.sql("""
				INSERT INTO academico.curso_grado (curso_id, grado_codigo)
				SELECT id, 'PRI-1' FROM academico.curso WHERE nombre = 'Álgebra'""").update();
		algebraPrimaria = plan("PRI-1", "Álgebra", 3);
		t2Primaria = seccion("PRI-1", "T2", aulaDos, null);
		asignarDocente(algebraPrimaria, t2Primaria, docente);
	}

	private UUID otroDocente() {
		return jdbc.sql("SELECT id FROM seguridad.usuario WHERE nombre_usuario = :u").param("u", COORDINADOR)
				.query(UUID.class).single();
	}

	/** Un usuario que solo tiene el rol TUTOR (no dicta ningún curso). */
	private UUID soloTutor() {
		UUID persona = jdbc.sql("""
				INSERT INTO personas.persona (tipo_documento, numero_documento, apellido_paterno, apellido_materno,
				                              nombres, fecha_nacimiento, nacionalidad, sexo)
				VALUES ('DNI', '79999991', 'Tutor', 'Solo', 'Tomás', '1980-01-01', 'Peruana', 'MASCULINO')
				RETURNING id""").query(UUID.class).single();
		UUID usuario = jdbc.sql("""
				INSERT INTO seguridad.usuario (persona_id, nombre_usuario, contrasena_hash, estado)
				VALUES (:p, 'U20269991', 'sin-acceso', 'ACTIVO') RETURNING id""").param("p", persona)
				.query(UUID.class).single();
		jdbc.sql("INSERT INTO seguridad.usuario_rol (usuario_id, rol_codigo) VALUES (:u, 'TUTOR')")
				.param("u", usuario).update();
		return usuario;
	}

	@Test
	void traeSusClasesDeVariasSeccionesConLaEstructuraYElNivelDeCadaUna() {
		preparar();
		bloque(cabecera(t1, estructuraSecundaria), "LUN", 1, algebra, aulaUno);
		bloque(cabecera(t2Primaria, estructuraPrimaria), "LUN", 2, algebraPrimaria, aulaDos);

		List<ClaseDelDocente> clases = clasesDelDocente.clasesDe(docente, ANIO_2027);

		assertThat(clases).containsExactly(
				new ClaseDelDocente(t1, "1.° T1", "SECUNDARIA", estructuraSecundaria, DiaSemana.LUN, 1, algebra,
						"Álgebra", aulaUno, "Aula TMP-A1"),
				new ClaseDelDocente(t2Primaria, "1.° T2", "PRIMARIA", estructuraPrimaria, DiaSemana.LUN, 2,
						algebraPrimaria, "Álgebra", aulaDos, "Aula TMP-A2"));
	}

	@Test
	void ordenaPorDiaDeLaSemanaYNoAlfabeticamente() {
		preparar();
		UUID cabecera = cabecera(t1, estructuraSecundaria);
		bloque(cabecera, "VIE", 1, algebra, aulaUno);
		bloque(cabecera, "MAR", 3, algebra, aulaUno);
		bloque(cabecera, "MAR", 1, algebra, aulaUno);
		bloque(cabecera, "LUN", 2, algebra, aulaUno);

		List<ClaseDelDocente> clases = clasesDelDocente.clasesDe(docente, ANIO_2027);

		assertThat(clases).extracting(c -> c.dia() + ":" + c.numeroBloque())
				.containsExactly("LUN:2", "MAR:1", "MAR:3", "VIE:1");
	}

	@Test
	void noTraeLasClasesDeOtroDocenteNiLasDeUnCursoCuyoTitularCambio() {
		preparar();
		UUID cabecera = cabecera(t1, estructuraSecundaria);
		bloque(cabecera, "LUN", 1, algebra, aulaUno);
		// El horario guardado sigue con Álgebra, pero el titular de la sección pasó a otro docente.
		jdbc.sql("UPDATE academico.docente_seccion SET docente_id = :d WHERE plan_curso_id = :p AND seccion_id = :s")
				.param("d", otroDocente()).param("p", algebra).param("s", t1).update();

		assertThat(clasesDelDocente.clasesDe(docente, ANIO_2027)).isEmpty();
		assertThat(clasesDelDocente.clasesDe(otroDocente(), ANIO_2027)).extracting(ClaseDelDocente::seccionId)
				.containsExactly(t1);
	}

	@Test
	void unTutorQueNoDictaNoTieneClasesAunqueSeaTutorDeUnaSeccionConHorario() {
		preparar();
		UUID tutor = soloTutor();
		jdbc.sql("UPDATE configuracion.seccion SET tutor_id = :t WHERE id = :s").param("t", tutor).param("s", t1)
				.update();
		bloque(cabecera(t1, estructuraSecundaria), "LUN", 1, algebra, aulaUno);

		assertThat(clasesDelDocente.clasesDe(tutor, ANIO_2027)).isEmpty();
	}

	@Test
	void soloTraeElAnioPedido() {
		preparar();
		bloque(cabecera(t1, estructuraSecundaria), "LUN", 1, algebra, aulaUno);

		assertThat(clasesDelDocente.clasesDe(docente, UUID.randomUUID())).isEmpty();
		assertThat(clasesDelDocente.clasesDe(docente, ANIO_2027)).hasSize(1);
	}

	@Test
	void aniosDevuelveSoloLosAniosConClasesDelDocente() {
		preparar();
		// 2027 tiene un bloque de este docente; en 2026 solo hay datos de prueba sin clases suyas.
		bloque(cabecera(t1, estructuraSecundaria), "LUN", 1, algebra, aulaUno);

		assertThat(clasesDelDocente.aniosDe(docente)).extracting(a -> a.id()).containsExactly(ANIO_2027);
		assertThat(clasesDelDocente.aniosDe(docente)).first().satisfies(a -> {
			assertThat(a.anio()).isEqualTo(2027);
			assertThat(a.estado()).isEqualTo("PLANIFICADO");
		});
	}

	@Test
	void aniosDeUnDocenteSinClasesEsVacio() {
		preparar();
		// Hay horario guardado, pero con otro titular: el docente no dicta nada.
		bloque(cabecera(t1, estructuraSecundaria), "LUN", 1, algebra, aulaUno);
		jdbc.sql("UPDATE academico.docente_seccion SET docente_id = :d WHERE plan_curso_id = :p AND seccion_id = :s")
				.param("d", otroDocente()).param("p", algebra).param("s", t1).update();

		assertThat(clasesDelDocente.aniosDe(docente)).isEmpty();
		assertThat(clasesDelDocente.aniosDe(soloTutor())).isEmpty();
	}

	@Test
	void unaSeccionSinHorarioGuardadoNoAportaClases() {
		preparar();

		assertThat(clasesDelDocente.clasesDe(docente, ANIO_2027)).isEmpty();
	}
}
