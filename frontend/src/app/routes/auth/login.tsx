import { useLocation, useNavigate, useSearchParams } from 'react-router';

import { AuthLayout } from '@/components/layouts/auth-layout';
import { paths } from '@/config/paths';
import { AvisoContrasena } from '@/features/auth/components/aviso-contrasena';
import { LoginForm } from '@/features/auth/components/login-form';
import { destinoSeguro } from '@/lib/auth';

const LoginRoute = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const redirectTo = destinoSeguro(searchParams.get('redirectTo'));
  // HU-4 y HU-5: al volver de restablecer o cambiar la contraseña se muestra el aviso de Figma una sola vez.
  const location = useLocation();
  const estado = location.state as {
    contrasenaRestablecida?: boolean;
    contrasenaActualizada?: boolean;
  } | null;
  const aviso = estado?.contrasenaActualizada
    ? 'actualizada'
    : estado?.contrasenaRestablecida
      ? 'restablecida'
      : null;

  return (
    <AuthLayout
      title="Iniciar sesión"
      description="Ingrese sus credenciales institucionales"
    >
      <LoginForm
        onSuccess={() => {
          // Sin redirectTo, AuthLayout envía al inicio del rol del usuario
          navigate(redirectTo ?? paths.app.root.getHref(), { replace: true });
        }}
      />
      <AvisoContrasena
        tipo={aviso}
        onClose={() =>
          navigate(`${location.pathname}${location.search}`, {
            replace: true,
            state: null,
          })
        }
      />
    </AuthLayout>
  );
};

export default LoginRoute;
