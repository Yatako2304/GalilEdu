package com.galiledu.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/** HU-75: reglas de una sección, sin base de datos. */
class SeccionTests {
	private static final UUID ANIO = UUID.randomUUID();
	private static final UUID TUTOR = UUID.randomUUID();
	private static final UUID COTUTOR = UUID.randomUUID();

	private static Seccion seccion() {
		return Seccion.crear(ANIO, "PRI-2", "A", 30);
	}

	@Test
	void laSeccionNaceActivaYSinTutores() {
		Seccion s = seccion();

		assertThat(s.getEstado()).isEqualTo(EstadoSeccion.ACTIVA);
		assertThat(s.estaActiva()).isTrue();
		assertThat(s.getTutorId()).isNull();
		assertThat(s.getCotutorId()).isNull();
		assertThat(s.getGradoCodigo()).isEqualTo("PRI-2");
		assertThat(s.getCapacidadMaxima()).isEqualTo(30);
		assertThat(s.getAlumnosOcupanVacante()).isZero();
	}

	@Test
	void elNombreSeGuardaSinEspaciosYEnMayusculas() {
		assertThat(Seccion.crear(ANIO, "PRI-2", "  c ", 30).getNombre()).isEqualTo("C");
	}

	@Test
	void rechazaUnNombreVacio() {
		assertThatThrownBy(() -> Seccion.crear(ANIO, "PRI-2", "   ", 30))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Ingresa el nombre de la sección.");
		assertThatThrownBy(() -> Seccion.crear(ANIO, "PRI-2", null, 30))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Ingresa el nombre de la sección.");
	}

	@Test
	void rechazaUnNombreDeMasDeCincoCaracteres() {
		assertThatThrownBy(() -> Seccion.crear(ANIO, "PRI-2", "ABCDEF", 30))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("El nombre de la sección admite hasta 5 caracteres.");
		assertThatCode(() -> Seccion.crear(ANIO, "PRI-2", "ABCDE", 30)).doesNotThrowAnyException();
	}

	@Test
	void rechazaUnNombreConSimbolos() {
		assertThatThrownBy(() -> Seccion.crear(ANIO, "PRI-2", "A-1", 30))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("El nombre de la sección solo admite letras y números.");
	}

	@Test
	void exigeUnGradoYUnAnio() {
		assertThatThrownBy(() -> Seccion.crear(ANIO, " ", "A", 30)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Elige un grado.");
		assertThatThrownBy(() -> Seccion.crear(null, "PRI-2", "A", 30)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Elige el año escolar.");
	}

	@Test
	void laCapacidadVaDeUnoACuarentaYCinco() {
		assertThat(Seccion.crear(ANIO, "PRI-2", "A", 1).getCapacidadMaxima()).isEqualTo(1);
		assertThat(Seccion.crear(ANIO, "PRI-2", "A", 45).getCapacidadMaxima()).isEqualTo(45);
		assertThatThrownBy(() -> Seccion.crear(ANIO, "PRI-2", "A", 0)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Entre 1 y 45 alumnos.");
		assertThatThrownBy(() -> Seccion.crear(ANIO, "PRI-2", "A", 46)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Entre 1 y 45 alumnos.");
	}

	@Test
	void cambiaLaCapacidadSiNoBajaDeLosAlumnosQueOcupanVacante() {
		Seccion s = seccion();

		s.cambiarCapacidad(25, 25);

		assertThat(s.getCapacidadMaxima()).isEqualTo(25);
	}

	@Test
	void noBajaLaCapacidadPorDebajoDeLosAlumnosQueOcupanVacante() {
		Seccion s = seccion();

		assertThatThrownBy(() -> s.cambiarCapacidad(24, 25)).isInstanceOf(IllegalStateException.class)
				.hasMessage("La capacidad no puede ser menor que los alumnos que ocupan vacante (25).");
		assertThat(s.getCapacidadMaxima()).isEqualTo(30);
	}

	@Test
	void alCambiarLaCapacidadTambienSeValidaElRango() {
		assertThatThrownBy(() -> seccion().cambiarCapacidad(46, 0)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Entre 1 y 45 alumnos.");
	}

	@Test
	void asignaTutorYCotutor() {
		Seccion s = seccion();

		s.asignarTutores(TUTOR, COTUTOR);

		assertThat(s.getTutorId()).isEqualTo(TUTOR);
		assertThat(s.getCotutorId()).isEqualTo(COTUTOR);
	}

	@Test
	void elCotutorEsOpcionalYSePuedeQuitar() {
		Seccion s = seccion();
		s.asignarTutores(TUTOR, COTUTOR);

		s.asignarTutores(TUTOR, null);

		assertThat(s.getTutorId()).isEqualTo(TUTOR);
		assertThat(s.getCotutorId()).isNull();
	}

	@Test
	void elTutorEsObligatorio() {
		assertThatThrownBy(() -> seccion().asignarTutores(null, COTUTOR))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Elige un tutor.");
	}

	@Test
	void elCotutorDebeSerDistintoDelTutor() {
		assertThatThrownBy(() -> seccion().asignarTutores(TUTOR, TUTOR))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("El cotutor debe ser distinto del tutor.");
	}

	@Test
	void inactivaYReactivaLaSeccion() {
		Seccion s = seccion();

		s.inactivar();
		assertThat(s.getEstado()).isEqualTo(EstadoSeccion.INACTIVA);
		assertThat(s.estaActiva()).isFalse();

		s.reactivar();
		assertThat(s.getEstado()).isEqualTo(EstadoSeccion.ACTIVA);
	}

	@Test
	void noSeInactivaUnaSeccionInactivaNiSeReactivaUnaActiva() {
		Seccion s = seccion();

		assertThatThrownBy(s::reactivar).isInstanceOf(IllegalStateException.class)
				.hasMessage("La sección ya está activa.");
		s.inactivar();
		assertThatThrownBy(s::inactivar).isInstanceOf(IllegalStateException.class)
				.hasMessage("La sección ya está inactiva.");
	}

	@Test
	void unAnioCerradoNoSeModifica() {
		assertThatThrownBy(() -> Seccion.validarAnioModificable(EstadoAnioEscolar.CERRADO))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("El año escolar está cerrado: no se pueden modificar sus secciones.");
		assertThatCode(() -> Seccion.validarAnioModificable(EstadoAnioEscolar.PLANIFICADO)).doesNotThrowAnyException();
		assertThatCode(() -> Seccion.validarAnioModificable(EstadoAnioEscolar.ACTIVO)).doesNotThrowAnyException();
	}
}
