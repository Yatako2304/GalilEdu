package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.galiledu.PruebaIntegracion;
import com.galiledu.configuracion.aplicacion.DirectorioDocentes;
import com.galiledu.configuracion.aplicacion.DirectorioDocentes.Docente;

/** La lectura de usuarios que necesitan las secciones: docentes elegibles y nombres de tutores y cotutores. */
class DirectorioDocentesSqlIT extends PruebaIntegracion {
	private static final UUID MARIA_FERNANDEZ = UUID.fromString("10000000-0000-0000-0000-000000000004");
	private static final UUID LUIS_PAREDES_APODERADO = UUID.fromString("10000000-0000-0000-0000-000000000003");
	private static final UUID LEONARDO_MONTOYA_INACTIVO = UUID.fromString("10000000-0000-0000-0000-000000000007");

	@Autowired
	private DirectorioDocentes directorioDocentes;

	@Test
	void losElegiblesSonLosUsuariosActivosConRolDocenteOTutorSinRepetirALosQueTienenAmbos() {
		List<Docente> elegibles = directorioDocentes.elegiblesParaTutoria();

		// María tiene los roles DOCENTE y TUTOR: aparece una sola vez. Los demás usuarios de prueba no son docentes.
		assertThat(elegibles).containsExactly(new Docente(MARIA_FERNANDEZ, "María Fernández Ríos"));
	}

	@Test
	void devuelveElNombreCompletoDeCadaUsuarioPedidoSeaCualSeaSuEstado() {
		Map<UUID, String> nombres = directorioDocentes
				.nombresDe(List.of(MARIA_FERNANDEZ, LUIS_PAREDES_APODERADO, LEONARDO_MONTOYA_INACTIVO));

		assertThat(nombres).containsOnly(
				Map.entry(MARIA_FERNANDEZ, "María Fernández Ríos"),
				Map.entry(LUIS_PAREDES_APODERADO, "Luis Paredes Silva"),
				// Una sección conserva a su tutor aunque después lo hayan desactivado.
				Map.entry(LEONARDO_MONTOYA_INACTIVO, "Leonardo Montoya Spray"));
	}

	@Test
	void ignoraLosIdsQueNoExistenYUnaListaVaciaDaUnMapaVacio() {
		assertThat(directorioDocentes.nombresDe(List.of(UUID.randomUUID()))).isEmpty();
		assertThat(directorioDocentes.nombresDe(List.of())).isEmpty();
	}
}
