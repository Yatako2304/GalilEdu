import { api } from '@/lib/api-client';

const MENSAJE = 'Ya existe un usuario registrado con este documento';

const FORMATO: Record<string, RegExp> = {
  DNI: /^[0-9]{8}$/,
  CARNET_EXTRANJERIA: /^[A-Za-z0-9]{1,12}$/,
};

/**
 * Revisa si el documento ya está registrado, para mostrar el error debajo del campo junto con las
 * demás validaciones (y no como notificación al guardar). Devuelve el mensaje de error, o null si se
 * puede usar. Usa GET /estudiantes/personas?documento= (200 = existe, 404 = libre, 409 = es un estudiante).
 * Si la consulta falla, no se bloquea: el backend vuelve a validar al guardar.
 *
 * Si es de un usuario eliminado (`cuentaEliminada`), al crear no es un error: el backend reactiva esa
 * cuenta. Al editar sí, porque no se puede usar el documento de otra persona.
 */
export const verificarDocumento = async (
  tipoDocumento: string,
  numeroDocumento: string,
  alCrear: boolean,
): Promise<string | null> => {
  const numero = numeroDocumento.trim();
  if (!FORMATO[tipoDocumento]?.test(numero)) return null;
  try {
    const respuesta: {
      personaId?: string;
      cuentaEliminada?: boolean;
      message?: string;
    } = await api.get('/estudiantes/personas', {
      params: { documento: numero },
      validateStatus: (s) => s === 200 || s === 404 || s === 409,
    });
    if (respuesta.personaId) {
      return alCrear && respuesta.cuentaEliminada ? null : MENSAJE;
    }
    return respuesta.message?.includes('estudiante') ? MENSAJE : null;
  } catch {
    return null;
  }
};
