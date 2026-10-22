package com.galiledu.configuracion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.SolicitudEstructuraHoraria;
import com.galiledu.configuracion.dominio.AnioEscolar;
import com.galiledu.configuracion.dominio.EstructuraHoraria;
import com.galiledu.configuracion.dominio.Regimen;
import com.galiledu.configuracion.infraestructura.AnioEscolarRepositorio;
import com.galiledu.configuracion.infraestructura.EstructuraHorariaRepositorio;

/**
 * El mensaje que recibe el usuario cuando la BD rechaza el guardado. Con el año bloqueado esas restricciones casi nunca
 * saltan, así que no se pueden provocar desde la API: se simulan con un repositorio de mentira que lanza
 * DataIntegrityViolationException con el texto de cada restricción.
 */
class ServicioEstructurasHorariasTests {
	private static final UUID ANIO_ID = UUID.randomUUID();

	private final EstructuraHorariaRepositorio estructuras = mock(EstructuraHorariaRepositorio.class);
	private final AnioEscolarRepositorio anios = mock(AnioEscolarRepositorio.class);
	private final PlanesDeEvaluacion planes = mock(PlanesDeEvaluacion.class);
	private final ServicioEstructurasHorarias servicio = new ServicioEstructurasHorarias(estructuras, anios, planes);

	@BeforeEach
	void prepararUnAnioPlanificado() {
		AnioEscolar anio = AnioEscolar.planificar(2027, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 12, 15),
				Regimen.BIMESTRAL);
		when(anios.findById(any())).thenReturn(Optional.of(anio));
	}

	private static SolicitudEstructuraHoraria solicitud() {
		return new SolicitudEstructuraHoraria("EH-1", "Prueba", "PRIMARIA", false, List.of("LUN", "MAR"),
				LocalTime.of(8, 0), 6, 45, List.of());
	}

	private void laBaseRechazaElGuardadoCon(String detalleDeLaBase) {
		when(estructuras.saveAndFlush(any(EstructuraHoraria.class)))
				.thenThrow(new DataIntegrityViolationException(detalleDeLaBase));
	}

	@Test
	void elIndiceDeLaPrincipalDaElMensajeDeOtraPersona() {
		laBaseRechazaElGuardadoCon(
				"ERROR: duplicate key value violates unique constraint \"ux_estructura_horaria_principal\"");

		assertThatThrownBy(() -> servicio.crearEstructuraHoraria(ANIO_ID, solicitud()))
				.isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
					assertThat(e.estado()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(e).hasMessage(ServicioEstructurasHorarias.MENSAJE_OTRA_PERSONA);
				});
	}

	@Test
	void laRestriccionDelCodigoDaElMensajeDeCodigoRepetido() {
		laBaseRechazaElGuardadoCon(
				"ERROR: duplicate key value violates unique constraint \"uq_estructura_horaria_codigo\"");

		assertThatThrownBy(() -> servicio.crearEstructuraHoraria(ANIO_ID, solicitud()))
				.isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
					assertThat(e.estado()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(e).hasMessage("Ya existe una estructura con el código EH-1 en este año.");
				});
	}

	@Test
	void cualquierOtraRestriccionDaUnMensajeGenericoSinNombresInternos() {
		for (String detalle : List.of(
				"ERROR: new row for relation \"estructura_horaria\" violates check constraint \"ck_estructura_horaria_bloques\"",
				"ERROR: invalid byte sequence for encoding \"UTF8\": 0x00",
				"ERROR: insert or update on table \"receso\" violates foreign key constraint \"fk_receso\"")) {
			laBaseRechazaElGuardadoCon(detalle);

			assertThatThrownBy(() -> servicio.crearEstructuraHoraria(ANIO_ID, solicitud()))
					.isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
						assertThat(e.estado()).isEqualTo(HttpStatus.CONFLICT);
						assertThat(e).hasMessage(ServicioEstructurasHorarias.MENSAJE_NO_SE_PUDO_GUARDAR);
						assertThat(e.getMessage()).doesNotContain("Otra persona", "ck_", "fk_", "ux_", "uq_",
								"estructura_horaria", "receso", "UTF8");
					});
		}
	}
}
