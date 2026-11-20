import { z } from 'zod';

const porcentaje = z
  .string()
  .trim()
  .min(1, 'Ingresa el porcentaje.')
  .refine((valor) => Number(valor) >= 1, 'El porcentaje debe ser mayor a 0.')
  .refine(
    (valor) => Number(valor) <= 100,
    'El porcentaje no puede superar 100 %.',
  );

const entero = z
  .string()
  .trim()
  .min(1, 'Ingresa una cantidad.')
  .refine(
    (valor) => Number.isInteger(Number(valor)),
    'Ingresa un número entero.',
  );

const enteroOpcional = z
  .string()
  .trim()
  .refine(
    (valor) => !valor || Number.isInteger(Number(valor)),
    'Ingresa un número entero.',
  )
  .refine(
    (valor) => !valor || Number(valor) >= 2,
    'Debe terminar en 2 hermanos o más.',
  );

const hermanosDesde = entero.refine(
  (valor) => Number(valor) >= 2,
  'Debe empezar desde 2 hermanos.',
);

const tramoFormulario = z.object({
  // Los campos se validan condicionalmente en superRefine. Si el descuento es
  // de porcentaje único, estas filas están ocultas y no deben bloquear el envío.
  hermanosDesde: z.string(),
  hermanosHasta: z.string(),
  porcentaje: z.string(),
});

export const descuentoSchema = z
  .object({
    nombre: z
      .string()
      .trim()
      .min(1, 'Ingresa la denominación del descuento.')
      .max(80, 'La denominación admite hasta 80 caracteres.'),
    descripcion: z
      .string()
      .max(300, 'La descripción admite hasta 300 caracteres.'),
    tipo: z.enum(['PORCENTAJE_UNICO', 'POR_HERMANOS']),
    aplicacion: z.enum(['MANUAL', 'AUTOMATICO']),
    porcentaje: z.string(),
    tramos: z.array(tramoFormulario),
    conceptos: z.array(z.string()),
    condicion: z.string().max(300, 'La condición admite hasta 300 caracteres.'),
  })
  .superRefine((valores, contexto) => {
    if (!valores.conceptos.length) {
      contexto.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['conceptos'],
        message: 'Asocia al menos un concepto de pago al descuento.',
      });
    }
    if (valores.tipo === 'PORCENTAJE_UNICO') {
      const resultado = porcentaje.safeParse(valores.porcentaje);
      if (!resultado.success) {
        contexto.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['porcentaje'],
          message: resultado.error.issues[0].message,
        });
      }
    }
    if (valores.tipo === 'POR_HERMANOS' && valores.tramos.length < 2) {
      contexto.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['tramos'],
        message: 'El descuento por hermanos necesita al menos dos tramos.',
      });
    }
    if (valores.tipo === 'POR_HERMANOS') {
      valores.tramos.forEach((tramo, indice) => {
        const campos = [
          ['hermanosDesde', hermanosDesde.safeParse(tramo.hermanosDesde)],
          ['hermanosHasta', enteroOpcional.safeParse(tramo.hermanosHasta)],
          ['porcentaje', porcentaje.safeParse(tramo.porcentaje)],
        ] as const;

        campos.forEach(([campo, resultado]) => {
          if (!resultado.success) {
            contexto.addIssue({
              code: z.ZodIssueCode.custom,
              path: ['tramos', indice, campo],
              message: resultado.error.issues[0].message,
            });
          }
        });

        if (
          tramo.hermanosHasta &&
          Number(tramo.hermanosHasta) < Number(tramo.hermanosDesde)
        ) {
          contexto.addIssue({
            code: z.ZodIssueCode.custom,
            path: ['tramos', indice, 'hermanosDesde'],
            message: 'El inicio no puede ser mayor que el final.',
          });
        }
      });

      valores.tramos.forEach((tramo, indice) => {
        if (
          !hermanosDesde.safeParse(tramo.hermanosDesde).success ||
          !enteroOpcional.safeParse(tramo.hermanosHasta).success
        )
          return;

        const desde = Number(tramo.hermanosDesde);
        const hasta = tramo.hermanosHasta
          ? Number(tramo.hermanosHasta)
          : Number.POSITIVE_INFINITY;
        if (
          !Number.isInteger(desde) ||
          (!Number.isInteger(hasta) && hasta !== Number.POSITIVE_INFINITY)
        )
          return;

        valores.tramos.slice(0, indice).forEach((anterior, indiceAnterior) => {
          if (
            !hermanosDesde.safeParse(anterior.hermanosDesde).success ||
            !enteroOpcional.safeParse(anterior.hermanosHasta).success
          )
            return;

          const desdeAnterior = Number(anterior.hermanosDesde);
          const hastaAnterior = anterior.hermanosHasta
            ? Number(anterior.hermanosHasta)
            : Number.POSITIVE_INFINITY;
          if (
            !Number.isInteger(desdeAnterior) ||
            (!Number.isInteger(hastaAnterior) &&
              hastaAnterior !== Number.POSITIVE_INFINITY)
          )
            return;

          const seSuperponen = desde <= hastaAnterior && desdeAnterior <= hasta;
          if (seSuperponen) {
            contexto.addIssue({
              code: z.ZodIssueCode.custom,
              path: ['tramos', indice, 'hermanosDesde'],
              message: `Este tramo se superpone con el tramo ${indiceAnterior + 1}.`,
            });
          }
        });
      });
    }
  });

export type ValoresDescuento = z.infer<typeof descuentoSchema>;

export const configuracionDescuentoSchema = z.object({
  porcentajeMaximo: porcentaje,
});

export type ValoresConfiguracionDescuento = z.infer<
  typeof configuracionDescuentoSchema
>;
