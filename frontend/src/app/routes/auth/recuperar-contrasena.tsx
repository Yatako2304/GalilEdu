import { AuthLayout } from '@/components/layouts/auth-layout';
import { RecuperarContrasenaForm } from '@/features/auth/components/recuperar-contrasena-form';

const RecuperarContrasenaRoute = () => {
  return (
    <AuthLayout
      title="Restablecer contraseña"
      description="Ingrese su correo institucional y le enviaremos un enlace para crear una nueva contraseña."
    >
      <RecuperarContrasenaForm />
    </AuthLayout>
  );
};

export default RecuperarContrasenaRoute;
