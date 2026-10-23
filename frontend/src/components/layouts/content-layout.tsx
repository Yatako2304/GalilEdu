import * as React from 'react';

import { Head } from '../seo';
import { Link } from '../ui/link';

import { SidebarToggle } from './dashboard-layout';

/** Un tramo de las migas de pan: texto, o texto con enlace para volver a ese nivel (Figma: Item de la miga). */
export type Miga = string | { label: string; to: string };

type ContentLayoutProps = {
  children: React.ReactNode;
  title: string;
  /**
   * Migas de pan; por defecto solo el título (Figma: "Item" sobre el título). El último tramo es la página actual y
   * nunca lleva enlace.
   */
  breadcrumbs?: Miga[];
  subtitle?: string;
  /** Etiqueta de estado junto al título. */
  titleTag?: React.ReactNode;
  /** Botones a la derecha del título (Figma > Page Header: 1 o 2 botones). */
  actions?: React.ReactNode;
};

// Encabezado de página + contenedor (Figma: Header + PageTitle, padding 32px, max 1200px).
export const ContentLayout = ({
  children,
  title,
  breadcrumbs,
  subtitle,
  titleTag,
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
            {crumbs.map((crumb, index) => {
              const etiqueta = typeof crumb === 'string' ? crumb : crumb.label;
              const esUltimo = index === crumbs.length - 1;
              return (
                <span key={etiqueta + index}>
                  {index > 0 && (
                    <span className="mx-1 text-content-placeholder">/</span>
                  )}
                  {typeof crumb !== 'string' && !esUltimo ? (
                    <Link
                      to={crumb.to}
                      className="text-content-quartiary hover:text-content-primary hover:underline"
                    >
                      {etiqueta}
                    </Link>
                  ) : (
                    <span
                      className={
                        esUltimo
                          ? 'text-content-primary'
                          : 'text-content-quartiary'
                      }
                      aria-current={esUltimo ? 'page' : undefined}
                    >
                      {etiqueta}
                    </span>
                  )}
                </span>
              );
            })}
          </nav>
        </div>

        <div className="mt-3 flex flex-wrap items-end justify-between gap-4">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-h4 font-semibold text-content-primary">
                {title}
              </h1>
              {titleTag}
            </div>
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
