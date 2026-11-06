package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.BloqueOcupado;
import com.galiledu.horarios.aplicacion.BloqueSolicitado;
import com.galiledu.horarios.aplicacion.CruceDetectado;
import com.galiledu.horarios.aplicacion.TipoCruce;
import com.galiledu.horarios.aplicacion.ValidadorCruces;

/**
 * HU-45: con un {@link ValidadorCruces} de mentira que siempre encuentra un cruce en LUN, bloque 1, el PUT responde 409
 * con su mensaje y no escribe nada, y el GET reparte el cruce en la celda correspondiente. La detección real es de
 * HU-46.
 */
@Import(HorarioCrucesIT.ValidadorFalso.class)
class HorarioCrucesIT extends PruebaHorarios {
	private static final String MENSAJE = "El docente ya dicta Álgebra en 5.° B ese día y a esa hora.";

	@TestConfiguration
	static class ValidadorFalso {
		@Bean
		@Primary
		ValidadorCruces validadorDeMentira() {
			return new ValidadorCruces() {
				@Override
				public List<CruceDetectado> buscarCruces(UUID anioId, UUID seccionId,
						List<BloqueSolicitado> solicitados) {
					return solicitados.stream().filter(b -> b.dia() == DiaSemana.LUN && b.numeroBloque() == 1)
							.map(b -> new CruceDetectado(TipoCruce.DOCENTE, b.dia(), b.numeroBloque(), MENSAJE, "5.° B",
									"Álgebra"))
							.toList();
				}

				@Override
				public List<CruceDetectado> compararCruces(List<BloqueSolicitado> solicitados,
						List<BloqueOcupado> ocupados) {
					return List.of();
				}
			};
		}
	}

	@Test
	void unCruceHaceQueElPutResponda409YNoEscribaNada() throws Exception {
		estructura("EH-T-27", "SECUNDARIA", true, 6);
		UUID aula = ambiente("TMP-A1", "ACTIVO");
		UUID seccion = seccion("SEC-1", "T1", aula, null);
		UUID algebra = plan("Álgebra", 5);
		asignarDocente(algebra, seccion, docente());

		guardar(seccion, COORDINADOR, null, new Celda("MAR", 2, algebra, aula), new Celda("LUN", 1, algebra, aula))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(MENSAJE));

		assertThat(cabeceras()).isZero();
		assertThat(bloquesGuardados()).isZero();
	}

	@Test
	void elGetRepartEnCadaCeldaSuCruce() throws Exception {
		UUID estructura = estructura("EH-T-27", "SECUNDARIA", true, 6);
		UUID aula = ambiente("TMP-A1", "ACTIVO");
		UUID seccion = seccion("SEC-1", "T1", aula, null);
		UUID algebra = plan("Álgebra", 5);
		asignarDocente(algebra, seccion, docente());
		UUID cab = cabecera(seccion, estructura);
		bloque(cab, "LUN", 1, algebra, aula);
		bloque(cab, "MAR", 2, algebra, aula);

		pedir("/horarios/secciones/" + seccion, COORDINADOR).andExpect(status().isOk())
				.andExpect(jsonPath("$.celdas[0].dia").value("LUN"))
				.andExpect(jsonPath("$.celdas[0].cruces[0].tipo").value("DOCENTE"))
				.andExpect(jsonPath("$.celdas[0].cruces[0].mensaje").value(MENSAJE))
				.andExpect(jsonPath("$.celdas[0].cruces[0].seccion").value("5.° B"))
				.andExpect(jsonPath("$.celdas[0].cruces[0].curso").value("Álgebra"))
				.andExpect(jsonPath("$.celdas[1].cruces", empty()));
	}
}
