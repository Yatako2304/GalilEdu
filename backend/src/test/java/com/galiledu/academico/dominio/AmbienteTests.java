package com.galiledu.academico.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** HU-43: las reglas de forma de un ambiente físico (nombre, aforo, recursos, estado, código y tipo inmutables). */
class AmbienteTests {
	private static final CodigoAmbiente CODIGO = new CodigoAmbiente("A-206");

	private static Ambiente aula() {
		return Ambiente.crear(TipoAmbiente.AULA_REGULAR, CODIGO, "Aula 206", 30, List.of("Pizarra", "Proyector"));
	}

	private static Ambiente espacio() {
		return Ambiente.crear(TipoAmbiente.ESPACIO_ESPECIAL, new CodigoAmbiente("E-001"), "Auditorio", 180, List.of());
	}

	// ---------- Crear ----------

	@Test
	void creaUnAmbienteActivoConLosDatosNormalizados() {
		var ambiente = Ambiente.crear(TipoAmbiente.AULA_REGULAR, CODIGO, "  Aula   206  ", 30,
				Arrays.asList(" Pizarra ", "", "  ", null, "Proyector"));

		assertThat(ambiente.getCodigo()).isEqualTo("A-206");
		assertThat(ambiente.getNombre()).isEqualTo("Aula 206");
		assertThat(ambiente.getTipo()).isEqualTo(TipoAmbiente.AULA_REGULAR);
		assertThat(ambiente.getAforoMaximo()).isEqualTo(30);
		assertThat(ambiente.getRecursos()).containsExactly("Pizarra", "Proyector");
		assertThat(ambiente.getEstado()).isEqualTo(EstadoAmbiente.ACTIVO);
		assertThat(ambiente.estaActivo()).isTrue();
	}

	@Test
	void unEspacioEspecialAdmiteMasDe45() {
		assertThat(espacio().getAforoMaximo()).isEqualTo(180);
	}

	@Test
	void unAulaRegularAdmiteExactamente45() {
		assertThatCode(() -> Ambiente.crear(TipoAmbiente.AULA_REGULAR, CODIGO, "Aula 206", 45, List.of()))
				.doesNotThrowAnyException();
	}

	@Test
	void aceptaUnAforoDeUno() {
		assertThatCode(() -> Ambiente.crear(TipoAmbiente.AULA_REGULAR, CODIGO, "Aula 206", 1, List.of()))
				.doesNotThrowAnyException();
	}

