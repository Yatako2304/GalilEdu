import { Alert } from '@/components/ui/alert';
import { Spinner } from '@/components/ui/spinner';

import {
  anioActivo,
  useAnioEscolarActual,
  useAniosEscolares,
} from '../../api/anios-escolares';

import { AniosTabla } from './anios-tabla';
import { CardAnioEnCurso } from './card-anio-en-curso';

// Figma > Personal Administrativo > CI-01 · Año escolar (HU-74): el año en curso y la tabla de años.
export const AniosLista = () => {
  const anios = useAniosEscolares();
  const activo = anios.data ? anioActivo(anios.data) : undefined;
  // /actual responde 404 sin un año ACTIVO: solo se consulta si la lista lo trae.
  const actual = useAnioEscolarActual(!!activo);

  if (anios.isLoading) {
    return (
      <div className="flex h-60 items-center justify-center">
        <Spinner />
      </div>
    );
  }

  if (anios.isError || !anios.data) {
    return (
      <Alert type="error" title="No pudimos cargar los años escolares.">
        Intenta de nuevo en unos minutos.
      </Alert>
    );
  }

  return (
    <>
      {activo && actual.data && <CardAnioEnCurso actual={actual.data} />}
      <AniosTabla anios={anios.data} />
    </>
  );
};
