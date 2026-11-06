package com.galiledu.horarios.aplicacion;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.horarios.aplicacion.DatosDeSeccion.DatosSeccion;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.JornadaEstructura;
import com.galiledu.horarios.dominio.HorarioSeccion;

/**
 * Qué jornada rige a una sección: la de la estructura guardada en su horario (aunque ya no sea la principal) y, si
 * aún no tiene horario, la de la estructura principal de su nivel. No escribe nada.
 */
@Component
public class ResolutorDeJornada {
	private final JornadaDeSeccion jornadas;

	public ResolutorDeJornada(JornadaDeSeccion jornadas) {
		this.jornadas = jornadas;
	}

	/** 409 si no hay horario guardado y el nivel no tiene estructura principal. */
	public JornadaEstructura resolver(DatosSeccion seccion, Optional<HorarioSeccion> horario) {
		if (horario.isPresent()) {
			return jornadas.deEstructura(horario.get().getEstructuraHorariaId());
		}
		return jornadas.principalDelNivel(seccion.anioEscolarId(), seccion.nivel())
				.orElseThrow(() -> ExcepcionNegocio.conflicto("Configura la estructura horaria principal de "
						+ seccion.nivel().toLowerCase(Locale.ROOT) + " antes de asignar horarios."));
	}
}