	@Test
	void sinTipoOSinCodigoLoRechaza() {
		assertThatThrownBy(() -> Ambiente.crear(null, CODIGO, "Aula", 30, List.of()))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Elige el tipo de ambiente.");
		assertThatThrownBy(() -> Ambiente.crear(TipoAmbiente.AULA_REGULAR, null, "Aula", 30, List.of()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// ---------- Nombre ----------

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", " ", "     ", "\t", "\n", " \t \n ", " ", "  "})
	void rechazaUnNombreVacioOSoloEspacios(String nombre) {
		assertThatThrownBy(() -> Ambiente.crear(TipoAmbiente.AULA_REGULAR, CODIGO, nombre, 30, List.of()))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Ingresa el nombre del ambiente.");
	}

	@Test
	void elNombreAdmiteHasta80Caracteres() {
		assertThatCode(() -> Ambiente.crear(TipoAmbiente.AULA_REGULAR, CODIGO, "a".repeat(80), 30, List.of()))
				.doesNotThrowAnyException();
	}

	@Test
	void rechazaUnNombreDe81Caracteres() {
		assertThatThrownBy(() -> Ambiente.crear(TipoAmbiente.AULA_REGULAR, CODIGO, "a".repeat(81), 30, List.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("El nombre admite como máximo 80 caracteres.");
	}

	@Test
	void elLargoDelNombreSeMideDespuesDeRecortar() {
		// 80 letras con espacios sobrantes alrededor y entre palabras: no pasa de 80 una vez normalizado.
		String nombre = "  " + "a".repeat(39) + "   " + "b".repeat(40) + "  ";
		assertThat(Ambiente.normalizarNombre(nombre)).hasSize(80);
	}

	@Test
	void elLargoSeCuentaEnPuntosDeCodigoComoLaBase() {
		// 80 emojis son 160 unidades de UTF-16 pero 80 caracteres para VARCHAR(80).
		assertThatCode(() -> Ambiente.normalizarNombre("😀".repeat(80))).doesNotThrowAnyException();
		assertThatThrownBy(() -> Ambiente.normalizarNombre("😀".repeat(81)))
				.hasMessage("El nombre admite como máximo 80 caracteres.");
	}

	@Test
	void reduceLosEspaciosRepetidosPeroConservaMayusculasYTildes() {
		assertThat(Ambiente.normalizarNombre("  Laboratorio \t de   CÓMPUTO\n")).isEqualTo("Laboratorio de CÓMPUTO");
	}

	@ParameterizedTest
	@ValueSource(strings = {"Aula\u0000101", "Aula\u0007", "\u001BAula"})
	void rechazaCaracteresDeControlEnElNombre(String nombre) {
		assertThatThrownBy(() -> Ambiente.crear(TipoAmbiente.AULA_REGULAR, CODIGO, nombre, 30, List.of()))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("El texto contiene caracteres no válidos.");
	}

	// ---------- Aforo ----------

	@ParameterizedTest
	@ValueSource(ints = {0, -1, -45, Integer.MIN_VALUE})
	void rechazaUnAforoQueNoEsMayorQueCero(int aforo) {
		assertThatThrownBy(
				() -> Ambiente.crear(TipoAmbiente.ESPACIO_ESPECIAL, new CodigoAmbiente("E-001"), "Sala", aforo,
						List.of()))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("El aforo debe ser mayor que 0.");
	}

	@ParameterizedTest
	@ValueSource(ints = {46, 47, 100, 32767})
	void rechazaUnAulaRegularDeMasDe45(int aforo) {
		assertThatThrownBy(() -> Ambiente.crear(TipoAmbiente.AULA_REGULAR, CODIGO, "Aula 206", aforo, List.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Un aula regular admite como máximo 45 estudiantes.");
	}

	@Test
	void unAforoMayorQueLaColumnaSeRechazaEnVezDeTruncarse() {
		// (short) 32768 sería -32768 y (short) 65537 sería 1: el cast silencioso guardaría un aforo equivocado.
		for (int aforo : new int[]{32768, 65537, Integer.MAX_VALUE}) {
			assertThatThrownBy(() -> Ambiente.crear(TipoAmbiente.ESPACIO_ESPECIAL, new CodigoAmbiente("E-001"), "Sala",
					aforo, List.of())).isInstanceOf(IllegalArgumentException.class)
					.hasMessage("El aforo no puede ser mayor que 32767.");
		}
	}

	// ---------- Recursos ----------

	@Test
	void quitaVaciosYRepetidosSinDistinguirMayusculasYConservaElOrdenYElPrimero() {
		var recursos = Ambiente.normalizarRecursos(
				Arrays.asList("Proyector", "  pizarra  ", "PROYECTOR", "", " ", null, "Pizarra", "Parlantes"));
		assertThat(recursos).containsExactly("Proyector", "pizarra", "Parlantes");
	}

	@Test
	void unRepetidoSeDetectaAunConEspaciosInternosDistintos() {
		assertThat(Ambiente.normalizarRecursos(List.of("35 computadoras", "35   computadoras", "35 computadoras")))
				.containsExactly("35 computadoras");
	}

	@Test
	void sinTildesNoSeNormalizan() {
		assertThat(Ambiente.normalizarRecursos(List.of("Estanterías", "Estanterias"))).hasSize(2);
	}

	@Test
	void unaListaNulaOVaciaEsNingunRecurso() {
		assertThat(Ambiente.normalizarRecursos(null)).isEmpty();
		assertThat(Ambiente.normalizarRecursos(List.of())).isEmpty();
		assertThat(Ambiente.normalizarRecursos(Arrays.asList(null, "", "  "))).isEmpty();
	}

	@Test
	void unRecursoAdmiteHasta60Caracteres() {
		assertThat(Ambiente.normalizarRecursos(List.of("r".repeat(60)))).hasSize(1);
	}

	@Test
	void rechazaUnRecursoDe61Caracteres() {
		assertThatThrownBy(() -> Ambiente.normalizarRecursos(List.of("Pizarra", "r".repeat(61))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Cada recurso admite como máximo 60 caracteres.");
	}

	@Test
	void rechazaUnRecursoLargoAunSiOtroIgualYaEstabaEnLaLista() {
		// El largo se valida antes de buscar repetidos: el segundo largo no se descarta en silencio.
		assertThatThrownBy(() -> Ambiente.normalizarRecursos(List.of("r".repeat(60), "R".repeat(61))))
				.hasMessage("Cada recurso admite como máximo 60 caracteres.");
	}

	@Test
	void rechazaCaracteresDeControlEnUnRecurso() {
		assertThatThrownBy(() -> Ambiente.normalizarRecursos(List.of("Proyector\u0000")))
				.hasMessage("El texto contiene caracteres no válidos.");
	}

	@Test
	void laListaOriginalNoSeModificaNiSeComparte() {
		var original = new ArrayList<>(List.of("Pizarra", "Pizarra"));
		var ambiente = Ambiente.crear(TipoAmbiente.AULA_REGULAR, CODIGO, "Aula 206", 30, original);

		original.add("Proyector");

		assertThat(ambiente.getRecursos()).containsExactly("Pizarra");
		assertThat(original).containsExactly("Pizarra", "Pizarra", "Proyector");
	}

	@Test
	void losRecursosDeLaEntidadNoSePuedenModificarPorFuera() {
		var recursos = aula().getRecursos();
		assertThatThrownBy(() -> recursos.add("Intruso")).isInstanceOf(UnsupportedOperationException.class);
	}

	// ---------- Editar ----------

	@Test
	void editaNombreAforoYRecursosYNoTocaCodigoNiTipoNiEstado() {
		var ambiente = aula();
		ambiente.desactivar();

		ambiente.editar("  Aula   207 ", 40, Arrays.asList("Proyector", "proyector", "Parlantes"));

		assertThat(ambiente.getNombre()).isEqualTo("Aula 207");
		assertThat(ambiente.getAforoMaximo()).isEqualTo(40);
		assertThat(ambiente.getRecursos()).containsExactly("Proyector", "Parlantes");
		assertThat(ambiente.getCodigo()).isEqualTo("A-206");
		assertThat(ambiente.getTipo()).isEqualTo(TipoAmbiente.AULA_REGULAR);
		assertThat(ambiente.getEstado()).isEqualTo(EstadoAmbiente.INACTIVO);
	}

	@Test
	void editarConUnAulaRegularRespetaElLimiteDe45YUnEspacioEspecialNo() {
		assertThatThrownBy(() -> aula().editar("Aula 206", 46, List.of()))
				.hasMessage("Un aula regular admite como máximo 45 estudiantes.");
		var auditorio = espacio();
		auditorio.editar("Auditorio", 300, List.of());
		assertThat(auditorio.getAforoMaximo()).isEqualTo(300);
	}

	@Test
	void sinNingunRecursoPuedeQuedarVacio() {
		var ambiente = aula();
		ambiente.editar("Aula 206", 30, List.of());
		assertThat(ambiente.getRecursos()).isEmpty();
	}

	@Test
	void siUnDatoEsInvalidoNoCambiaNingunoDeLosOtros() {
		// Valida antes de modificar: un nombre válido y un aforo inválido no deja el nombre a medio cambiar.
		var ambiente = aula();
		assertThatThrownBy(() -> ambiente.editar("Aula nueva", 0, List.of("Parlantes")))
				.hasMessage("El aforo debe ser mayor que 0.");
		assertThat(ambiente.getNombre()).isEqualTo("Aula 206");
		assertThat(ambiente.getAforoMaximo()).isEqualTo(30);
		assertThat(ambiente.getRecursos()).containsExactly("Pizarra", "Proyector");

		// Y el orden inverso: nombre y aforo válidos, un recurso de 61 caracteres.
		assertThatThrownBy(() -> ambiente.editar("Aula nueva", 20, List.of("Parlantes", "r".repeat(61))))
				.hasMessage("Cada recurso admite como máximo 60 caracteres.");
		assertThat(ambiente.getNombre()).isEqualTo("Aula 206");
		assertThat(ambiente.getAforoMaximo()).isEqualTo(30);
		assertThat(ambiente.getRecursos()).containsExactly("Pizarra", "Proyector");
	}

	@Test
	void editarConNombreVacioNoCambiaNada() {
		var ambiente = aula();
		assertThatThrownBy(() -> ambiente.editar("   ", 20, List.of())).hasMessage("Ingresa el nombre del ambiente.");
		assertThat(ambiente.getNombre()).isEqualTo("Aula 206");
		assertThat(ambiente.getAforoMaximo()).isEqualTo(30);
	}

	// ---------- Estado ----------

	@Test
	void desactivarYReactivarCambianElEstado() {
		var ambiente = aula();

		ambiente.desactivar();
		assertThat(ambiente.getEstado()).isEqualTo(EstadoAmbiente.INACTIVO);
		assertThat(ambiente.estaActivo()).isFalse();

		ambiente.reactivar();
		assertThat(ambiente.getEstado()).isEqualTo(EstadoAmbiente.ACTIVO);
		assertThat(ambiente.estaActivo()).isTrue();
	}

	@Test
	void desactivarYReactivarSonIdempotentes() {
		var ambiente = aula();

		assertThatCode(ambiente::reactivar).doesNotThrowAnyException(); // ya estaba activo
		assertThat(ambiente.getEstado()).isEqualTo(EstadoAmbiente.ACTIVO);

		ambiente.desactivar();
		assertThatCode(ambiente::desactivar).doesNotThrowAnyException(); // ya estaba inactivo
		assertThat(ambiente.getEstado()).isEqualTo(EstadoAmbiente.INACTIVO);
	}

	@Test
	void desactivarNoTocaLosDemasDatos() {
		var ambiente = aula();
		ambiente.desactivar();
		assertThat(ambiente.getNombre()).isEqualTo("Aula 206");
		assertThat(ambiente.getAforoMaximo()).isEqualTo(30);
		assertThat(ambiente.getRecursos()).containsExactly("Pizarra", "Proyector");
	}

	// ---------- Código y tipo inmutables ----------

	@Test
	void laClaseNoOfreceFormaDeCambiarElCodigoNiElTipo() {
		var metodosPublicos = Arrays.stream(Ambiente.class.getDeclaredMethods())
				.filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
				.map(java.lang.reflect.Method::getName)
				.toList();
		assertThat(metodosPublicos).doesNotContain("setCodigo", "setTipo", "cambiarCodigo", "cambiarTipo");
	}

	@Test
	void lasColumnasDeCodigoYTipoNoSeActualizan() throws NoSuchFieldException {
		assertThat(Ambiente.class.getDeclaredField("codigo").getAnnotation(Column.class).updatable()).isFalse();
		assertThat(Ambiente.class.getDeclaredField("tipo").getAnnotation(Column.class).updatable()).isFalse();
	}

	@Test
	void mapeaLaTablaDeAmbientesEnElEsquemaDeConfiguracion() {
		var tabla = Ambiente.class.getAnnotation(Table.class);
		assertThat(tabla.schema()).isEqualTo("configuracion");
		assertThat(tabla.name()).isEqualTo("ambiente");
	}
}
