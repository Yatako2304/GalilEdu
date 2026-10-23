import { useNavigate, useSearchParams } from 'react-router';

import { AuthLayout } from '@/components/layouts/auth-layout';
import { paths } from '@/config/paths';
import { LoginForm } from '@/features/auth/components/login-form';
import { destinoSeguro } from '@/lib/auth';

const LoginRoute = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const redirectTo = destinoSeguro(searchParams.get('redirectTo'));

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
    </AuthLayout>
  );
};

export default LoginRoute;
