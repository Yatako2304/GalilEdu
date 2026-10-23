// Las pruebas que suben una plantilla (HU-23) mandan un `FormData` con un `File` por axios y MSW. jsdom 24 no
// implementa `arrayBuffer()`, `text()` ni `stream()` en `Blob`/`File`. Node 24 no reconoce el `FormData` de jsdom y lo
// convierte en texto, así que no los necesita; Node 20 sí lo trata como un `FormData`, intenta leer el archivo con esos
// métodos y la petición nunca termina (las pruebas se agotan). Con este parche el multipart se arma bien en las dos
// versiones. Solo define lo que falta, y solo lo importan las pruebas de configuración que suben archivos: no va en
// `setup-tests.ts` porque cambiaría `Blob` para todas las pruebas.

const leer = (blob: Blob) =>
  new Promise<ArrayBuffer>((resolver, rechazar) => {
    const lector = new FileReader();
    lector.onload = () => resolver(lector.result as ArrayBuffer);
    lector.onerror = () => rechazar(lector.error);
    lector.readAsArrayBuffer(blob);
  });

const proto = Blob.prototype as Partial<Blob>;

if (!proto.arrayBuffer) {
  proto.arrayBuffer = function (this: Blob) {
    return leer(this);
  };
}

if (!proto.text) {
  proto.text = async function (this: Blob) {
    return new TextDecoder().decode(await leer(this));
  };
}

if (!proto.stream) {
  proto.stream = function (this: Blob) {
    return new ReadableStream<Uint8Array>({
      start: async (controlador) => {
        controlador.enqueue(new Uint8Array(await leer(this)));
        controlador.close();
      },
    }) as ReturnType<Blob['stream']>;
  };
}
