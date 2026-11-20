import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';

// HU-2: el administrador activa o desactiva el inicio de sesión con Google para el colegio.
export type ConfiguracionAcceso = {
  googleHabilitado: boolean;
  /** El servidor tiene GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET; sin ellos no se puede activar. */
  googleConfigurado: boolean;
};

const CLAVE = ['seguridad', 'configuracion-acceso'];

// GET /seguridad/configuracion-acceso
export const useConfiguracionAcceso = () =>
  useQuery({
    queryKey: CLAVE,
    queryFn: (): Promise<ConfiguracionAcceso> =>
      api.get('/seguridad/configuracion-acceso'),
  });

// PUT /seguridad/configuracion-acceso { googleHabilitado }
export const useCambiarAccesoGoogle = (
  onSuccess?: (configuracion: ConfiguracionAcceso) => void,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (googleHabilitado: boolean): Promise<ConfiguracionAcceso> =>
      api.put('/seguridad/configuracion-acceso', { googleHabilitado }),
    onSuccess: (configuracion) => {
      queryClient.setQueryData(CLAVE, configuracion);
      // La pantalla de inicio de sesión vuelve a preguntar si muestra el botón de Google.
      queryClient.invalidateQueries({ queryKey: ['auth', 'metodos'] });
      onSuccess?.(configuracion);
    },
  });
};
