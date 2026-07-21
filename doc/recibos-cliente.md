# Recibos al cliente de la tienda web (diseño para implementación futura)

> **Estado: propuesta.** Nada de lo descrito aquí está implementado todavía. Este documento
> fija el diseño para dos formas de hacerle llegar el recibo al cliente que compra en la
> tienda en línea (`/tienda`): por **correo** (HTML con la marca) y por **WhatsApp** (PDF).
> Sirve como guía para cuando se decida construirlo.

La tienda web ya genera una boleta electrónica real al confirmar el pedido (entra por el flujo
del POS con el usuario sistema `tienda`; ver el manual del programador, sección de comprobantes).
Lo que falta es **entregarle ese comprobante al cliente** de forma automática y con la imagen
de Mamma Tomato. Hoy el cliente ve la confirmación en pantalla, pero no recibe nada a su correo
ni a su celular.

---

## (a) Recibo por correo en HTML con la marca

### Qué existe hoy

El gancho ya está en el backend: `EmailComprobanteService`
(`src/main/java/com/erp/pizzeria/service/EmailComprobanteService.java`). Su comportamiento real:

- Método `enviar(Boleta, List<DetallePedido>, String email)` que **nunca lanza**: ante cualquier
  problema devuelve un estado en vez de romper la venta.
- Estados: `ENVIADO`, `PENDIENTE` (SMTP configurado pero el envío falló) y `NO_CONFIGURADO`.
- Si `spring.mail.*` no está configurado (no hay `JavaMailSender` disponible o falta
  `spring.mail.username`), el servicio **queda inerte**: registra un `WARN`, devuelve
  `NO_CONFIGURADO` y la venta continúa. En ese caso el comprobante se guarda con
  `email_estado = NO_CONFIGURADO`.
- Ya arma un HTML propio en `construirHtml(...)` con nombre comercial, razón social, RUC,
  número de comprobante, detalle de líneas y el desglose IGV/total (recordar: **IGV incluido**,
  se extrae del total, no se suma encima).

O sea: el envío de correo del comprobante **ya funciona** en cuanto se configure el SMTP. Lo que
falta para el caso de la tienda web es afinar plantilla y disparo.

### Qué haría falta

1. **Plantilla HTML de marca.** El HTML actual de `construirHtml` es funcional pero sobrio
   (Arial en línea). Para la tienda conviene una plantilla propia con la identidad de Mamma
   Tomato: logo, paleta (`--tomate`, `--verde`, `--crema` de `tokens.css`), tipografía de marca
   y pie con datos del negocio. Opciones:
   - Extraer el HTML a un template Thymeleaf (`templates/correo/recibo-tienda.html`) renderizado
     con `SpringTemplateEngine`, en vez de concatenar `String`. Es lo más mantenible y permite
     reutilizar estilos.
   - Los estilos deben ir **en línea** (los clientes de correo ignoran `<style>` y hojas
     externas); usar imágenes alojadas por URL absoluta o adjuntas con `cid`.
2. **Destinatario.** Para el pedido web, el email es el de la cuenta del cliente registrado:
   tabla `cliente_cuenta`. El pedido web ya se asocia a esa cuenta, así que el correo sale de
   ahí (no del formulario de la boleta del POS).
3. **Disparo tras crear el pedido web.** Enganchar el envío justo después de que el pedido web
   quede confirmado y con su boleta generada, fuera de la transacción de la venta o de forma
   asíncrona (`@Async`), para que un SMTP lento no penalice la respuesta al cliente. Guardar el
   estado devuelto (`ENVIADO`/`PENDIENTE`/`NO_CONFIGURADO`) en el comprobante para trazabilidad.
4. **Reintentos (opcional).** Un job que reprocese los comprobantes en estado `PENDIENTE`
   cuando el SMTP vuelva a estar disponible.

### Coste y requisitos

- **Coste:** cero en librerías (Spring Mail ya está en el `pom`). Solo hace falta una cuenta SMTP
  (Gmail con contraseña de aplicación, o un proveedor transaccional como Brevo/SendGrid si se
  quiere buena entregabilidad y volumen).
- **Config:** `spring.mail.host/port/username/password` por variables de entorno
  (`MAIL_USER`/`MAIL_PASSWORD`), nunca en el repo.

---

## (b) Recibo en PDF por WhatsApp (compra desde el celular)

Cuando el cliente compra desde el celular, lo natural es recibir el comprobante por WhatsApp.
La idea: generar el recibo en **PDF** y enviarlo como documento por un proveedor de mensajería.

### Generación del PDF

- **OpenPDF ya está en el `pom`** (se usa para los reportes). Sirve para armar el recibo en PDF
  reutilizando los mismos datos de la boleta (emisor, número, líneas, IGV extraído del total,
  QR SUNAT que ya genera `GeneradorQrService`).
- Diseño coherente con la plantilla del correo: logo, colores de marca, desglose y QR.
- El PDF se puede generar bajo demanda y subir a una URL temporal firmada, o adjuntarlo según
  lo que exija el proveedor (ver abajo).

### Envío por WhatsApp

WhatsApp **no permite** enviar mensajes de negocio libremente; hay que pasar por la
**WhatsApp Business Platform (Cloud API)** de Meta, directamente o vía un BSP (proveedor de
soluciones) como **Twilio**, Meta Cloud API, 360dialog, etc. Puntos clave:

- **Plantillas aprobadas.** El primer mensaje que inicia el negocio debe usar una *message
  template* aprobada por Meta (categoría *utility* para un recibo de compra). El PDF va como
  adjunto (media/document) dentro de ese mensaje.
- **Número de WhatsApp Business** verificado y asociado a la cuenta de Meta Business.
- **Consentimiento (opt-in).** El cliente debe haber aceptado recibir mensajes por WhatsApp; hay
  que capturarlo en el registro/checkout y guardarlo.
- **Teléfono del cliente:** ya se pide en el registro (`RegistroTiendaDTO.telefono`, 9 dígitos);
  habría que normalizarlo a formato internacional (`+51`).

### Coste y requisitos

- **Coste:** WhatsApp cobra por conversación/plantilla según país (en Perú, tarifa de plantillas
  *utility*); si se usa un BSP como Twilio, se suma su margen por mensaje. Requiere cuenta de
  Meta Business verificada.
- **Requisitos técnicos:** cliente HTTP hacia la API del proveedor (token/credenciales por
  variables de entorno, nunca en el repo), almacenamiento temporal del PDF accesible por URL o
  subida como media, y manejo de webhooks para estados de entrega (enviado/entregado/leído).
- **Alternativa de menor fricción:** si el coste o el alta en Meta no se justifican para una
  demo, se puede dejar un enlace de descarga del PDF y un botón *"Enviar por WhatsApp"* que abra
  `wa.me` con un mensaje prellenado (lo dispara el cliente, sin API ni coste), aunque eso no
  adjunta el PDF automáticamente.

---

## Resumen

| Canal | Base ya presente | Falta | Coste |
|---|---|---|---|
| Correo HTML de marca | `EmailComprobanteService` (inerte sin `spring.mail.*`) | Plantilla de marca, destinatario desde `cliente_cuenta`, disparo tras el pedido web | SMTP (posible gratis) |
| PDF por WhatsApp | OpenPDF y `GeneradorQrService` en el `pom` | Generar el PDF, integrar WhatsApp Business API/BSP, opt-in y teléfono normalizado | Por mensaje + alta en Meta |

Ambos son **diseño para implementación futura**; ninguno está construido a la fecha de este
documento.
