package com.galiledu.academico.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** HU-43: el código de un ambiente lo genera el sistema: prefijo del tipo y el mayor número existente + 1. */
class CodigoAmbienteTests {
	/** Los códigos de los datos de prueba. */
	private static final List<String> SEMILLA = List.of("A-101", "A-102", "A-201", "A-205", "LAB-01", "LAB-02",
			"BIB-01", "AUD-01");

	@DisplayName("Con la semilla el primer aula es a206")
	@Test
	void conLaSemillaElPrimerAulaEsA206() {
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, SEMILLA).valor()).isEqualTo("A-206");
	}

	@DisplayName("Con la semilla el primer espacio especial es e001")
	@Test
	void conLaSemillaElPrimerEspacioEspecialEsE001() {
		// LAB-01, BIB-01 y AUD-01 no usan el prefijo E: no cuentan.
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.ESPACIO_ESPECIAL, SEMILLA).valor()).isEqualTo("E-001");
	}

	@DisplayName("Suma uno al mayor con el prefijo")
	@Test
	void sumaUnoAlMayorConElPrefijo() {
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, List.of("A-300", "A-302", "A-017")).valor())
				.isEqualTo("A-303");
	}

	@DisplayName("No depende del orden de la lista")
	@Test
	void noDependeDelOrdenDeLaLista() {
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, List.of("A-009", "A-010", "A-002")).valor())
				.isEqualTo("A-011");
	}

	@DisplayName("Sin codigos previos empieza en uno")
	@Test
	void sinCodigosPreviosEmpiezaEnUno() {
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, List.of()).valor()).isEqualTo("A-001");
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.ESPACIO_ESPECIAL, List.of()).valor()).isEqualTo("E-001");
	}

	@DisplayName("Una lista nula equivale a sin codigos")
	@Test
	void unaListaNulaEquivaleASinCodigos() {
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, null).valor()).isEqualTo("A-001");
	}

	@DisplayName("Con solo codigos de otro prefijo empieza en uno")
	@Test
	void conSoloCodigosDeOtroPrefijoEmpiezaEnUno() {
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, List.of("E-005", "E-006")).valor())
				.isEqualTo("A-001");
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.ESPACIO_ESPECIAL, List.of("A-005")).valor())
				.isEqualTo("E-001");
	}

	@DisplayName("Pasa de tres cifras sin cortarse")
	@Test
	void pasaDeTresCifrasSinCortarse() {
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, List.of("A-999")).valor())
				.isEqualTo("A-1000");
	}

	@DisplayName("Rellena con ceros hasta tres cifras")
	@Test
	void rellenaConCerosHastaTresCifras() {
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, List.of("A-1")).valor()).isEqualTo("A-002");
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, List.of("A-09")).valor()).isEqualTo("A-010");
	}

	@DisplayName("Ignora los codigos que no siguen el formato")
	@Test
	void ignoraLosCodigosQueNoSiguenElFormato() {
		// Prefijo más largo, sin número, número con letras, minúscula, otro separador, espacios, vacío y nulo.
		List<String> hostiles = Arrays.asList("AA-900", "A-", "A-12X", "a-800", "A_700", " A-600", "A-500 ", "",
				null, "A-1-2", "A--5", "A-+7", "A-٣٠٠");
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, hostiles).valor()).isEqualTo("A-001");
	}

	@DisplayName("Un número demasiado grande no rompe el cálculo")
	@Test
	void unNumeroDemasiadoGrandeNoRompeElCalculo() {
		// Más de 9 cifras: se ignora (no cabe en un int); los demás códigos siguen contando.
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, List.of("A-99999999999", "A-004")).valor())
				.isEqualTo("A-005");
	}

	@DisplayName("Sin tipo lo rechaza")
	@Test
	void sinTipoLoRechaza() {
		assertThatThrownBy(() -> CodigoAmbiente.siguiente(null, SEMILLA)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Elige el tipo de ambiente.");
	}

	@DisplayName("Conserva el valor y se muestra como texto")
	@Test
	void conservaElValorYSeMuestraComoTexto() {
		var codigo = CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, SEMILLA);
		assertThat(codigo).hasToString("A-206");
		assertThat(codigo).isEqualTo(new CodigoAmbiente("A-206"));
	}

	@DisplayName("Un código con formato inválido no se puede construir")
	@ParameterizedTest
	@ValueSource(strings = {"", " ", "A", "A-", "-001", "a-001", "A 001", "A-00X", "1-001", "A-001\n"})
	void unCodigoConFormatoInvalidoNoSePuedeConstruir(String valor) {
		assertThatThrownBy(() -> new CodigoAmbiente(valor)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("El código del ambiente no tiene un formato válido.");
	}

	@DisplayName("Un código nulo no se puede construir")
	@Test
	void unCodigoNuloNoSePuedeConstruir() {
		assertThatThrownBy(() -> new CodigoAmbiente(null)).isInstanceOf(IllegalArgumentException.class);
	}
}
