import { useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';

import { Head } from '@/components/seo';
import { paths } from '@/config/paths';
import { CambiarContrasena } from '@/features/auth/components/cambiar-contrasena';
import { tokenStorage } from '@/lib/api-client';

// HU-5 · Figma > 🌟 Autenticación > Cambiar contraseña.
const CambiarContrasenaRoute = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  return (
    <>
      <Head title="Cambiar contraseña" />
      <CambiarContrasena
        onCambiada={() => {
          // El backend ya cerró todas las sesiones (también esta): se borra el token y lo que quedó en memoria.
          tokenStorage.clear();
          queryClient.clear();
          navigate(paths.auth.login.getHref(), {
            replace: true,
            state: { contrasenaActualizada: true },
          });
        }}
      />
    </>
  );
};

export default CambiarContrasenaRoute;
