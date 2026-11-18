package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.galiledu.horarios.aplicacion.DtosHorarios.Referencia;
import com.galiledu.horarios.aplicacion.ResponsablesElegibles;

/**
 * HU-48: quién puede ser responsable de una reserva, leído de la BD real. María Fernández (U20260004) es Docente y
 * Tutor a la vez; el Coordinador no es ninguno de los dos.
 */
class ResponsablesElegiblesSqlIT extends PruebaReservas {
	@Autowired
	private ResponsablesElegibles responsables;

	private List<UUID> ids() {
		return responsables.elegibles().stream().map(Referencia::id).toList();
	}

	@Test
	void unDocenteConDosRolesApareceUnaSolaVezConSuNombreCompleto() {
		UUID maria = docente();

		assertThat(responsables.elegibles()).filteredOn(r -> r.id().equals(maria)).singleElement()
				.extracting(Referencia::nombre).isEqualTo("María Fernández Ríos");
	}

	@Test
	void quienSoloEsTutorTambienEsElegible() {
		UUID tutor = usuario("79999931", "U20269931", "ACTIVO", false, "TUTOR");

		assertThat(ids()).contains(tutor);
		assertThat(responsables.existe(tutor)).isTrue();
	}

	@Test
	void unDocenteInactivoOEliminadoNoEsElegible() {
		UUID inactivo = usuario("79999932", "U20269932", "INACTIVO", false, "DOCENTE");
		UUID eliminado = usuario("79999933", "U20269933", "ACTIVO", true, "DOCENTE");

		assertThat(ids()).doesNotContain(inactivo, eliminado);
		assertThat(responsables.existe(inactivo)).isFalse();
		assertThat(responsables.existe(eliminado)).isFalse();
	}

	@Test
	void quienNoEsDocenteNiTutorNoEsElegible() {
		UUID coordinador = coordinador();
		UUID apoderado = usuario("79999934", "U20269934", "ACTIVO", false, "APODERADO");

		assertThat(ids()).doesNotContain(coordinador, apoderado);
		assertThat(responsables.existe(coordinador)).isFalse();
		assertThat(responsables.existe(apoderado)).isFalse();
	}

	@Test
	void unUsuarioInexistenteNoEsElegible() {
		assertThat(responsables.existe(UUID.randomUUID())).isFalse();
	}

	@Test
	void losElegiblesSalenOrdenadosPorNombre() {
		usuario("79999935", "U20269935", "ACTIVO", false, "DOCENTE");

		List<String> nombres = responsables.elegibles().stream().map(Referencia::nombre).toList();

		assertThat(nombres).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER.thenComparing(String::compareTo))
				.hasSizeGreaterThanOrEqualTo(2);
	}
}
