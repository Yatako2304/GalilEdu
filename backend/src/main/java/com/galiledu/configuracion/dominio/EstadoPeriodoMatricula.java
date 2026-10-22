package com.galiledu.configuracion.dominio;

/**
 * Estados guardados de matricula.periodo_matricula (CHECK ck_periodo_matricula_estado). Guardar la configuración lo
 * deja HABILITADO; EN_CONFIGURACION no se usa en el flujo de la pantalla 13. Lo que se muestra (PROGRAMADO, ABIERTO,
 * CERRADO) es {@link EstadoVisiblePeriodoMatricula} y se calcula con las fechas.
 */
public enum EstadoPeriodoMatricula {
	EN_CONFIGURACION, HABILITADO, CERRADO
}
