// Enlaces a pantallas de otros módulos que todavía no existen. Mientras la constante sea `false`, el botón o la opción
// se muestra deshabilitado con el aviso "Disponible pronto"; al hacer la pantalla basta con cambiarla a `true`.

/**
 * A3 "Matrículas del periodo" de Matrícula (HU-24, HU-71): `paths.app.administrativo.matriculasDelPeriodo`.
 * Al activarla, registrar la ruta en el router.
 */
export const MATRICULAS_DEL_PERIODO_DISPONIBLE = false;

/**
 * P19 "Tarifario y calendario" (HU-34): `paths.app.administrativo.tarifario`. Al activarla, registrar la ruta en el
 * router y habilitar el botón "Configuración" de la pantalla de Pensiones.
 */
export const TARIFARIO_DISPONIBLE = false;

export const TEXTO_DISPONIBLE_PRONTO = 'Disponible pronto';
