package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.galiledu.horarios.dominio.EstadoReserva;
import com.galiledu.horarios.dominio.ReservaAmbiente;
import com.galiledu.horarios.infraestructura.ReservaAmbienteRepositorio;

/**
 * HU-48: la entidad {@link ReservaAmbiente} y las consultas de superposición del repositorio, contra la BD real. Todas
 * usan la misma sección, ambiente y responsable, y cada prueba cambia solo lo que quiere probar.
 */
class ReservaAmbienteRepositorioIT extends PruebaReservas {
	private static final LocalDate FECHA = LocalDate.of(2027, 3, 8);

	@Autowired
	private ReservaAmbienteRepositorio repositorio;

	private UUID laboratorio;
	private UUID otroLaboratorio;
	private UUID seccion;
	private UUID otraSeccion;
	private UUID responsable;
	private UUID otroResponsable;

	private void preparar() {
		laboratorio = espacio("TMP-LAB", "ACTIVO", 30);
		otroLaboratorio = espacio("TMP-LAB2", "ACTIVO", 30);
		seccion = seccion("SEC-1", "R1", null, null);
		otraSeccion = seccion("SEC-1", "R2", null, null);
		responsable = docente();
		otroResponsable = coordinador();
	}

	private ReservaAmbiente guardar(UUID ambiente, UUID seccionId, UUID responsableId, LocalDate fecha, String inicio,
			String fin) {
		return repositorio.save(ReservaAmbiente.crear(ambiente, seccionId, responsableId, "Experimento", fecha,
				LocalTime.parse(inicio), LocalTime.parse(fin), coordinador()));
	}

	private static LocalTime hora(String texto) {
		return LocalTime.parse(texto);
	}

	@DisplayName("Guarda la reserva activa con sus datos y su versión cero")
	@Test
	void guardaLaReservaActivaConSusDatosYSuVersionCero() {
		preparar();

		ReservaAmbiente guardada = repositorio.saveAndFlush(
				ReservaAmbiente.crear(laboratorio, seccion, responsable, "  Experimento de química  ", FECHA,
						hora("08:00"), hora("09:30"), coordinador()));

		ReservaAmbiente leida = repositorio.findById(guardada.getId()).orElseThrow();
		assertThat(leida.getEstado()).isEqualTo(EstadoReserva.ACTIVA);
		assertThat(leida.getActividad()).isEqualTo("Experimento de química");
		assertThat(leida.getFecha()).isEqualTo(FECHA);
		assertThat(leida.getHoraInicio()).isEqualTo(hora("08:00"));
		assertThat(leida.getHoraFin()).isEqualTo(hora("09:30"));
		assertThat(leida.getCreadoPor()).isEqualTo(coordinador());
		assertThat(leida.getVersion()).isZero();
		assertThat(leida.getCreadoEn()).isNotNull();
	}

	@DisplayName("Cada cambio sube la versión y cancelar conserva la fila")
	@Test
	void cadaCambioSubeLaVersionYCancelarConservaLaFila() {
		preparar();
		ReservaAmbiente reserva = repositorio.saveAndFlush(guardar(laboratorio, seccion, responsable, FECHA, "08:00",
				"09:30"));

		reserva.reprogramar(otroLaboratorio, seccion, responsable, "Otra actividad", FECHA, hora("10:00"),
				hora("11:00"));
		repositorio.saveAndFlush(reserva);
		assertThat(reserva.getVersion()).isEqualTo(1);

		reserva.cancelar();
		repositorio.saveAndFlush(reserva);
		assertThat(reserva.getVersion()).isEqualTo(2);
		assertThat(jdbc.sql("SELECT estado FROM horarios.reserva_ambiente WHERE id = :id").param("id", reserva.getId())
				.query(String.class).single()).isEqualTo("CANCELADA");
	}

	@DisplayName("Superpuestas por ambiente solo trae las activas de la fecha que se cruzan")
	@Test
	void superpuestasPorAmbienteSoloTraeLasActivasDeLaFechaQueSeCruzan() {
		preparar();
		ReservaAmbiente cruza = guardar(laboratorio, seccion, responsable, FECHA, "09:00", "10:00");
		guardar(laboratorio, seccion, responsable, FECHA, "09:30", "10:30").cancelar(); // cancelada: no cuenta
		guardar(laboratorio, seccion, responsable, FECHA.plusDays(1), "08:00", "09:30"); // otra fecha
		guardar(otroLaboratorio, seccion, responsable, FECHA, "08:00", "09:30"); // otro ambiente

		List<ReservaAmbiente> halladas = repositorio.superpuestasPorAmbiente(laboratorio, FECHA, hora("08:00"),
				hora("09:30"), null);

		assertThat(halladas).extracting(ReservaAmbiente::getId).containsExactly(cruza.getId());
	}

	@DisplayName("Dos franjas contiguas no se superponen")
	@Test
	void dosFranjasContiguasNoSeSuperponen() {
		preparar();
		guardar(laboratorio, seccion, responsable, FECHA, "09:30", "10:15");
		guardar(laboratorio, seccion, responsable, FECHA, "07:15", "08:00");

		assertThat(repositorio.superpuestasPorAmbiente(laboratorio, FECHA, hora("08:00"), hora("09:30"), null))
				.isEmpty();
	}

