package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.BloqueOcupado;
import com.galiledu.horarios.aplicacion.OcupacionHorarios;

/**
 * HU-45: la ocupación de las demás secciones del año con las horas de SU estructura. Secundaria empieza a las 08:00 y
 * primaria a las 08:30 (bloques de 45 min, con un receso de 20 min después del bloque 3).
 */
class OcupacionHorariosSqlIT extends PruebaHorarios {
	@Autowired
	private OcupacionHorarios ocupacion;

	@Test
	void devuelveLosBloquesDeLasOtrasSeccionesConLasHorasDeSuEstructura() {
		UUID secundaria = estructura("EH-SEC-27", "SECUNDARIA", true, 6, "08:00");
		UUID primaria = estructura("EH-PRI-27", "PRIMARIA", true, 6, "08:30");
		UUID aulaA = ambiente("TMP-A1", "ACTIVO");
		UUID aulaB = ambiente("TMP-A2", "ACTIVO");
		UUID docente = docente();

		// Sección de secundaria: Álgebra con docente, lunes, bloque 2 (08:45 a 09:30) y miércoles, bloque 4 (10:35).
		UUID seccionSec = seccion("SEC-1", "T1", aulaA, null);
		UUID algebra = plan("Álgebra", 5);
		asignarDocente(algebra, seccionSec, docente);
		UUID cabSec = cabecera(seccionSec, secundaria);
		bloque(cabSec, "LUN", 2, algebra, aulaA);
		bloque(cabSec, "MIE", 4, algebra, aulaA);

		// Sección de primaria: el mismo curso en 1.° de primaria, sin docente, lunes, bloque 2 (09:15 a 10:00).
		jdbc.sql("""
				INSERT INTO academico.curso_grado (curso_id, grado_codigo)
				SELECT id, 'PRI-1' FROM academico.curso WHERE nombre = 'Álgebra'""").update();
		UUID seccionPri = seccion("PRI-1", "T2", aulaB, null);
		UUID algebraPri = plan("PRI-1", "Álgebra", 3);
		bloque(cabecera(seccionPri, primaria), "LUN", 2, algebraPri, aulaB);

		// La sección que se edita: su propio bloque no debe aparecer.
		UUID editada = seccion("SEC-1", "T3", aulaA, null);
		bloque(cabecera(editada, secundaria), "MAR", 1, algebra, aulaA);

		List<BloqueOcupado> ocupados = ocupacion.bloquesOcupados(ANIO_2027, editada);

		assertThat(ocupados).hasSize(3);
		assertThat(ocupados).extracting(BloqueOcupado::seccionId).doesNotContain(editada);
		BloqueOcupado lunes = ocupados.stream()
				.filter(o -> o.seccionId().equals(seccionSec) && o.dia() == DiaSemana.LUN).findFirst().orElseThrow();
		assertThat(lunes.inicio()).isEqualTo(LocalTime.of(8, 45));
		assertThat(lunes.fin()).isEqualTo(LocalTime.of(9, 30));
		assertThat(lunes.docenteId()).isEqualTo(docente);
		assertThat(lunes.ambienteId()).isEqualTo(aulaA);
		assertThat(lunes.seccion()).isEqualTo("1.° T1");
		assertThat(lunes.curso()).isEqualTo("Álgebra");
		assertThat(lunes.docente()).contains("María");
		// El mensaje de cruce nombra el ambiente por su código (A-502), como se ve en las celdas de Figma.
		assertThat(lunes.ambiente()).isEqualTo("TMP-A1");

		// Después del receso: el bloque 4 de secundaria empieza a las 10:35.
		BloqueOcupado miercoles = ocupados.stream()
				.filter(o -> o.seccionId().equals(seccionSec) && o.dia() == DiaSemana.MIE).findFirst().orElseThrow();
		assertThat(miercoles.inicio()).isEqualTo(LocalTime.of(10, 35));
		assertThat(miercoles.fin()).isEqualTo(LocalTime.of(11, 20));

		// Primaria usa SU estructura (08:30), no la de la sección editada (08:00).
		BloqueOcupado deLaPrimaria = ocupados.stream().filter(o -> o.seccionId().equals(seccionPri)).findFirst()
				.orElseThrow();
		assertThat(deLaPrimaria.inicio()).isEqualTo(LocalTime.of(9, 15));
		assertThat(deLaPrimaria.fin()).isEqualTo(LocalTime.of(10, 0));
		assertThat(deLaPrimaria.docenteId()).isNull();
		assertThat(deLaPrimaria.docente()).isNullOrEmpty();
		assertThat(deLaPrimaria.seccion()).isEqualTo("1.° T2");
	}

	@Test
	void excluyeLaSeccionIndicadaYNoMiraOtrosAnios() {
		UUID secundaria = estructura("EH-SEC-27", "SECUNDARIA", true, 6);
		UUID aula = ambiente("TMP-A1", "ACTIVO");
		UUID seccion = seccion("SEC-1", "T1", aula, null);
		UUID algebra = plan("Álgebra", 5);
		bloque(cabecera(seccion, secundaria), "LUN", 1, algebra, aula);

		assertThat(ocupacion.bloquesOcupados(ANIO_2027, seccion)).isEmpty();
		assertThat(ocupacion.bloquesOcupados(ANIO_2027, UUID.randomUUID())).hasSize(1);
		assertThat(ocupacion.bloquesOcupados(UUID.fromString("30000000-0000-0000-0000-000000002026"), seccion))
				.isEmpty();
	}
}
