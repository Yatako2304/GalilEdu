import { zodResolver } from '@hookform/resolvers/zod';
import { BarChart3 } from 'lucide-react';
import { useForm } from 'react-hook-form';

import { Input } from '@/components/ui/form';
import { NivelLogro, TablaConversionDetalle } from '@/types/api';
import { cn } from '@/utils/cn';

import { useCrearTablaConversion } from '../api/escala-calificacion';

import { BarraNiveles, InsigniaNivel } from './barra-niveles';
import { DialogoCatalogo } from './dialogo-catalogo';
import { NuevaVersionFormulario, nuevaVersionSchema } from './esquemas';

const SIGNIFICADO: Record<NivelLogro, string> = {
  AD: 'Logro destacado',
  A: 'Logro esperado',
  B: 'En proceso',
  C: 'En inicio',
};

const NIVELES: NivelLogro[] = ['AD', 'A', 'B', 'C'];

/** Parte de la versión vigente; si no hay, de la tabla de Figma. */
const valoresIniciales = (
  base: TablaConversionDetalle | undefined,
): NuevaVersionFormulario => ({
  vigenteDesde: '',
  niveles: NIVELES.map((nivel) => {
    const actual = base?.niveles.find((n) => n.nivel === nivel);
    const porDefecto = { AD: [18, 20], A: [14, 17], B: [11, 13], C: [0, 10] }[
      nivel
    ];
    return {
      nivel,
      notaDesde: actual?.notaDesde ?? porDefecto[0],
      notaHasta: actual?.notaHasta ?? porDefecto[1],
    };
  }),
});

const campoNota =
  'h-9 w-20 rounded-xl border border-stroke-primary bg-white px-3 text-body-3 focus-visible:border-stroke-focus focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-deep-blue-100 read-only:bg-surface-tertiary read-only:text-content-quartiary';

type DialogoNuevaVersionProps = {
  vigente: TablaConversionDetalle | undefined;
  onCerrar: () => void;
};

// Figma CI-23 > "Modal - Nueva versión de la tabla de conversión" (HU-55)
export const DialogoNuevaVersion = ({
  vigente,
  onCerrar,
}: DialogoNuevaVersionProps) => {
  const form = useForm<NuevaVersionFormulario>({
    resolver: zodResolver(nuevaVersionSchema),
    defaultValues: valoresIniciales(vigente),
  });
  const crear = useCrearTablaConversion(onCerrar);
  const niveles = form.watch('niveles').map((n) => ({
    nivel: n.nivel,
    notaDesde: Number(n.notaDesde),
    notaHasta: Number(n.notaHasta),
  }));
  const errores = form.formState.errors;

  return (
    <DialogoCatalogo
      open
      onCerrar={onCerrar}
      icono={<BarChart3 className="size-5" aria-hidden="true" />}
      titulo="Nueva versión de la tabla de conversión"
      descripcion="Los rangos deben cubrir de 0 a 20 sin vacíos ni cruces. La versión vigente pasa al historial cuando empiece la nueva."
      formId="form-nueva-version"
      textoGuardar="Guardar versión"
      guardando={crear.isPending}
      className="max-w-[760px]"
    >
      <form
        noValidate
        id="form-nueva-version"
        className="space-y-5"
        onSubmit={form.handleSubmit((valores) =>
          crear.mutate(nuevaVersionSchema.parse(valores)),
        )}
      >
        <Input
          type="date"
          label="Vigente desde *"
          hint="Si eliges una fecha futura, la versión quedará programada."
          registration={form.register('vigenteDesde')}
          error={errores.vigenteDesde}
        />

        <table className="w-full text-left">
          <thead className="bg-surface-secondary text-label-3 text-content-tertiary">
            <tr>
              <th className="px-3 py-2 font-medium">Nivel</th>
              <th className="px-3 py-2 font-medium">Significado</th>
              <th className="px-3 py-2 font-medium">Desde</th>
              <th className="px-3 py-2 font-medium">Hasta</th>
            </tr>
          </thead>
          <tbody>
            {NIVELES.map((nivel, i) => (
              <tr key={nivel} className="border-b border-stroke-tertiary">
                <td className="px-3 py-2">
                  <InsigniaNivel nivel={nivel} />
                </td>
                <td className="px-3 py-2 text-body-3 text-content-secondary">
                  {SIGNIFICADO[nivel]}
                </td>
                <td className="px-3 py-2">
                  <input
                    type="number"
                    min={0}
                    max={20}
                    aria-label={`${nivel} desde`}
                    // C siempre empieza en 0.
                    readOnly={nivel === 'C'}
                    className={campoNota}
                    {...form.register(`niveles.${i}.notaDesde`)}
                  />
                </td>
                <td className="px-3 py-2">
                  <input
                    type="number"
                    min={0}
                    max={20}
                    aria-label={`${nivel} hasta`}
                    // AD siempre termina en 20.
                    readOnly={nivel === 'AD'}
                    className={campoNota}
                    {...form.register(`niveles.${i}.notaHasta`)}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {errores.niveles && (
          <p role="alert" className={cn('text-body-4 text-content-error')}>
            {errores.niveles.message ??
              errores.niveles.root?.message ??
              'Revisa las notas: van de 0 a 20.'}
          </p>
        )}

        <div className="space-y-2">
          <p className="text-label-3 font-medium text-content-primary">
            Vista previa
          </p>
          <BarraNiveles niveles={niveles} />
        </div>
      </form>
    </DialogoCatalogo>
  );
};
