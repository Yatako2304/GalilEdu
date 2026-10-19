import { Navigate } from 'react-router';

import { paths } from '@/config/paths';
import { useUser } from '@/lib/auth';
import { getDefaultHomeHref } from '@/lib/authorization';

// "/" no tiene landing en Figma: manda al login o al inicio del usuario.
const LandingRoute = () => {
  const user = useUser();

  return (
    <Navigate
      to={
        user.data ? getDefaultHomeHref(user.data) : paths.auth.login.getHref()
      }
      replace
    />
  );
};

export default LandingRoute;
