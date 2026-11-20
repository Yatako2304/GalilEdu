package com.galiledu.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-75: reglas de una sección, sin base de datos. */
class SeccionTests {
	private static final UUID ANIO = UUID.randomUUID();
	private static final UUID TUTOR = UUID.randomUUID();
	private static final UUID COTUTOR = UUID.randomUUID();

	private static Seccion seccion() {
		return Seccion.crear(ANIO, "PRI-2", "A", 30);
	}

	@DisplayName("La sección nace activa y sin tutores")
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

	@DisplayName("El nombre se guarda sin espacios y en mayúsculas")
	@Test
	void elNombreSeGuardaSinEspaciosYEnMayusculas() {
		assertThat(Seccion.crear(ANIO, "PRI-2", "  c ", 30).getNombre()).isEqualTo("C");
	}

	@DisplayName("Rechaza un nombre vacio")
	@Test
	void rechazaUnNombreVacio() {
		assertThatThrownBy(() -> Seccion.crear(ANIO, "PRI-2", "   ", 30))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Ingresa el nombre de la sección.");
		assertThatThrownBy(() -> Seccion.crear(ANIO, "PRI-2", null, 30))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Ingresa el nombre de la sección.");
	}

	@DisplayName("Rechaza un nombre de más de cinco caracteres")
	@Test
	void rechazaUnNombreDeMasDeCincoCaracteres() {
		assertThatThrownBy(() -> Seccion.crear(ANIO, "PRI-2", "ABCDEF", 30))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("El nombre de la sección admite hasta 5 caracteres.");
		assertThatCode(() -> Seccion.crear(ANIO, "PRI-2", "ABCDE", 30)).doesNotThrowAnyException();
	}

	@DisplayName("Rechaza un nombre con simbolos")
	@Test
	void rechazaUnNombreConSimbolos() {
		assertThatThrownBy(() -> Seccion.crear(ANIO, "PRI-2", "A-1", 30))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("El nombre de la sección solo admite letras y números.");
	}

	@DisplayName("Exige un grado y un año")
	@Test
	void exigeUnGradoYUnAnio() {
		assertThatThrownBy(() -> Seccion.crear(ANIO, " ", "A", 30)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Elige un grado.");
		assertThatThrownBy(() -> Seccion.crear(null, "PRI-2", "A", 30)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Elige el año escolar.");
	}

	@DisplayName("La capacidad va de uno a cuarenta y cinco")
	@Test
	void laCapacidadVaDeUnoACuarentaYCinco() {
		assertThat(Seccion.crear(ANIO, "PRI-2", "A", 1).getCapacidadMaxima()).isEqualTo(1);
		assertThat(Seccion.crear(ANIO, "PRI-2", "A", 45).getCapacidadMaxima()).isEqualTo(45);
		assertThatThrownBy(() -> Seccion.crear(ANIO, "PRI-2", "A", 0)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Entre 1 y 45 alumnos.");
		assertThatThrownBy(() -> Seccion.crear(ANIO, "PRI-2", "A", 46)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Entre 1 y 45 alumnos.");
	}

	@DisplayName("Cambia la capacidad si no baja de los alumnos que ocupan vacante")
	@Test
	void cambiaLaCapacidadSiNoBajaDeLosAlumnosQueOcupanVacante() {
		Seccion s = seccion();

		s.cambiarCapacidad(25, 25);

		assertThat(s.getCapacidadMaxima()).isEqualTo(25);
	}

	@DisplayName("No baja la capacidad por debajo de los alumnos que ocupan vacante")
	@Test
	void noBajaLaCapacidadPorDebajoDeLosAlumnosQueOcupanVacante() {
		Seccion s = seccion();

		assertThatThrownBy(() -> s.cambiarCapacidad(24, 25)).isInstanceOf(IllegalStateException.class)
				.hasMessage("La capacidad no puede ser menor que los alumnos que ocupan vacante (25).");
		assertThat(s.getCapacidadMaxima()).isEqualTo(30);
	}

	@DisplayName("Al cambiar la capacidad también se válida el rango")
	@Test
	void alCambiarLaCapacidadTambienSeValidaElRango() {
		assertThatThrownBy(() -> seccion().cambiarCapacidad(46, 0)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Entre 1 y 45 alumnos.");
	}

	@DisplayName("Asigna tutor y cotutor")
	@Test
	void asignaTutorYCotutor() {
		Seccion s = seccion();

		s.asignarTutores(TUTOR, COTUTOR);

		assertThat(s.getTutorId()).isEqualTo(TUTOR);
		assertThat(s.getCotutorId()).isEqualTo(COTUTOR);
	}

	@DisplayName("El cotutor es opcional y se puede quitar")
	@Test
	void elCotutorEsOpcionalYSePuedeQuitar() {
		Seccion s = seccion();
		s.asignarTutores(TUTOR, COTUTOR);

		s.asignarTutores(TUTOR, null);

		assertThat(s.getTutorId()).isEqualTo(TUTOR);
		assertThat(s.getCotutorId()).isNull();
	}

	@DisplayName("El tutor es obligatorio")
	@Test
	void elTutorEsObligatorio() {
		assertThatThrownBy(() -> seccion().asignarTutores(null, COTUTOR))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Elige un tutor.");
	}

	@DisplayName("El cotutor debe ser distinto del tutor")
	@Test
	void elCotutorDebeSerDistintoDelTutor() {
		assertThatThrownBy(() -> seccion().asignarTutores(TUTOR, TUTOR))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("El cotutor debe ser distinto del tutor.");
	}

	@DisplayName("Inactiva y reactiva la sección")
	@Test
	void inactivaYReactivaLaSeccion() {
		Seccion s = seccion();

		s.inactivar();
		assertThat(s.getEstado()).isEqualTo(EstadoSeccion.INACTIVA);
		assertThat(s.estaActiva()).isFalse();

		s.reactivar();
		assertThat(s.getEstado()).isEqualTo(EstadoSeccion.ACTIVA);
	}

	@DisplayName("No se inactiva una sección inactiva ni se reactiva una activa")
	@Test
	void noSeInactivaUnaSeccionInactivaNiSeReactivaUnaActiva() {
		Seccion s = seccion();

		assertThatThrownBy(s::reactivar).isInstanceOf(IllegalStateException.class)
				.hasMessage("La sección ya está activa.");
		s.inactivar();
		assertThatThrownBy(s::inactivar).isInstanceOf(IllegalStateException.class)
				.hasMessage("La sección ya está inactiva.");
	}

	@DisplayName("Un año cerrado no se modifica")
	@Test
	void unAnioCerradoNoSeModifica() {
		assertThatThrownBy(() -> Seccion.validarAnioModificable(EstadoAnioEscolar.CERRADO))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("El año escolar está cerrado: no se pueden modificar sus secciones.");
		assertThatCode(() -> Seccion.validarAnioModificable(EstadoAnioEscolar.PLANIFICADO)).doesNotThrowAnyException();
		assertThatCode(() -> Seccion.validarAnioModificable(EstadoAnioEscolar.ACTIVO)).doesNotThrowAnyException();
	}
}
