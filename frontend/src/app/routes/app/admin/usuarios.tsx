import { CirclePlus } from 'lucide-react';
import * as React from 'react';
import { useSearchParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { Button } from '@/components/ui/button';
import {
  DialogoUsuario,
  ModoDialogo,
} from '@/features/usuarios/components/dialogo-usuario';
import { DialogoUsuarioRegistrado } from '@/features/usuarios/components/dialogo-usuario-registrado';
import { UsuariosLista } from '@/features/usuarios/components/usuarios-lista';
import { EstadoUsuario, UsuarioCreado } from '@/types/api';

// Figma > Administrador > "Admin / Usuarios | HU-6 | HU-7 | HU-8 - grilla" (HU-6, HU-8, HU-9, HU-12)
const AdminUsuariosRoute = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [dialogo, setDialogo] = React.useState<ModoDialogo | null>(
    searchParams.get('nuevo') ? { modo: 'crear' } : null,
  );
  const [creado, setCreado] = React.useState<UsuarioCreado | null>(null);

  /** Quita ?nuevo=1 (acceso rápido del Inicio) al cerrar el modal. */
  const limpiarParametro = (nombre: string) => {
    if (searchParams.has(nombre)) {
      searchParams.delete(nombre);
      setSearchParams(searchParams, { replace: true });
    }
  };

  const cerrar = () => {
    setDialogo(null);
    limpiarParametro('nuevo');
  };

  return (
    <ContentLayout
      title="Usuarios"
      subtitle="Administra las cuentas de los usuarios del sistema: crea nuevos usuarios, asigna roles y activa o desactiva su acceso."
      actions={
        <Button
          icon={<CirclePlus className="size-4" />}
          onClick={() => setDialogo({ modo: 'crear' })}
        >
          Nuevo usuario
        </Button>
      }
    >
      <UsuariosLista
        onAbrir={setDialogo}
        estadoInicial={(searchParams.get('estado') as EstadoUsuario) ?? ''}
      />
      <DialogoUsuario estado={dialogo} onCerrar={cerrar} onCreado={setCreado} />
      <DialogoUsuarioRegistrado
        usuario={creado}
        onCerrar={() => setCreado(null)}
      />
    </ContentLayout>
  );
};

export default AdminUsuariosRoute;
