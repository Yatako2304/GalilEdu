package com.galiledu.horarios.infraestructura;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.DetalleEstructuraHoraria;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.EstructuraHorariaResumen;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.FilaJornadaResumen;
import com.galiledu.configuracion.aplicacion.ServicioEstructurasHorarias;
import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion;

/**
 * Obtiene la jornada de {@link ServicioEstructurasHorarias}, que es quien calcula los bloques y los recesos (con
 * {@code CalculadoraJornada}). Aquí solo se traducen sus filas; las horas no se vuelven a calcular.
 */
@Component
public class JornadaDeSeccionConfiguracion implements JornadaDeSeccion {
	private final ServicioEstructurasHorarias estructuras;

	public JornadaDeSeccionConfiguracion(ServicioEstructurasHorarias estructuras) {
		this.estructuras = estructuras;
	}

	@Override
	public JornadaEstructura deEstructura(UUID estructuraId) {
		return aJornada(estructuras.detalleEstructuraHoraria(estructuraId));
	}

	@Override
	public Optional<JornadaEstructura> principalDelNivel(UUID anioEscolarId, String nivel) {
		return estructuras.listarPorAnioEscolar(anioEscolarId).estructuras().stream()
				.filter(e -> e.esPrincipal() && e.nivel().equals(nivel)).map(EstructuraHorariaResumen::id).findFirst()
				.map(this::deEstructura);
	}

	private static JornadaEstructura aJornada(DetalleEstructuraHoraria d) {
		List<DiaSemana> dias = d.diasLectivos().stream().map(DiaSemana::valueOf)
				.sorted(Comparator.comparingInt(DiaSemana::ordinal)).toList();
		List<BloqueDeJornada> bloques = new ArrayList<>();
		List<RecesoDeJornada> recesos = new ArrayList<>();
		int ultimoBloque = 0;
		for (FilaJornadaResumen fila : d.jornada()) {
			LocalTime inicio = LocalTime.parse(fila.inicio());
			LocalTime fin = LocalTime.parse(fila.fin());
			if ("BLOQUE".equals(fila.tipo())) {
				ultimoBloque = fila.numero();
				bloques.add(new BloqueDeJornada(fila.numero(), inicio, fin));
			} else {
				recesos.add(new RecesoDeJornada(ultimoBloque, inicio, fin));
			}
		}
		return new JornadaEstructura(d.id(), d.codigo(), d.esPrincipal(), dias, bloques, recesos);
	}
}
