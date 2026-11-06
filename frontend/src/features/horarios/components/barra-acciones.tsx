import { Button } from '@/components/ui/button';

type BarraAccionesProps = {
  editando: boolean;
  /** Con el año cerrado no se puede editar y la barra no aparece. */
  editable: boolean;
  hayCambios: boolean;
  guardando: boolean;
  onEditar: () => void;
  onCancelar: () => void;
  onGuardar: () => void;
};

// Figma > HU-45 > CTA al pie de la página: «Cancelar» y «Guardar cambios». «Editar horario» (modo lectura) no está en
// Figma: se agregó en la misma barra.
export const BarraAcciones = ({
  editando,
  editable,
  hayCambios,
  guardando,
  onEditar,
  onCancelar,
  onGuardar,
}: BarraAccionesProps) => {
  if (!editable) return null;

  return (
    <div className="flex items-center justify-end gap-2">
      {editando ? (
        <>
          {hayCambios && (
            <p role="status" className="mr-2 text-label-2 text-content-warning">
              Tienes cambios sin guardar
            </p>
          )}
          <Button variant="tertiary" onClick={onCancelar} disabled={guardando}>
            Cancelar
          </Button>
          <Button
            onClick={onGuardar}
            disabled={!hayCambios || guardando}
            isLoading={guardando}
          >
            Guardar cambios
          </Button>
        </>
      ) : (
        <Button onClick={onEditar}>Editar horario</Button>
      )}
    </div>
  );
};
