package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HU-48 (V17): lo que la BD garantiza de horarios.reserva_ambiente. Cada rechazo usa un dato válido en todo salvo en
 * una regla y exige que la BD lo rechace por ESA restricción.
 */
class ReservaAmbienteEsquemaIT extends PruebaReservas {
	/** Inserta una reserva con cualquier valor sustituible; el resto es válido. */
	private int insertar(UUID ambiente, UUID seccion, UUID responsable, UUID creadoPor, String actividad,
			String inicio, String fin, String estado) {
		return jdbc
				.sql("""
						INSERT INTO horarios.reserva_ambiente
						    (ambiente_id, seccion_id, responsable_id, actividad, fecha, hora_inicio, hora_fin, estado, creado_por)
						VALUES (:ambiente, :seccion, :responsable, :actividad, DATE '2027-03-08', CAST(:inicio AS time),
						        CAST(:fin AS time), :estado, :creadoPor)""")
				.param("ambiente", ambiente).param("seccion", seccion).param("responsable", responsable)
				.param("actividad", actividad).param("inicio", inicio).param("fin", fin).param("estado", estado)
				.param("creadoPor", creadoPor).update();
	}

	private UUID laboratorio;
	private UUID seccion;
	private UUID responsable;
	private UUID coordinador;

	private void preparar() {
		laboratorio = espacio("TMP-LAB", "ACTIVO", 30);
		seccion = seccion("SEC-1", "R1", null, null);
		responsable = docente();
		coordinador = coordinador();
	}

	@DisplayName("Una reserva válida se guarda activa y con versión cero")
	@Test
	void unaReservaValidaSeGuardaActivaYConVersionCero() {
		preparar();

		assertThat(insertar(laboratorio, seccion, responsable, coordinador, "Experimento", "08:00", "09:30", "ACTIVA"))
				.isEqualTo(1);

		assertThat(jdbc.sql("SELECT estado FROM horarios.reserva_ambiente WHERE seccion_id = :s").param("s", seccion)
				.query(String.class).single())
				.isEqualTo("ACTIVA");
		assertThat(jdbc.sql("SELECT version FROM horarios.reserva_ambiente WHERE seccion_id = :s").param("s", seccion)
				.query(Long.class).single()).isZero();
	}

	private void rechazaReserva(String restriccion, UUID ambiente, UUID seccionId, UUID responsableId, UUID creador,
			String actividad, String inicio, String fin, String estado) {
		rechazaPor(restriccion,
				() -> insertar(ambiente, seccionId, responsableId, creador, actividad, inicio, fin, estado));
	}

	@DisplayName("La hora de fin no puede ser igual a la de inicio")
	@Test
	void laHoraDeFinNoPuedeSerIgualALaDeInicio() {
		preparar();

		rechazaReserva("ck_reserva_ambiente_horas", laboratorio, seccion, responsable, coordinador, "Experimento",
				"09:00", "09:00", "ACTIVA");
	}

	@DisplayName("La hora de fin no puede ser anterior a la de inicio")
	@Test
	void laHoraDeFinNoPuedeSerAnteriorALaDeInicio() {
		preparar();

		rechazaReserva("ck_reserva_ambiente_horas", laboratorio, seccion, responsable, coordinador, "Experimento",
				"09:30", "09:00", "ACTIVA");
	}

	@DisplayName("El estado no puede ser finalizada")
	@Test
	void elEstadoNoPuedeSerFinalizada() {
		preparar();

		rechazaReserva("ck_reserva_ambiente_estado", laboratorio, seccion, responsable, coordinador, "Experimento",
				"08:00", "09:30", "FINALIZADA");
	}

	@DisplayName("Una reserva cancelada se guarda")
	@Test
	void unaReservaCanceladaSeGuarda() {
		preparar();

		assertThat(insertar(laboratorio, seccion, responsable, coordinador, "Experimento", "08:00", "09:30",
				"CANCELADA")).isEqualTo(1);
	}

	@DisplayName("La actividad no admite más de120 caracteres")
	@Test
	void laActividadNoAdmiteMasDe120Caracteres() {
		preparar();

		rechazaReserva("character varying(120)", laboratorio, seccion, responsable, coordinador, "x".repeat(121),
				"08:00", "09:30", "ACTIVA");
	}

	@DisplayName("La actividad admite exactamente120 caracteres")
	@Test
	void laActividadAdmiteExactamente120Caracteres() {
		preparar();

		assertThat(insertar(laboratorio, seccion, responsable, coordinador, "x".repeat(120), "08:00", "09:30",
				"ACTIVA")).isEqualTo(1);
	}

	@DisplayName("El ambiente debe existir")
	@Test
	void elAmbienteDebeExistir() {
		preparar();

		rechazaReserva("reserva_ambiente_ambiente_id_fkey", UUID.randomUUID(), seccion, responsable, coordinador,
				"Experimento", "08:00", "09:30", "ACTIVA");
	}

	@DisplayName("La sección debe existir")
	@Test
	void laSeccionDebeExistir() {
		preparar();

		rechazaReserva("reserva_ambiente_seccion_id_fkey", laboratorio, UUID.randomUUID(), responsable, coordinador,
				"Experimento", "08:00", "09:30", "ACTIVA");
	}

	@DisplayName("El responsable debe existir")
	@Test
	void elResponsableDebeExistir() {
		preparar();

		rechazaReserva("reserva_ambiente_responsable_id_fkey", laboratorio, seccion, UUID.randomUUID(), coordinador,
				"Experimento", "08:00", "09:30", "ACTIVA");
	}

	@DisplayName("Quien registra la reserva debe existir")
	@Test
	void quienRegistraLaReservaDebeExistir() {
		preparar();

		rechazaReserva("reserva_ambiente_creado_por_fkey", laboratorio, seccion, responsable, UUID.randomUUID(),
				"Experimento", "08:00", "09:30", "ACTIVA");
	}

	@DisplayName("Un ambiente con reservas no se elimina")
	@Test
	void unAmbienteConReservasNoSeElimina() {
		preparar();
		reserva(laboratorio, seccion, responsable, "2027-03-08", "08:00", "09:30", "ACTIVA");

		rechazaPor("reserva_ambiente_ambiente_id_fkey",
				() -> jdbc.sql("DELETE FROM configuracion.ambiente WHERE id = :id").param("id", laboratorio).update());
	}

	@DisplayName("Un ambiente sin reservas si se elimina")
	@Test
	void unAmbienteSinReservasSiSeElimina() {
		preparar();

		assertThat(jdbc.sql("DELETE FROM configuracion.ambiente WHERE id = :id").param("id", laboratorio).update())
				.isEqualTo(1);
	}

	@DisplayName("Al eliminar la sección se borran sus reservas")
	@Test
	void alEliminarLaSeccionSeBorranSusReservas() {
		preparar();
		reserva(laboratorio, seccion, responsable, "2027-03-08", "08:00", "09:30", "ACTIVA");

		jdbc.sql("DELETE FROM configuracion.seccion WHERE id = :id").param("id", seccion).update();

		assertThat(jdbc.sql("SELECT count(*) FROM horarios.reserva_ambiente WHERE seccion_id = :s").param("s", seccion)
				.query(Long.class).single()).isZero();
	}
}
