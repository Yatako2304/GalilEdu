package com.galiledu.horarios.aplicacion;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.configuracion.dominio.DiaSemana;

/**
 * La jornada de una estructura horaria de Configuración, con las horas ya calculadas. Es de solo lectura: las horas
 * las calcula Configuración ({@code CalculadoraJornada}) y aquí no se vuelven a calcular.
 */
public interface JornadaDeSeccion {
	/** Un bloque de la jornada de un día, con su hora de inicio y de fin. */
	record BloqueDeJornada(int numero, LocalTime inicio, LocalTime fin) {
	}

	/** Un receso: va después del bloque {@code despuesDeBloque}. */
	record RecesoDeJornada(int despuesDeBloque, LocalTime inicio, LocalTime fin) {
	}

	/** La jornada es igual cada día lectivo de {@code dias}. */
	record JornadaEstructura(UUID estructuraId, String codigo, boolean principal, List<DiaSemana> dias,
			List<BloqueDeJornada> bloques, List<RecesoDeJornada> recesos) {
	}

	/** La jornada de una estructura (la guardada en el horario, aunque ya no sea la principal). */
	JornadaEstructura deEstructura(UUID estructuraId);

	/** La jornada de la estructura principal del nivel en el año; vacío si el nivel no tiene principal. */
	Optional<JornadaEstructura> principalDelNivel(UUID anioEscolarId, String nivel);
}
