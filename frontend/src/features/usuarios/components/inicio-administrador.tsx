import {
  CalendarDays,
  ChevronRight,
  FileSpreadsheet,
  UserPlus,
  UserRoundCog,
  Users,
} from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Link } from '@/components/ui/link';
import { Spinner } from '@/components/ui/spinner';
import { Tag } from '@/components/ui/tag';
import { formatDay } from '@/utils/format';

import { useResumenUsuarios } from '../api/get-usuario';

import { NOMBRE_ROL } from './rol-tag';

type InicioAdministradorProps = {
  hrefUsuarios: string;
  hrefNuevoUsuario: string;
  hrefInactivos: string;
};

const Estadistica = ({
  icono,
  valor,
  etiqueta,
  pista,
}: {
  icono: React.ReactNode;
  valor: React.ReactNode;
  etiqueta: string;
  pista?: React.ReactNode;
}) => (
  <Card className="flex flex-col gap-3 p-5">
    <span className="inline-flex size-9 items-center justify-center rounded-lg bg-surface-accent-light text-content-accent-primary">
      {icono}
    </span>
    <p className="text-h4 font-semibold text-content-primary">{valor}</p>
    <div className="flex items-center justify-between gap-2">
      <p className="text-body-3 text-content-tertiary">{etiqueta}</p>
      {pista && (
        <p className="text-body-4 font-medium text-content-success">{pista}</p>
      )}
    </div>
  </Card>
);

const AccesoRapido = ({
  to,
  icono,
  titulo,
  descripcion,
}: {
  to: string;
  icono: React.ReactNode;
  titulo: string;
  descripcion: string;
}) => (
  <Link
    to={to}
    className="flex items-center gap-3 rounded-[12px] border border-stroke-tertiary bg-white p-4 text-content-primary no-underline transition-colors hover:bg-surface-secondary hover:no-underline"
  >
    <span className="inline-flex size-9 shrink-0 items-center justify-center rounded-lg bg-surface-tertiary text-content-tertiary">
      {icono}
    </span>
    <span className="min-w-0 flex-1">
      <span className="block text-label-2 font-semibold">{titulo}</span>
      <span className="block text-body-4 text-content-tertiary">
        {descripcion}
      </span>
    </span>
    <ChevronRight className="size-4 text-content-tertiary" aria-hidden="true" />
  </Link>
);

// Figma > Administrador > "00 · Home — Administrador"
export const InicioAdministrador = ({
  hrefUsuarios,
  hrefNuevoUsuario,
  hrefInactivos,
}: InicioAdministradorProps) => {
  const resumen = useResumenUsuarios();

  if (resumen.isLoading) {
    return (
      <div className="flex h-60 items-center justify-center">
        <Spinner />
      </div>
    );
  }
  const r = resumen.data;
  if (!r) return null;

  return (
    <>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <Estadistica
          icono={<Users className="size-4" aria-hidden="true" />}
          valor={r.total}
          etiqueta="Usuarios registrados"
          pista={`${r.activos} activos`}
        />
        <Estadistica
          icono={<UserRoundCog className="size-4" aria-hidden="true" />}
          valor={r.inactivos}
          etiqueta={
            r.inactivos === 1 ? 'Usuario inactivo' : 'Usuarios inactivos'
          }
          pista={r.cuentasInactivas[0]?.nombre}
        />
        <Estadistica
          icono={<CalendarDays className="size-4" aria-hidden="true" />}
          valor={r.ultimoAcceso ? formatDay(r.ultimoAcceso.fecha) : '—'}
          etiqueta="Último acceso registrado"
        />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card className="p-5">
          <h2 className="text-h6 font-semibold text-content-primary">
            Usuarios por rol
          </h2>
          <p className="text-body-4 text-content-tertiary">
            Distribución actual de cuentas
          </p>
          <ul className="mt-3 divide-y divide-stroke-tertiary border-t border-stroke-tertiary">
            {r.porRol.map((g) => (
              <li
                key={g.rol}
                className="flex items-center justify-between gap-4 py-3"
              >
                <div className="min-w-0">
                  <p className="text-body-3 text-content-primary">
                    {NOMBRE_ROL[g.rol]}
                  </p>
                  <p className="truncate text-body-4 text-content-tertiary">
                    {g.usuarios.join(', ')}
                  </p>
                </div>
                <Tag color="info" size="sm" className="rounded-full">
                  {g.cantidad}
                </Tag>
              </li>
            ))}
          </ul>
          <Button asChild variant="primary-light" size="sm" className="mt-3">
            <Link to={hrefUsuarios}>Ver usuarios</Link>
          </Button>
        </Card>

        <Card className="self-start p-5">
          <h2 className="text-h6 font-semibold text-content-primary">
            Cuentas inactivas
          </h2>
          <p className="text-body-4 text-content-tertiary">
            No pueden iniciar sesión hasta que las reactives
          </p>
          {r.cuentasInactivas.length === 0 ? (
            <p className="mt-4 text-body-3 text-content-tertiary">
              No hay cuentas inactivas.
            </p>
          ) : (
            <ul className="mt-3 divide-y divide-stroke-tertiary border-t border-stroke-tertiary">
              {r.cuentasInactivas.slice(0, 4).map((u) => (
                <li
                  key={u.id}
                  className="flex items-center justify-between gap-4 py-3"
                >
                  <div className="min-w-0">
                    <p className="text-body-3 text-content-primary">
                      {u.nombre}
                    </p>
                    <p className="truncate text-body-4 text-content-tertiary">
                      {u.correo} ·{' '}
                      {u.roles.map((rol) => NOMBRE_ROL[rol]).join(', ')}
                    </p>
                  </div>
                  <Tag color="neutral" size="sm" className="rounded-full">
                    Inactivo
                  </Tag>
                </li>
              ))}
            </ul>
          )}
          <Button asChild variant="primary-light" size="sm" className="mt-3">
            <Link to={hrefInactivos}>Revisar cuentas</Link>
          </Button>
        </Card>
      </div>

      <section className="flex flex-col gap-3">
        <h2 className="text-h6 font-semibold text-content-primary">
          Accesos rápidos
        </h2>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <AccesoRapido
            to={hrefUsuarios}
            icono={<Users className="size-4" aria-hidden="true" />}
            titulo="Usuarios"
            descripcion="Consulta, edita y activa o desactiva cuentas"
          />
          <AccesoRapido
            to={hrefNuevoUsuario}
            icono={<UserPlus className="size-4" aria-hidden="true" />}
            titulo="Nuevo usuario"
            descripcion="Crea una cuenta y asigna su rol"
          />
          <AccesoRapido
            to={hrefUsuarios}
            icono={<FileSpreadsheet className="size-4" aria-hidden="true" />}
            titulo="Carga masiva"
            descripcion="Registra varios usuarios con una plantilla (HU-8, próximamente)"
          />
        </div>
      </section>
    </>
  );
};
