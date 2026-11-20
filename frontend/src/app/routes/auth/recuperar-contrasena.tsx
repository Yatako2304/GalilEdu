import { useNavigate } from 'react-router';

import { Head } from '@/components/seo';
import { paths } from '@/config/paths';
import { RecuperarContrasena } from '@/features/auth/components/recuperar-contrasena';

// HU-4 · Figma > 🌟 Autenticación > Restablecer contraseña: pantalla sin el panel azul, con la tarjeta centrada.
const RecuperarContrasenaRoute = () => {
  const navigate = useNavigate();
  return (
    <>
      <Head title="Restablecer contraseña" />
      <RecuperarContrasena
        onRestablecida={() =>
          navigate(paths.auth.login.getHref(), {
            replace: true,
            state: { contrasenaRestablecida: true },
          })
        }
      />
    </>
  );
};

export default RecuperarContrasenaRoute;
