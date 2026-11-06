package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * HU-45 + HU-46: el PUT y el GET de la matriz con el validador de cruces real (a diferencia de
 * {@link HorarioCrucesIT}, que usa uno de mentira). 1.° T1 ya tiene guardado Álgebra el lunes en el bloque 1.
 */
class HorarioConValidadorRealIT extends PruebaHorarios {
	private static final String MENSAJE = "María Fernández Ríos ya dicta Álgebra con 1.° T1 "
			+ "el lunes de 8:00 a 8:45.";

	private UUID estructura;
	private UUID aulaUno;
	private UUID aulaDos;
	private UUID algebra;
	private UUID t2;

	private void preparar() {
		estructura = estructura("EH-T-27", "SECUNDARIA", true, 6);
		aulaUno = ambiente("TMP-A1", "ACTIVO");
		aulaDos = ambiente("TMP-A2", "ACTIVO");
		algebra = plan("Álgebra", 5);
		UUID t1 = seccion("SEC-1", "T1", aulaUno, null);
		t2 = seccion("SEC-1", "T2", aulaDos, null);
		asignarDocente(algebra, t1, docente());
		asignarDocente(algebra, t2, docente());
		bloque(cabecera(t1, estructura), "LUN", 1, algebra, aulaUno);
	}

	@Test
	void guardarUnBloqueQueChocaConOtraSeccionResponde409YNoGuardaNada() throws Exception {
		preparar();

		guardar(t2, COORDINADOR, null, new Celda("MAR", 2, algebra, aulaDos), new Celda("LUN", 1, algebra, aulaDos))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Docente no disponible: " + MENSAJE));

		// Solo queda lo de T1: ni la cabecera ni los bloques de T2.
		assertThat(cabeceras()).isEqualTo(1);
		assertThat(bloquesGuardados()).isEqualTo(1);
	}

	@Test
	void guardarSinChoquesSigueFuncionando() throws Exception {
		preparar();

		guardar(t2, COORDINADOR, null, new Celda("MAR", 2, algebra, aulaDos)).andExpect(status().isOk());

		assertThat(bloquesGuardados()).isEqualTo(2);
	}

	@Test
	void elGetTraeElCruceEnLaCeldaQueChoca() throws Exception {
		preparar();
		UUID cabeceraT2 = cabecera(t2, estructura);
		bloque(cabeceraT2, "LUN", 1, algebra, aulaDos);
		bloque(cabeceraT2, "MAR", 2, algebra, aulaDos);

		pedir("/horarios/secciones/" + t2, COORDINADOR).andExpect(status().isOk())
				.andExpect(jsonPath("$.celdas[0].dia").value("LUN"))
				.andExpect(jsonPath("$.celdas[0].cruces[0].tipo").value("DOCENTE"))
				.andExpect(jsonPath("$.celdas[0].cruces[0].mensaje").value(MENSAJE))
				.andExpect(jsonPath("$.celdas[0].cruces[0].seccion").value("1.° T1"))
				.andExpect(jsonPath("$.celdas[0].cruces[0].curso").value("Álgebra"))
				.andExpect(jsonPath("$.celdas[1].dia").value("MAR"))
				.andExpect(jsonPath("$.celdas[1].cruces", empty()));
	}
}
