import { Plus } from 'lucide-react';
import * as React from 'react';
import { useNavigate } from 'react-router';

import { Button } from '@/components/ui/button';
import { paths } from '@/config/paths';

import { useAniosEscolares } from '../../api/anios-escolares';

import { DialogoAnio } from './dialogo-anio';

// Figma > CI-01 · Año escolar > botón "Nuevo año escolar" y su modal. Al crear, abre el año nuevo.
export const BotonNuevoAnio = () => {
  const [abierto, setAbierto] = React.useState(false);
  const anios = useAniosEscolares();
  const navigate = useNavigate();

  return (
    <>
      {/* El modal propone el año siguiente al último registrado, así que espera a conocer la lista. */}
      <Button
        icon={<Plus className="size-4" aria-hidden="true" />}
        disabled={anios.isLoading}
        onClick={() => setAbierto(true)}
      >
        Nuevo año escolar
      </Button>
      <DialogoAnio
        abierto={abierto}
        onCerrar={() => setAbierto(false)}
        modo="nuevo"
        aniosRegistrados={anios.data ?? []}
        onGuardado={(creado) =>
          navigate(
            paths.app.administrativo.anioEscolarDetalle.getHref(creado.id),
          )
        }
      />
    </>
  );
};
