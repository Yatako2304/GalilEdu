package com.galiledu.academico;

import static com.galiledu.academico.AmbientesDePrueba.PREFIJO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/**
 * HU-43: {@code POST /ambientes} (Figma, modal "Nuevo ambiente físico", 857:11500), el único endpoint que confirma de
 * verdad: {@code crear} usa una transacción por intento y el rollback de una prueba no la deshace. Por eso esta clase
 * <b>no usa transacción de prueba</b> ({@code NOT_SUPPORTED} anula la de {@link PruebaIntegracion}, que no se
 * modifica) y limpia lo que crea con {@link AmbientesDePrueba}: solo las filas nuevas, nunca las de la semilla, y deja
 * libres A-206 y E-001. Los 401, 403 y 400 de este endpoint también están aquí aunque no deberían crear nada: si la
 * seguridad o una validación fallara, la fila quedaría confirmada y esta clase la borraría.
 * <p>
 * Iniciar sesión sin transacción de prueba <b>confirma</b> {@code ultimo_acceso} de ese usuario. Se guarda su valor al
 * empezar y se restaura al terminar, para no dejar rastros en los datos de prueba compartidos.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AmbienteCrearIT extends PruebaIntegracion {
	private static final String[] USUARIOS = {ADMIN, PERSONAL_ADMINISTRATIVO, DOCENTE, COORDINADOR};

	@Autowired
	private JdbcClient jdbc;

	private Set<UUID> idsAlEmpezar;
	private List<AmbientesDePrueba.Acceso> accesosAlEmpezar;
	private String personal;

	// ---------- Limpieza ----------

	@BeforeEach
	void empezarLimpio() throws Exception {
		idsAlEmpezar = AmbientesDePrueba.empezarLimpio(jdbc);
		accesosAlEmpezar = AmbientesDePrueba.accesos(jdbc, USUARIOS);
		assertThat(accesosAlEmpezar).as("los 4 usuarios de prueba").hasSize(USUARIOS.length);
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	@AfterEach
	void limpiar() {
		AmbientesDePrueba.borrarLoCreadoDesde(jdbc, idsAlEmpezar);
		AmbientesDePrueba.restaurarAccesos(jdbc, accesosAlEmpezar, USUARIOS);
	}

	// ---------- Apoyo ----------

	private int contarAmbientes() {
		return jdbc.sql("SELECT count(*)::int FROM configuracion.ambiente").query(Integer.class).single();
	}

	private static String solicitud(String tipo, String nombre, Object aforo, String recursosJson) {
		return """
				{"tipo":"%s","nombre":"%s","aforoMaximo":%s,"recursos":%s}""".formatted(tipo, nombre, aforo,
				recursosJson);
	}

	// ---------- Crear ----------

	@DisplayName("Crea un aula con el código generado y los datos normalizados")
	@Test
	void creaUnAulaConElCodigoGeneradoYLosDatosNormalizados() throws Exception {
		mvc.perform(conToken(post("/ambientes"), personal, """
				{"tipo":"AULA_REGULAR","nombre":"  %sAula   nueva ","aforoMaximo":30,
				 "recursos":[" Pizarra ","","Proyector","proyector"]}""".formatted(PREFIJO)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.codigo").value("A-206")) // el mayor de la semilla es A-205
				.andExpect(jsonPath("$.nombre").value(PREFIJO + "Aula nueva"))
				.andExpect(jsonPath("$.tipo").value("AULA_REGULAR"))
				.andExpect(jsonPath("$.aforoMaximo").value(30))
				.andExpect(jsonPath("$.recursos", contains("Pizarra", "Proyector")))
				.andExpect(jsonPath("$.estado").value("ACTIVO"));
	}

	@DisplayName("El Administrador también crea ambientes")
	@Test
	void elAdministradorTambienCreaAmbientes() throws Exception {
		mvc.perform(conToken(post("/ambientes"), token(ADMIN),
				solicitud("ESPACIO_ESPECIAL", PREFIJO + "Del admin", 60, "[]")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.codigo").value("E-001"))
				.andExpect(jsonPath("$.estado").value("ACTIVO"));
	}

	@DisplayName("El alta queda confirmada y aparec en el listado y en las tarjetas")
	@Test
	void elAltaQuedaConfirmadaYAparecEnElListadoYEnLasTarjetas() throws Exception {
		String antes = mvc.perform(conToken(get("/ambientes/resumen"), personal)).andReturn().getResponse()
				.getContentAsString();
		int activosAntes = JsonPath.read(antes, "$.activos");
		int aforoAntes = JsonPath.read(antes, "$.aforoTotal");

		mvc.perform(conToken(post("/ambientes"), personal, solicitud("AULA_REGULAR", PREFIJO + "Confirmada", 25, "[]")))
				.andExpect(status().isCreated());

		// Sin transacción de prueba, JdbcClient lee con su propia conexión y solo ve lo confirmado.
		assertThat(jdbc.sql("SELECT codigo FROM configuracion.ambiente WHERE nombre = :nombre")
				.param("nombre", PREFIJO + "Confirmada").query(String.class).list()).containsExactly("A-206");
		mvc.perform(conToken(get("/ambientes").param("q", PREFIJO + "Confirmada"), personal))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].codigo").value("A-206"));
		mvc.perform(conToken(get("/ambientes/resumen"), personal))
				.andExpect(jsonPath("$.activos").value(activosAntes + 1))
				.andExpect(jsonPath("$.aforoTotal").value(aforoAntes + 25));
	}

	@DisplayName("El código que envie el cliente se ignora porque lo genera el sistema")
	@Test
	void elCodigoQueEnvieElClienteSeIgnoraPorqueLoGeneraElSistema() throws Exception {
		mvc.perform(conToken(post("/ambientes"), personal,
				"""
						{"tipo":"AULA_REGULAR","nombre":"%sCon código propio","aforoMaximo":30,"codigo":"A-999","recursos":[]}"""
						.formatted(PREFIJO)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.codigo").value("A-206"));
		assertThat(jdbc.sql("SELECT count(*)::int FROM configuracion.ambiente WHERE codigo = 'A-999'")
				.query(Integer.class).single()).isZero();
	}

	@DisplayName("Cada tipo lleva su propio prefijo y correlativo")
	@Test
	void cadaTipoLlevaSuPropioPrefijoYCorrelativo() throws Exception {
		mvc.perform(
				conToken(post("/ambientes"), personal, solicitud("ESPACIO_ESPECIAL", PREFIJO + "Auditorio", 200, "[]")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.codigo").value("E-001"));
		mvc.perform(conToken(post("/ambientes"), personal, solicitud("AULA_REGULAR", PREFIJO + "Aula 1", 30, "[]")))
				.andExpect(jsonPath("$.codigo").value("A-206"));
		mvc.perform(conToken(post("/ambientes"), personal, solicitud("ESPACIO_ESPECIAL", PREFIJO + "Taller", 15, "[]")))
				.andExpect(jsonPath("$.codigo").value("E-002"));
		mvc.perform(conToken(post("/ambientes"), personal, solicitud("AULA_REGULAR", PREFIJO + "Aula 2", 30, "[]")))
				.andExpect(jsonPath("$.codigo").value("A-207"));
	}

	@DisplayName("Un espacio especial admite más de45 y un aula regular exactamente45")
	@Test
	void unEspacioEspecialAdmiteMasDe45YUnAulaRegularExactamente45() throws Exception {
		mvc.perform(conToken(post("/ambientes"), personal, solicitud("ESPACIO_ESPECIAL", PREFIJO + "Losa", 180, "[]")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.aforoMaximo").value(180));
		mvc.perform(conToken(post("/ambientes"), personal, solicitud("AULA_REGULAR", PREFIJO + "Aula de 45", 45, "[]")))
				.andExpect(status().isCreated());
	}

	// ---------- Rechazos ----------

	@DisplayName("Un nombre repetido da409 sin distinguir mayúsculas ni espacios y no crea nada")
	@Test
	void unNombreRepetidoDa409SinDistinguirMayusculasNiEspaciosYNoCreaNada() throws Exception {
		int antes = contarAmbientes();

		mvc.perform(conToken(post("/ambientes"), personal, solicitud("AULA_REGULAR", "aula 101", 30, "[]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ya existe un ambiente llamado \"aula 101\"."));
		mvc.perform(conToken(post("/ambientes"), personal, solicitud("ESPACIO_ESPECIAL", "  AULA    101  ", 30, "[]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ya existe un ambiente llamado \"AULA 101\"."));
		// Un ambiente inactivo (A-205) también ocupa su nombre: se puede reactivar.
		mvc.perform(conToken(post("/ambientes"), personal, solicitud("AULA_REGULAR", "AULA 205", 30, "[]")))
				.andExpect(status().isConflict());

		assertThat(contarAmbientes()).isEqualTo(antes);
	}

	@DisplayName("Un aula de46 da400 con el texto de figma y no crea nada")
	@Test
	void unAulaDe46Da400ConElTextoDeFigmaYNoCreaNada() throws Exception {
		int antes = contarAmbientes();

		mvc.perform(conToken(post("/ambientes"), personal, solicitud("AULA_REGULAR", PREFIJO + "Aula de 46", 46, "[]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Un aula regular admite como máximo 45."));

		assertThat(contarAmbientes()).isEqualTo(antes);
	}

	@DisplayName("Los datos invalidos dan400 con su mensaje y no crean nada")
	@Test
	void losDatosInvalidosDan400ConSuMensajeYNoCreanNada() throws Exception {
		int antes = contarAmbientes();

		mvc.perform(conToken(post("/ambientes"), personal, """
				{"tipo":"AULA_REGULAR","nombre":"   ","aforoMaximo":30}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.nombre").value("Ingresa el nombre del ambiente."));
		mvc.perform(conToken(post("/ambientes"), personal, """
				{"nombre":"%sSin tipo","aforoMaximo":30}""".formatted(PREFIJO)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.tipo").value("Elige el tipo de ambiente."));
		mvc.perform(conToken(post("/ambientes"), personal, """
				{"tipo":"AULA_REGULAR","nombre":"%sSin aforo"}""".formatted(PREFIJO)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.aforoMaximo").value("Ingresa el aforo máximo."));
		mvc.perform(conToken(post("/ambientes"), personal, solicitud("AULA_REGULAR", PREFIJO + "Aforo cero", 0, "[]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.aforoMaximo").value("El aforo debe ser mayor que 0."));
		mvc.perform(conToken(post("/ambientes"), personal,
				solicitud("AULA_REGULAR", PREFIJO + "Recurso largo", 30, "[\"" + "r".repeat(61) + "\"]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Cada recurso admite como máximo 60 caracteres."));
		mvc.perform(conToken(post("/ambientes"), personal, solicitud("AULA_REGULAR", "a".repeat(81), 30, "[]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El nombre admite como máximo 80 caracteres."));
		// Un tipo que no existe y un cuerpo que no es JSON.
		mvc.perform(conToken(post("/ambientes"), personal, solicitud("AULA", PREFIJO + "Tipo raro", 30, "[]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La solicitud tiene un formato inválido"));
		mvc.perform(conToken(post("/ambientes"), personal, "{no es json"))
				.andExpect(status().isBadRequest());

		assertThat(contarAmbientes()).isEqualTo(antes);
	}

	// ---------- Permisos ----------

	@DisplayName("Sin sesión da401 y no crea nada")
	@Test
	void sinSesionDa401YNoCreaNada() throws Exception {
		int antes = contarAmbientes();

		mvc.perform(post("/ambientes").contentType("application/json")
				.content(solicitud("AULA_REGULAR", PREFIJO + "Sin sesión", 30, "[]")))
				.andExpect(status().isUnauthorized());

		assertThat(contarAmbientes()).isEqualTo(antes);
	}

	@DisplayName("Otros roles reciben403 y no crean nada")
	@Test
	void otrosRolesReciben403YNoCreanNada() throws Exception {
		int antes = contarAmbientes();

		for (String usuario : new String[]{DOCENTE, COORDINADOR}) {
			mvc.perform(conToken(post("/ambientes"), token(usuario),
					solicitud("AULA_REGULAR", PREFIJO + "Sin permiso", 30, "[]"))).andExpect(status().isForbidden());
		}

		assertThat(contarAmbientes()).isEqualTo(antes);
	}
}
