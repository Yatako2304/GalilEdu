import * as React from 'react';

import { Head } from '../seo';

import { SidebarToggle } from './dashboard-layout';

type ContentLayoutProps = {
  children: React.ReactNode;
  title: string;
  /** Migas de pan; por defecto solo el título (Figma: "Item" sobre el título). */
  breadcrumbs?: string[];
  subtitle?: string;
  /** Botones a la derecha del título (Figma > Page Header: 1 o 2 botones). */
  actions?: React.ReactNode;
};

// Encabezado de página + contenedor (Figma: Header + PageTitle, padding 32px, max 1200px).
export const ContentLayout = ({
  children,
  title,
  breadcrumbs,
  subtitle,
  actions,
}: ContentLayoutProps) => {
  const crumbs = breadcrumbs ?? [title];

  return (
    <>
      <Head title={title} />
      <div className="mx-auto w-full max-w-content px-4 py-6 md:p-8">
        <div className="flex items-center gap-2">
          <SidebarToggle />
          <span className="h-4 w-px bg-stroke-secondary" aria-hidden="true" />
          <nav aria-label="Ruta" className="truncate text-body-4">
            {crumbs.map((crumb, index) => (
              <span key={crumb + index}>
                {index > 0 && (
                  <span className="mx-1 text-content-placeholder">/</span>
                )}
                <span
                  className={
                    index === crumbs.length - 1
                      ? 'text-content-primary'
                      : 'text-content-quartiary'
                  }
                >
                  {crumb}
                </span>
              </span>
            ))}
          </nav>
        </div>

        <div className="mt-3 flex flex-wrap items-end justify-between gap-4">
          <div className="min-w-0">
            <h1 className="text-h4 font-semibold text-content-primary">
              {title}
            </h1>
            {subtitle && (
              <p className="mt-1 text-body-3 text-content-quartiary">
                {subtitle}
              </p>
            )}
          </div>
          {actions && <div className="flex gap-3">{actions}</div>}
        </div>

        <div className="mt-8 flex flex-col gap-8">{children}</div>
      </div>
    </>
  );
};
