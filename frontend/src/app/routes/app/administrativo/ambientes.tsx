import { CirclePlus } from 'lucide-react';
import * as React from 'react';

import { ContentLayout } from '@/components/layouts';
import { Button } from '@/components/ui/button';
import { Ambientes } from '@/features/catalogo-academico/components/ambientes';
import { DialogoAmbiente } from '@/features/catalogo-academico/components/dialogos-ambientes';
import { AmbienteResumen } from '@/types/api';

// Figma > Personal Administrativo > "CI-26 · Ambientes físicos (HU-43)" (nodo 857:10215)
// y modal "Nuevo ambiente físico" (nodo 857:11500).
const AdministrativoAmbientesRoute = () => {
  // null = modal cerrado; { ambiente: null } = ambiente nuevo.
  const [modal, setModal] = React.useState<{
    ambiente: AmbienteResumen | null;
  } | null>(null);

  return (
    <ContentLayout
      title="Ambientes físicos"
      breadcrumbs={['Configuración', 'Ambientes físicos']}
      subtitle="Registra las aulas y espacios especiales del colegio con su aforo y recursos. Se usan al asignar aulas a las secciones y al generar horarios."
      actions={
        <Button
          icon={<CirclePlus className="size-4" />}
          onClick={() => setModal({ ambiente: null })}
        >
          Nuevo ambiente
        </Button>
      }
    >
      <Ambientes onEditar={(ambiente) => setModal({ ambiente })} />
      {modal && (
        // La llave reinicia el formulario si se abre otro ambiente sin cerrar el modal.
        <DialogoAmbiente
          key={modal.ambiente?.id ?? 'nuevo'}
          ambiente={modal.ambiente}
          onCerrar={() => setModal(null)}
        />
      )}
    </ContentLayout>
  );
};

export default AdministrativoAmbientesRoute;
