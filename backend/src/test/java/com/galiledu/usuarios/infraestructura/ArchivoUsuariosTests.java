package com.galiledu.usuarios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.galiledu.compartido.web.ExcepcionNegocio;

class ArchivoUsuariosTests {
	private final ArchivoUsuarios archivos = new ArchivoUsuarios();

	private List<ArchivoUsuarios.Fila> csv(String texto) {
		return archivos.leer("usuarios.csv", new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void reconoceEncabezadosSinImportarTildesNiMayusculas() {
		assertThat(ArchivoUsuarios.clave("N.° de documento")).isEqualTo("numeroDocumento");
		assertThat(ArchivoUsuarios.clave("CORREO ELECTRONICO")).isEqualTo("correo");
		assertThat(ArchivoUsuarios.clave("Exalumno (Sí / No)")).isEqualTo("esExalumno");
		assertThat(ArchivoUsuarios.clave("Columna inventada")).isNull();
	}

	@Test
	void leeCsvConPuntoYComaYComillas() {
		var filas = csv("""
				\uFEFFTipo de documento;N.° de documento;Apellido paterno;Nombres;Celular;Correo electrónico;Rol
				DNI;41234567;Quispe;"Jorge; Luis";987111222;jorge@gmail.com;Docente
				""");
		assertThat(filas).hasSize(1);
		assertThat(filas.get(0).numero()).isEqualTo(2);
		assertThat(filas.get(0).valor("nombres")).isEqualTo("Jorge; Luis");
		assertThat(filas.get(0).valor("roles")).isEqualTo("Docente");
	}

	@Test
	void ignoraFilasVacias() {
		var filas = csv("""
				Tipo de documento,N.° de documento,Apellido paterno,Nombres,Celular,Correo electrónico,Rol
				,,,,,,
				DNI,41234567,Quispe,Jorge,987111222,jorge@gmail.com,Docente
				""");
		assertThat(filas).singleElement().extracting(ArchivoUsuarios.Fila::numero).isEqualTo(3);
	}

	@Test
	void exigeLasColumnasObligatorias() {
		assertThatThrownBy(() -> csv("Nombres,Correo electrónico\nJorge,j@gmail.com\n"))
				.isInstanceOf(ExcepcionNegocio.class)
				.hasMessageContaining("Falta la columna");
	}

	@Test
	void rechazaFormatosNoSoportados() {
		assertThatThrownBy(() -> archivos.leer("usuarios.txt", new ByteArrayInputStream(new byte[0])))
				.isInstanceOf(ExcepcionNegocio.class)
				.hasMessage("El archivo debe ser .xlsx, .xls o .csv");
	}

	@Test
	void laPlantillaSePuedeVolverALeer() {
		var filas = archivos.leer("plantilla.xlsx", new ByteArrayInputStream(archivos.plantilla()));
		assertThat(filas).singleElement().satisfies(f -> {
			assertThat(f.valor("numeroDocumento")).isEqualTo("41234567");
			assertThat(f.valor("fechaNacimiento")).isEqualTo("03/08/1988");
			assertThat(f.valor("roles")).isEqualTo("Docente");
		});
	}
}
