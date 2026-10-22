package com.galiledu.configuracion;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;

import com.galiledu.PruebaIntegracion;

/**
 * Base de las pruebas de integración de Configuración que dependen de la fecha de hoy.
 *
 * <p>
 * <b>El reloj.</b> Reemplaza el {@code Clock} de la aplicación (America/Lima) por un {@link RelojPrueba}, que empieza
 * fijo en <b>15/09/2026</b>: en los datos de prueba el III Bimestre de 2026 está en curso, el I y el II ya terminaron y
 * el IV empieza el 19/10. Una prueba que necesite otro día inyecta el reloj con {@code @Autowired RelojPrueba reloj} y
 * llama a {@code reloj.fijarEn(LocalDate.of(2026, 10, 12))}. No hace falta reiniciarlo: {@link RestauradorRelojPrueba}
 * lo devuelve al 15/09/2026 después de cada prueba, porque el contexto de Spring se comparte entre clases.
 *
 * <p>
 * <b>SQL junto con JPA.</b> La prueba y la petición de MockMvc comparten una sola transacción y un solo contexto de
 * JPA. Si una prueba cambia datos con {@code JdbcClient} (por ejemplo, cerrar un año con un {@code UPDATE}) después de
 * que JPA ya cargó esa fila, el servicio seguiría viendo la entidad vieja. Llama a {@code entityManager.clear()} justo
 * después del SQL para que la relea. En sentido contrario, si cambias algo con JPA y lo verificas con
 * {@code JdbcClient},
 * llama antes a {@code entityManager.flush()} (o usa un servicio que ya haga flush).
 *
 * <p>
 * Las pruebas que no usan el reloj ni mezclan SQL con JPA pueden seguir extendiendo {@link PruebaIntegracion}.
 */
@Import(RelojFijoConfig.class)
@TestExecutionListeners(listeners = RestauradorRelojPrueba.class, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
public abstract class PruebaIntegracionConfiguracion extends PruebaIntegracion {
}
