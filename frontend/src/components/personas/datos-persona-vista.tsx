import { SelectOption } from '@/components/ui/form';
import { ListaDatos } from '@/components/ui/lista-datos';
import { SEXOS, TIPOS_DOCUMENTO } from '@/config/catalogos';
import { DatosPersona } from '@/types/api';
import { formatDay } from '@/utils/format';

const etiqueta = (opciones: SelectOption[], v?: string | null) =>
  opciones.find((o) => o.value === v)?.label ?? v;

/**
 * Datos personales de solo lectura, con las mismas etiquetas que el formulario (`PersonaFields`). Se usa en «Mis datos»
 * (antes de pulsar «Editar datos») y en las pestañas Padre, Madre y Apoderado de «Datos de la Familia».
 */
export const DatosPersonaVista = ({
  persona: p,
  conExalumno = false,
  soloIdentidad = false,
}: {
  persona: DatosPersona;
  /** «Es exalumno(a)» solo se pide a padres y apoderados. */
  conExalumno?: boolean;
  /** Padre o madre fallecido: solo se registró su identidad. */
  soloIdentidad?: boolean;
}) => (
  <div className="divide-y divide-stroke-tertiary [&>section:first-child]:pt-0 [&>section:last-child]:pb-0 [&>section]:py-5">
    <ListaDatos
      titulo="Identidad"
      datos={[
        { etiqueta: 'Nombres', valor: p.nombres },
        { etiqueta: 'Apellido paterno', valor: p.apellidoPaterno },
        { etiqueta: 'Apellido materno', valor: p.apellidoMaterno },
        {
          etiqueta: 'Documento',
          valor: `${etiqueta(TIPOS_DOCUMENTO, p.tipoDocumento)} ${p.numeroDocumento}`,
        },
        {
          etiqueta: 'Fecha de nacimiento',
          valor: p.fechaNacimiento && formatDay(p.fechaNacimiento),
        },
        { etiqueta: 'Sexo', valor: etiqueta(SEXOS, p.sexo) },
        { etiqueta: 'Nacionalidad', valor: p.nacionalidad },
        { etiqueta: 'País de nacimiento', valor: p.lugarNacimiento },
        ...(soloIdentidad
          ? []
          : [
              { etiqueta: 'Estado civil', valor: p.estadoCivil },
              { etiqueta: 'Grado de instrucción', valor: p.gradoInstruccion },
            ]),
        ...(conExalumno && !soloIdentidad
          ? [
              {
                etiqueta: 'Exalumno(a) del colegio',
                valor: p.esExalumno ? 'Sí' : 'No',
              },
            ]
          : []),
      ]}
    />
    {!soloIdentidad && (
      <>
        <ListaDatos
          titulo="Contacto y domicilio"
          datos={[
            { etiqueta: 'Celular', valor: p.celular },
            { etiqueta: 'Correo electrónico', valor: p.correo },
            { etiqueta: 'Teléfono de casa', valor: p.telefonoCasa },
            { etiqueta: 'Distrito', valor: p.distrito },
            { etiqueta: 'Dirección', valor: p.direccion },
          ]}
        />
        <ListaDatos
          titulo="Trabajo"
          datos={[
            { etiqueta: 'Profesión u ocupación', valor: p.profesion },
            { etiqueta: 'Centro de trabajo', valor: p.centroTrabajo },
            { etiqueta: 'Cargo', valor: p.cargo },
          ]}
        />
      </>
    )}
  </div>
);