	@DisplayName("Una franja contenida o que contiene a la otra si se superpone")
	@Test
	void unaFranjaContenidaOQueContieneALaOtraSiSeSuperpone() {
		preparar();
		ReservaAmbiente interior = guardar(laboratorio, seccion, responsable, FECHA, "08:30", "09:00");
		ReservaAmbiente exterior = guardar(laboratorio, otraSeccion, otroResponsable, FECHA, "07:00", "11:00");

		assertThat(repositorio.superpuestasPorAmbiente(laboratorio, FECHA, hora("08:00"), hora("09:30"), null))
				.extracting(ReservaAmbiente::getId).containsExactlyInAnyOrder(interior.getId(), exterior.getId());
	}

	@DisplayName("Superpuestas por sección y por responsable filtran por su clave")
	@Test
	void superpuestasPorSeccionYPorResponsableFiltranPorSuClave() {
		preparar();
		ReservaAmbiente deLaSeccion = guardar(laboratorio, seccion, otroResponsable, FECHA, "08:00", "09:00");
		ReservaAmbiente delResponsable = guardar(otroLaboratorio, otraSeccion, responsable, FECHA, "08:30", "09:30");

		assertThat(repositorio.superpuestasPorSeccion(seccion, FECHA, hora("08:00"), hora("09:30"), null))
				.extracting(ReservaAmbiente::getId).containsExactly(deLaSeccion.getId());
		assertThat(repositorio.superpuestasPorResponsable(responsable, FECHA, hora("08:00"), hora("09:30"), null))
				.extracting(ReservaAmbiente::getId).containsExactly(delResponsable.getId());
	}

	@DisplayName("Las canceladas no cuentan para la sección ni para el responsable")
	@Test
	void lasCanceladasNoCuentanParaLaSeccionNiParaElResponsable() {
		preparar();
		ReservaAmbiente cancelada = guardar(laboratorio, seccion, responsable, FECHA, "08:00", "09:30");
		cancelada.cancelar();
		repositorio.flush();

		assertThat(repositorio.superpuestasPorSeccion(seccion, FECHA, hora("08:00"), hora("09:30"), null)).isEmpty();
		assertThat(repositorio.superpuestasPorResponsable(responsable, FECHA, hora("08:00"), hora("09:30"), null))
				.isEmpty();
	}

	@DisplayName("Al editar se excluye la propia reserva en las tres consultas")
	@Test
	void alEditarSeExcluyeLaPropiaReservaEnLasTresConsultas() {
		preparar();
		ReservaAmbiente propia = guardar(laboratorio, seccion, responsable, FECHA, "08:00", "09:30");
		ReservaAmbiente otra = guardar(laboratorio, seccion, responsable, FECHA, "09:00", "10:00");

		assertThat(repositorio.superpuestasPorAmbiente(laboratorio, FECHA, hora("08:00"), hora("09:30"),
				propia.getId())).extracting(ReservaAmbiente::getId).containsExactly(otra.getId());
		assertThat(repositorio.superpuestasPorSeccion(seccion, FECHA, hora("08:00"), hora("09:30"), propia.getId()))
				.extracting(ReservaAmbiente::getId).containsExactly(otra.getId());
		assertThat(repositorio.superpuestasPorResponsable(responsable, FECHA, hora("08:00"), hora("09:30"),
				propia.getId())).extracting(ReservaAmbiente::getId).containsExactly(otra.getId());
		// Sin excluir (una reserva nueva) las dos cruzan.
		assertThat(repositorio.superpuestasPorAmbiente(laboratorio, FECHA, hora("08:00"), hora("09:30"), null))
				.hasSize(2);
	}

	@DisplayName("La entidad rechaza datos incompletos o horas invertidas")
	@Test
	void laEntidadRechazaDatosIncompletosOHorasInvertidas() {
		preparar();
		UUID creador = coordinador();

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> ReservaAmbiente.crear(laboratorio, seccion,
				responsable, "Experimento", FECHA, hora("09:00"), hora("09:00"), creador))
				.isInstanceOf(IllegalArgumentException.class);
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> ReservaAmbiente.crear(laboratorio, seccion,
				responsable, " ", FECHA, hora("08:00"), hora("09:00"), creador))
				.isInstanceOf(IllegalArgumentException.class);
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> ReservaAmbiente.crear(laboratorio, seccion,
				responsable, "x".repeat(121), FECHA, hora("08:00"), hora("09:00"), creador))
				.isInstanceOf(IllegalArgumentException.class);
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> ReservaAmbiente.crear(laboratorio, seccion,
				responsable, "Experimento", FECHA, hora("08:00"), hora("09:00"), null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@DisplayName("Está finalizada cuando llega la hora de fin y una cancelada no lo está")
	@Test
	void estaFinalizadaCuandoLlegaLaHoraDeFinYUnaCanceladaNoLoEsta() {
		preparar();
		ReservaAmbiente reserva = guardar(laboratorio, seccion, responsable, FECHA, "08:00", "09:30");

		assertThat(reserva.estaFinalizada(FECHA.atTime(9, 29))).isFalse();
		assertThat(reserva.estaFinalizada(FECHA.atTime(9, 30))).isTrue();
		assertThat(reserva.estaFinalizada(FECHA.plusDays(1).atTime(7, 0))).isTrue();

		reserva.cancelar();
		assertThat(reserva.estaFinalizada(FECHA.plusDays(1).atTime(7, 0))).isFalse();
		assertThat(reserva.estaCancelada()).isTrue();
	}
}
