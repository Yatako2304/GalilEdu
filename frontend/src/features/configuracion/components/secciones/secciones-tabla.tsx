import {
  EllipsisVertical,
  Pencil,
  Plus,
  Trash2,
  UserCheck,
} from 'lucide-react';
import * as React from 'react';

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { ProgressBar } from '@/components/ui/progress-bar';
import {
  TableBody,
  TableCell,
  TableElement,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { GradoConSecciones, SeccionResumen } from '@/types/api';

import { BotonSeccion } from './boton-seccion';
import {
  ESTADO_SECCION,
  etiquetaSeccion,
  porcentajeMatriculados,
  textoSeccionesActivas,
} from './formato';

// Figma > CI-09 > "HeaderRow": los anchos de cada columna (en px). Sin ellos la tabla reparte el espacio por
// contenido y "Capacidad máx." queda pegada a "Sección". Se apartan de Figma (250, 190 y 190 px) en Sección, Tutor y
// Cotutor: Sección cede 50 px para que los nombres de tutor y cotutor tengan más espacio (suman los mismos 1086 px).
const COLUMNAS = [
  { titulo: 'Sección', ancho: 'w-[200px]' },
  { titulo: 'Capacidad máx.', ancho: 'w-[130px]' },
  { titulo: 'Matriculados', ancho: 'w-[160px]' },
  { titulo: 'Tutor', ancho: 'w-[215px]' },
  { titulo: 'Cotutor', ancho: 'w-[215px]' },
  { titulo: 'Estado', ancho: 'w-[110px]' },
  { titulo: '', ancho: 'w-14' },
];

type SeccionesTablaProps = {
  grados: GradoConSecciones[];
  /** Con el año CERRADO no se agregan secciones ni se asignan tutores. */
  editable: boolean;
  onAgregarSeccion?: (grado: GradoConSecciones) => void;
  onAsignarTutor?: AccionDeSeccion;
  onEditar?: AccionDeSeccion;
  onEliminar?: AccionDeSeccion;
};

type AccionDeSeccion = (
  seccion: SeccionResumen,
  grado: GradoConSecciones,
) => void;

// Figma > CI-09 > "Table": una fila de grupo por grado ("1.° de primaria · 3 secciones activas · Agregar sección") y
// debajo sus secciones con capacidad, matriculados, tutor, cotutor, estado y menú ⋮. Las filas de grupo no caben en
// `Table` (que solo pinta filas de datos), por eso se arma con sus piezas.
export const SeccionesTabla = ({
  grados,
  editable,
  onAgregarSeccion,
  onAsignarTutor,
  onEditar,
  onEliminar,
}: SeccionesTablaProps) => (
  <TableElement>
    <TableHeader>
      <TableRow>
        {COLUMNAS.map(({ titulo, ancho }, indice) => (
          <TableHead key={titulo + indice} className={ancho}>
            {titulo}
          </TableHead>
        ))}
      </TableRow>
    </TableHeader>
    <TableBody>
      {grados.map((grado) => (
        <React.Fragment key={grado.codigo}>
          <TableRow className="bg-surface-secondary hover:bg-surface-secondary">
            <TableCell colSpan={COLUMNAS.length} className="py-2">
              <div className="flex items-center justify-between gap-4">
                <p className="flex items-baseline gap-2">
                  <span className="text-label-2 font-semibold text-content-primary">
                    {grado.nombre}
                  </span>
                  <span className="text-caption-1 text-content-tertiary">
                    {textoSeccionesActivas(grado.cantidadSeccionesActivas)}
                  </span>
                </p>
                <BotonSeccion
                  bloqueado={!editable}
                  size="sm"
                  variant="tertiary"
                  icon={<Plus className="size-3.5" aria-hidden="true" />}
                  onClick={() => onAgregarSeccion?.(grado)}
                >
                  Agregar sección
                </BotonSeccion>
              </div>
            </TableCell>
          </TableRow>
          {grado.secciones.map((seccion) => (
            <FilaSeccion
              key={seccion.id}
              grado={grado}
              seccion={seccion}
              editable={editable}
              onAsignarTutor={onAsignarTutor}
              onEditar={onEditar}
              onEliminar={onEliminar}
            />
          ))}
        </React.Fragment>
      ))}
    </TableBody>
  </TableElement>
);

/**
 * Un nombre largo se recorta con "…" en una sola línea (todas las filas miden lo mismo, como en Figma) y el nombre
 * completo sale al pasar el mouse. 183 px son los 215 px de la columna menos su relleno. Sin nombre se muestra "—".
 */
const NombreRecortado = ({ nombre }: { nombre: string | null }) =>
  nombre ? (
    <span className="block max-w-[183px] truncate" title={nombre}>
      {nombre}
    </span>
  ) : (
    <span>—</span>
  );

type FilaSeccionProps = {
  grado: GradoConSecciones;
  seccion: SeccionResumen;
  editable: boolean;
  onAsignarTutor?: SeccionesTablaProps['onAsignarTutor'];
  onEditar?: SeccionesTablaProps['onEditar'];
  onEliminar?: SeccionesTablaProps['onEliminar'];
};

const FilaSeccion = ({
  grado,
  seccion,
  editable,
  onAsignarTutor,
  onEditar,
  onEliminar,
}: FilaSeccionProps) => {
  const etiqueta = etiquetaSeccion(grado, seccion);
  const { texto, color } = ESTADO_SECCION[seccion.estado];
  return (
    <TableRow>
      <TableCell>
        <p className="text-label-2 font-medium text-content-primary">
          {etiqueta}
        </p>
        <p className="text-caption-1 text-content-tertiary">
          Sección {seccion.nombre}
        </p>
      </TableCell>
      <TableCell>{seccion.capacidadMaxima}</TableCell>
      {/* Figma: barra de 90 px + 8 px + el texto, a 16 px del borde y a 5 px del borde derecho de los 160 px. */}
      <TableCell className="pr-1">
        <div className="flex items-center gap-2">
          <div className="w-[90px] shrink-0">
            <ProgressBar
              value={porcentajeMatriculados(seccion)}
              label={`Matriculados de ${etiqueta}`}
              showLabel={false}
            />
          </div>
          <span className="whitespace-nowrap text-caption-1 text-content-secondary">
            {seccion.alumnosOcupanVacante} / {seccion.capacidadMaxima}
          </span>
        </div>
      </TableCell>
      <TableCell>
        <NombreRecortado nombre={seccion.tutor} />
      </TableCell>
      <TableCell>
        <NombreRecortado nombre={seccion.cotutor} />
      </TableCell>
      <TableCell>
        <Tag color={color} size="md">
          {texto}
        </Tag>
      </TableCell>
      <TableCell className="w-14">
        {editable && (
          <DropdownMenu>
            <DropdownMenuTrigger className="inline-flex size-8 items-center justify-center rounded-lg text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus">
              <EllipsisVertical className="size-4" aria-hidden="true" />
              <span className="sr-only">Acciones de {etiqueta}</span>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-44">
              <DropdownMenuItem
                onClick={() => onAsignarTutor?.(seccion, grado)}
              >
                <UserCheck className="mr-2 size-4" aria-hidden="true" /> Asignar
                tutor
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => onEditar?.(seccion, grado)}>
                <Pencil className="mr-2 size-4" aria-hidden="true" /> Editar
                sección
              </DropdownMenuItem>
              <DropdownMenuItem
                className="text-content-error focus:text-content-error"
                onClick={() => onEliminar?.(seccion, grado)}
              >
                <Trash2 className="mr-2 size-4" aria-hidden="true" /> Eliminar
                sección
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </TableCell>
    </TableRow>
  );
};
