import { Navigate } from 'react-router';

import { useUser } from '@/lib/auth';
import { getDefaultHomeHref } from '@/lib/authorization';

// /app -> inicio del primer rol del usuario
const RedirectToHomeRoute = () => {
  const user = useUser();

  if (!user.data) return null;

  return <Navigate to={getDefaultHomeHref(user.data)} replace />;
};

export default RedirectToHomeRoute;
