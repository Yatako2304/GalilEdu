package com.galiledu.academico;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * HU-60 · Modificación de una calificación. El III bimestre de 2026 ya cerró su registro (16/10) y el IV está abierto
 * hasta el 23/12: fuera del plazo solo se modifica con motivo; dentro, el registro en lote también deja historial.
 */
class ModificacionCalificacionIT extends PruebaIntegracion {
	private static final String BASE = "/calificaciones/docente";

	@Autowired
	private JdbcClient jdbc;

	private DatosNotasPrueba datos;
	private String docente;
	private String cantidad;
	private String ana;

	@BeforeEach
	void preparar() throws Exception {
		datos = new DatosNotasPrueba(jdbc);
		docente = token(DOCENTE);
		cantidad = datos.competencia(DatosNotasPrueba.CANTIDAD);
		ana = datos.matriculaDe("Ana Lucía");
	}

	private String ruta(String item, String matricula) {
		return BASE + "/items/" + item + "/calificaciones/" + matricula;
	}

	@Test
	void modificaFueraDelPlazoConMotivoYDejaHistorial() throws Exception {
		String item = datos.item("Aritmética", 3, cantidad, "Examen HU-60");
		datos.nota(item, ana, 12);

		mvc.perform(conToken(put(ruta(item, ana)), docente, """
				{"notaNumerica": 15, "motivo": "Error al transcribir el examen"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.notaNumerica").value(15.0))
				.andExpect(jsonPath("$.modificaciones").value(1));

		mvc.perform(conToken(get(ruta(item, ana) + "/historial"), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].notaAnterior").value(12.0))
				.andExpect(jsonPath("$[0].notaNueva").value(15.0))
				.andExpect(jsonPath("$[0].motivo").value("Error al transcribir el examen"))
				.andExpect(jsonPath("$[0].modificadoPor", containsString("María")));

		// La tabla de «Calificar alumnos» muestra la nota nueva y cuántas veces cambió.
		mvc.perform(conToken(get(BASE + "/items/" + item + "/calificaciones"), docente))
				.andExpect(jsonPath("$[?(@.matriculaId == '%s')].modificaciones".formatted(ana)).value(1));
	}

	@Test
	void elPeriodoCerradoPermiteModificarPeroNoRegistrarEnLote() throws Exception {
		String item = datos.item("Aritmética", 3, cantidad, "Examen HU-60");
		datos.nota(item, ana, 12);

		mvc.perform(conToken(put(BASE + "/items/" + item + "/calificaciones"), docente, """
				{"calificaciones": [{"matriculaId": "%s", "notaNumerica": 18}]}""".formatted(ana)))
				.andExpect(status().isConflict());

		mvc.perform(conToken(get(BASE + "/cursos"), docente))
				.andExpect(jsonPath("$[0].periodos[2].registroNotasAbierto").value(false))
				.andExpect(jsonPath("$[0].periodos[2].modificacionHabilitada").value(true));
	}

	@Test
	void exigeMotivoSuficienteYUnValorDistinto() throws Exception {
		String item = datos.item("Aritmética", 3, cantidad, "Examen HU-60");
		datos.nota(item, ana, 12);

		mvc.perform(conToken(put(ruta(item, ana)), docente, """
				{"notaNumerica": 15, "motivo": "corto"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("motivo")));
		mvc.perform(conToken(put(ruta(item, ana)), docente, """
				{"notaNumerica": 12.00, "motivo": "Revisión solicitada por el apoderado"}"""))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(put(ruta(item, ana)), docente, """
				{"nivelLogro": "A", "motivo": "Revisión solicitada por el apoderado"}"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void fueraDePlazoRegistraConMotivoUnaNotaQueFaltaba() throws Exception {
		String item = datos.item("Aritmética", 3, cantidad, "Examen HU-60");
		datos.nota(item, ana, 12);
		String diego = datos.matriculaDe("Diego José");

		mvc.perform(conToken(put(ruta(item, diego)), docente, """
				{"notaNumerica": 14}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("motivo")));
		mvc.perform(conToken(put(ruta(item, diego)), docente, """
				{"notaNumerica": 14, "motivo": "Rindió el examen en fecha de recuperación"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CALIFICADO"));

		mvc.perform(conToken(get(ruta(item, diego) + "/historial"), docente))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].notaAnterior").isEmpty())
				.andExpect(jsonPath("$[0].noEvaluadoAnterior").value(false))
				.andExpect(jsonPath("$[0].notaNueva").value(14.0));
	}

	@Test
	void noEvaluadoExigeMotivoYSacaElItemDelCalculoDelEstudiante() throws Exception {
		String item = datos.item("Aritmética", 3, cantidad, "Examen HU-60");
		datos.nota(item, ana, 16);
		String diego = datos.matriculaDe("Diego José");
		String notas = "/calificaciones/docente/cursos/%s/secciones/%s/notas-competencia"
				.formatted(datos.plan("Aritmética"), datos.seccionA());
		String periodoDiego = "$.filas[?(@.matriculaId == '%s')].competencias[?(@.competenciaId == '%s')].periodos[2]"
				.formatted(diego, cantidad);

		// A Diego le falta la nota: su nota oficial del III bimestre está pendiente, sin número.
		mvc.perform(conToken(get(notas).param("competenciaIds", cantidad), docente))
				.andExpect(jsonPath(periodoDiego + ".pendiente").value(contains(true)))
				.andExpect(jsonPath(periodoDiego + ".nota").value(contains("—")))
				.andExpect(jsonPath(periodoDiego + ".calificadas").value(contains(0)))
				.andExpect(jsonPath(periodoDiego + ".total").value(contains(1)));

		mvc.perform(conToken(put(ruta(item, diego)), docente, """
				{"noEvaluado": true}"""))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(put(ruta(item, diego)), docente, """
				{"noEvaluado": true, "motivo": "Se matriculó después del examen"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("NO_EVALUADO"))
				.andExpect(jsonPath("$.motivoNoEvaluado").value("Se matriculó después del examen"));

		// Ya no falta nada para Diego: el ítem no cuenta para él y su nota deja de estar pendiente.
		mvc.perform(conToken(get(notas).param("competenciaIds", cantidad), docente))
				.andExpect(jsonPath(periodoDiego + ".pendiente").value(contains(false)))
				.andExpect(jsonPath(periodoDiego + ".total").value(contains(0)));
		mvc.perform(conToken(get(ruta(item, diego) + "/historial"), docente))
				.andExpect(jsonPath("$[0].noEvaluadoNuevo").value(true));
	}

	@Test
	void elRegistroEnLoteNoBorraUnNoEvaluado() throws Exception {
		String item = datos.item("Aritmética", 4, cantidad, "Examen HU-60");
		mvc.perform(conToken(put(ruta(item, ana)), docente, """
				{"noEvaluado": true, "motivo": "Licencia médica durante la evaluación"}"""))
				.andExpect(status().isOk());

		mvc.perform(conToken(put(BASE + "/items/" + item + "/calificaciones"), docente, """
				{"calificaciones": [{"matriculaId": "%s", "notaNumerica": null}]}""".formatted(ana)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.matriculaId == '%s')].estado".formatted(ana))
						.value(contains("NO_EVALUADO")));
	}

	@Test
	void dentroDelPlazoElLoteSoloRegistraYUnaNotaGuardadaSeCambiaConModificar() throws Exception {
		String item = datos.item("Aritmética", 4, cantidad, "Examen HU-60");
		String lote = """
				{"calificaciones": [{"matriculaId": "%s", "notaNumerica": %s}]}""";

		mvc.perform(conToken(put(BASE + "/items/" + item + "/calificaciones"), docente, lote.formatted(ana, "10")))
				.andExpect(status().isOk());
		// Registrar por primera vez no es una modificación.
		mvc.perform(conToken(get(ruta(item, ana) + "/historial"), docente)).andExpect(jsonPath("$", hasSize(0)));

		// Ya registrada: el lote no la cambia ni la borra.
		mvc.perform(conToken(put(BASE + "/items/" + item + "/calificaciones"), docente, lote.formatted(ana, "11")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message", containsString("Modificar")));
		mvc.perform(conToken(put(BASE + "/items/" + item + "/calificaciones"), docente, lote.formatted(ana, "null")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.matriculaId == '%s')].notaNumerica".formatted(ana)).value(contains(10.0)));

		mvc.perform(conToken(put(ruta(item, ana)), docente, """
				{"notaNumerica": 11}"""))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(put(ruta(item, ana)), docente, """
				{"notaNumerica": 11, "motivo": "Error al sumar los puntos del examen"}"""))
				.andExpect(status().isOk());
		mvc.perform(conToken(get(ruta(item, ana) + "/historial"), docente))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].notaAnterior").value(10.0))
				.andExpect(jsonPath("$[0].motivo").value("Error al sumar los puntos del examen"));
	}

	@Test
	void elPromedioDelPeriodoCuentaComoCeroLoQueFaltaSinLetraYConElPesoCalificado() throws Exception {
		// Dos ítems de 100 % cada uno en la competencia: Ana solo tiene uno, la mitad del peso. Un ítem que nadie
		// tiene registrado no contaría y se vería «—».
		String examen = datos.item("Aritmética", 4, cantidad, "Examen HU-60");
		String practica = datos.item("Aritmética", 4, cantidad, "Práctica HU-60");
		datos.nota(examen, ana, 18);
		// Diego sí tiene la práctica: el ítem cuenta, y a Ana le falta registrarlo.
		datos.nota(practica, datos.matriculaDe("Diego José"), 12);
		String fila = "$.filas[?(@.matriculaId == '%s')].valores['%s:%s']";

		mvc.perform(conToken(get(BASE + "/reportes").param("tipo", "COMPETENCIA")
				.param("planCursoId", datos.plan("Aritmética")).param("seccionId", datos.seccionA())
				.param("periodoId", datos.periodo(4)).param("competenciaIds", cantidad), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath(fila.formatted(ana, cantidad, examen)).value(contains("18")))
				.andExpect(jsonPath(fila.formatted(ana, cantidad, practica)).value(contains("Por registrar")))
				// Lo que falta cuenta como 0: (18 × 100 + 0 × 100) / 200 = 9. Es un piso: solo sube al registrar.
				.andExpect(jsonPath(fila.formatted(ana, cantidad, "resultado")).value(contains("9")))
				.andExpect(jsonPath(fila.formatted(ana, cantidad, "resultado_estado"))
						.value(contains("Faltan registros (cuentan como 0) · 50 % del peso calificado")));
	}

	@Test
	void soloElDocenteModifica() throws Exception {
		String item = datos.item("Aritmética", 3, cantidad, "Examen HU-60");
		datos.nota(item, ana, 12);

		mvc.perform(conToken(put(ruta(item, ana)), token(COORDINADOR), """
				{"notaNumerica": 15, "motivo": "Revisión solicitada por el apoderado"}"""))
				.andExpect(status().isForbidden());
	}
}
