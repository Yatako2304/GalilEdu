import { Outlet } from 'react-router';

import { DashboardLayout } from '@/components/layouts';

export const ErrorBoundary = () => {
  return (
    <div className="p-8 text-body-3 text-content-error">
      Ocurrió un error al cargar esta página.
    </div>
  );
};

const AppRoot = () => {
  return (
    <DashboardLayout>
      <Outlet />
    </DashboardLayout>
  );
};

export default AppRoot;
