import * as React from 'react';
import { useNavigate, useSearchParams } from 'react-router';

import logo from '@/assets/logo.png';
import { Head } from '@/components/seo';
import { useUser } from '@/lib/auth';
import { getDefaultHomeHref } from '@/lib/authorization';

type LayoutProps = {
  children: React.ReactNode;
  title: string;
  description?: string;
};

// Figma > 🌟 Autenticación: panel azul a la izquierda + formulario centrado a la derecha.
export const AuthLayout = ({ children, title, description }: LayoutProps) => {
  const user = useUser();
  const [searchParams] = useSearchParams();
  const redirectTo = searchParams.get('redirectTo');

  const navigate = useNavigate();

  React.useEffect(() => {
    if (user.data) {
      navigate(redirectTo ? redirectTo : getDefaultHomeHref(user.data), {
        replace: true,
      });
    }
  }, [user.data, navigate, redirectTo]);

  return (
    <>
      <Head title={title} />
      <div className="flex min-h-screen bg-white">
        <div className="hidden p-6 lg:block lg:w-[47%]">
          <div className="h-full rounded-xl bg-gradient-to-br from-vibrant-blue-500 to-vibrant-blue-600" />
        </div>

        <div className="flex flex-1 items-center justify-center px-4 py-12">
          <div className="w-full max-w-[400px]">
            <img
              src={logo}
              alt="Galiledu"
              className="mx-auto h-[72px] w-auto object-contain"
            />

            <div className="mt-10">
              <h1 className="text-h6 font-semibold text-content-primary">
                {title}
              </h1>
              {description && (
                <p className="mt-2 text-body-4 text-content-tertiary">
                  {description}
                </p>
              )}
            </div>

            <div className="mt-6">{children}</div>
          </div>
        </div>
      </div>
    </>
  );
};
