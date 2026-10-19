package com.galiledu.usuarios;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;

/** HU-8: carga masiva de usuarios desde Excel o CSV. */
class CargaMasivaIT extends PruebaIntegracion {
	private static final String ENCABEZADO = "Tipo de documento,N.° de documento,Apellido paterno,Apellido materno,"
			+ "Nombres,Fecha de nacimiento,Sexo,Celular,Correo electrónico,Exalumno (Sí / No),Rol\n";

	/** Fila 2 válida, 3 con DNI ya registrado, 4 con correo inválido, 5 repite el DNI de la fila 2. */
	private static final String CSV = ENCABEZADO
			+ "DNI,45678912,Pérez,Quispe,Ana Paula,12/04/1990,Femenino,999888777,aperez@galiledu.edu.pe,No,\"Docente, Tutor\"\n"
			+ "DNI,70000002,Torres,Vega,Ana,,,987000002,otra.ana@gmail.com,No,Docente\n"
			+ "DNI,45678913,Ríos,,Carlos,,,987654321,correo-malo,No,Coordinador\n"
			+ "DNI,45678912,Copia,,Repetida,,,987654320,copia@gmail.com,No,Apoderado\n";

	private String admin;

	@BeforeEach
	void iniciarSesion() throws Exception {
		admin = token(ADMIN);
	}

	private ResultActions enviar(String ruta, String nombre, byte[] contenido) throws Exception {
		return mvc
				.perform(conToken(multipart(ruta).file(new MockMultipartFile("archivo", nombre, "text/csv", contenido)),
						admin));
	}

	@Test
	void descargaLaPlantilla() throws Exception {
		mvc.perform(conToken(get("/usuarios/carga-masiva/plantilla"), admin))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", containsString("usuarios_galiledu.xlsx")));
	}

	@Test
	void validarInformaErroresPorFilaSinCrearNada() throws Exception {
		enviar("/usuarios/carga-masiva/validacion", "usuarios.csv", CSV.getBytes(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(4))
				.andExpect(jsonPath("$.validos").value(1))
				.andExpect(jsonPath("$.creados").value(0))
				.andExpect(jsonPath("$.errores[0].fila").value(3))
				.andExpect(jsonPath("$.errores[0].mensaje").value("DNI duplicado: 70000002 ya está registrado"))
				.andExpect(jsonPath("$.errores[1].fila").value(4))
				.andExpect(jsonPath("$.errores[1].mensaje").value("Ingresa un correo válido"))
				.andExpect(jsonPath("$.errores[2].mensaje").value(containsString("repetido en la fila 2")));

		mvc.perform(conToken(get("/usuarios").param("q", "aperez"), admin))
				.andExpect(jsonPath("$.meta.total").value(0));
	}

	@Test
	void cargarCreaSoloLasFilasValidas() throws Exception {
		enviar("/usuarios/carga-masiva", "usuarios.csv", CSV.getBytes(StandardCharsets.UTF_8))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.creados").value(1))
				.andExpect(jsonPath("$.errores.length()").value(3));

		mvc.perform(conToken(get("/usuarios").param("q", "aperez"), admin))
				.andExpect(jsonPath("$.meta.total").value(1))
				.andExpect(jsonPath("$.data[0].roles.length()").value(2));
	}

	@Test
	void leeLaPlantillaDeExcel() throws Exception {
		byte[] plantilla = mvc.perform(conToken(get("/usuarios/carga-masiva/plantilla"), admin))
				.andReturn().getResponse().getContentAsByteArray();
		enviar("/usuarios/carga-masiva/validacion", "usuarios_galiledu.xlsx", plantilla)
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.validos").value(1));
	}

	@Test
	void rechazaArchivosSinLasColumnas() throws Exception {
		enviar("/usuarios/carga-masiva/validacion", "x.csv", "Nombres\nAna\n".getBytes(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("Falta la columna")));
	}

	@Test
	void soloElAdministradorPuedeCargar() throws Exception {
		mvc.perform(conToken(multipart("/usuarios/carga-masiva")
				.file(new MockMultipartFile("archivo", "u.csv", "text/csv", CSV.getBytes(StandardCharsets.UTF_8))),
				token(DOCENTE))).andExpect(status().isForbidden());
	}
}
