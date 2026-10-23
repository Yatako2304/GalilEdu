import { Plus } from 'lucide-react';
import * as React from 'react';

import { Alert } from '@/components/ui/alert';
import { Card } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import {
  EstadoAnioEscolar,
  GradoConSecciones,
  SeccionResumen,
} from '@/types/api';

import { useGradosConSecciones } from '../../api/secciones';

import { BotonSeccion } from './boton-seccion';
import { DialogoEditarSeccion } from './dialogo-editar-seccion';
import { DialogoEliminarSeccion } from './dialogo-eliminar-seccion';
import { DialogoSeccion } from './dialogo-seccion';
import { DialogoTutor } from './dialogo-tutor';
import { etiquetaSeccion } from './formato';
import { SeccionesTabla } from './secciones-tabla';

export type SeccionesPestanaProps = {
  anioId: string;
  estadoAnio: EstadoAnioEscolar;
};

// Figma > Personal Administrativo > CI-09 · Secciones por grado y tutores (HU-75): "Card - Secciones" con el botón
// "Nueva sección" y la tabla agrupada por grado. Solo un año CERRADO bloquea los cambios (un año ACTIVO admite
// secciones nuevas y tutores).
export const SeccionesPestana = ({
  anioId,
  estadoAnio,
}: SeccionesPestanaProps) => {
  const grados = useGradosConSecciones(anioId);
  const editable = estadoAnio !== 'CERRADO';

  // El modal "Nueva sección" se abre sin grado desde el botón de arriba, y con el grado elegido desde el "Agregar
  // sección" de cada grado. El grado se conserva al cerrar para que no cambie mientras el modal se oculta.
  const [nuevaSeccion, setNuevaSeccion] = React.useState<{
    abierta: boolean;
    gradoCodigo?: string;
  }>({ abierta: false });
  const abrirNuevaSeccion = (gradoCodigo?: string) =>
    setNuevaSeccion({ abierta: true, gradoCodigo });

  // Asignar tutor, Editar sección y Eliminar sección se abren desde el menú ⋮ de una sección.
  const [accion, setAccion] = React.useState<{
    tipo: 'tutor' | 'editar' | 'eliminar';
    seccion: SeccionResumen;
    grado: GradoConSecciones;
  } | null>(null);
  const abrirAccion =
    (tipo: 'tutor' | 'editar' | 'eliminar') =>
    (seccion: SeccionResumen, grado: GradoConSecciones) =>
      setAccion({ tipo, seccion, grado });
  const cerrarAccion = () => setAccion(null);

  return (
    <Card className="flex flex-col gap-4 p-6">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-label-1 font-semibold text-content-primary">
          Secciones por grado
        </h2>
        {/* Sin la lista de grados cargada el modal no tendría opciones que ofrecer. */}
        <BotonSeccion
          bloqueado={!editable}
          disabled={!grados.data}
          icon={<Plus className="size-4" aria-hidden="true" />}
          onClick={() => abrirNuevaSeccion()}
        >
          Nueva sección
        </BotonSeccion>
      </div>

      {grados.data ? (
        <SeccionesTabla
          grados={grados.data}
          editable={editable}
          onAgregarSeccion={(grado) => abrirNuevaSeccion(grado.codigo)}
          onAsignarTutor={abrirAccion('tutor')}
          onEditar={abrirAccion('editar')}
          onEliminar={abrirAccion('eliminar')}
        />
      ) : grados.isError ? (
        <Alert type="error" title="No pudimos cargar las secciones.">
          Vuelve a intentarlo en unos minutos.
        </Alert>
      ) : (
        <div className="flex h-60 items-center justify-center">
          <Spinner />
        </div>
      )}

      {editable && grados.data && (
        <DialogoSeccion
          abierto={nuevaSeccion.abierta}
          onCerrar={() =>
            setNuevaSeccion((actual) => ({ ...actual, abierta: false }))
          }
          anioId={anioId}
          grados={grados.data}
          gradoCodigo={nuevaSeccion.gradoCodigo}
        />
      )}

      {editable && accion?.tipo === 'tutor' && (
        <DialogoTutor
          anioId={anioId}
          seccion={accion.seccion}
          etiqueta={etiquetaSeccion(accion.grado, accion.seccion)}
          onCerrar={cerrarAccion}
        />
      )}
      {editable && accion?.tipo === 'editar' && (
        <DialogoEditarSeccion
          seccion={accion.seccion}
          etiqueta={etiquetaSeccion(accion.grado, accion.seccion)}
          onCerrar={cerrarAccion}
        />
      )}
      {editable && accion?.tipo === 'eliminar' && (
        <DialogoEliminarSeccion
          seccion={accion.seccion}
          etiqueta={etiquetaSeccion(accion.grado, accion.seccion)}
          onCerrar={cerrarAccion}
        />
      )}
    </Card>
  );
};
