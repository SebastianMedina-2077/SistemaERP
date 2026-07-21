# Manual del programador — Mamma Tomato (pizzeria-erp)

Mini ERP comercial para **Mamma Tomato** (STARFOOD PERU S.A.C.), una pizzería en Perú.
Este documento explica cómo está construido el sistema por dentro: arquitectura, módulos,
flujos de negocio y convenciones. Es la referencia para cualquier desarrollador que entre
al proyecto. El informe de la última revisión de código está en
[revision-de-codigo.md](revision-de-codigo.md). El diseño propuesto para enviar el recibo al
cliente de la tienda web (correo de marca y PDF por WhatsApp) está en
[recibos-cliente.md](recibos-cliente.md).

---

## 1. Visión general

Es un **monolito Spring Boot** con vistas renderizadas en servidor (Thymeleaf) que sirve
tres pantallas de un mismo local, sincronizadas en tiempo real por SSE:

| Pantalla | Rol | Ruta de entrada | Qué hace |
|---|---|---|---|
| Administración | `ADMINISTRADOR` | `/admin/dashboard` | Catálogo, inventario, compras, personas, reportes, auditoría, caja |
| Punto de venta (POS) | `CAJERO` | `/cajero/pos` | Armar pedidos, cobrar, emitir comprobantes, caja del turno |
| Cocina (KDS) | `COCINA` | `/cocina` | Tablero de órdenes por preparar, cambio de estados |

Reglas de negocio transversales que hay que tener grabadas antes de tocar código:

- **Los precios incluyen IGV.** El total de una venta es la suma directa de las líneas;
  el impuesto se *extrae* del total (`igv = total × 18/118`) y el subtotal es la base
  imponible (`total − igv`). Nunca se suma 18% encima del precio.
- **El total lo calcula el backend.** El POS pide una cotización a
  `POST /api/pedidos/cotizar` y ese es el total autoritativo (incluye descuentos de
  promoción). El cálculo local del navegador es solo un respaldo visual.
- **La numeración de comprobantes es secuencial y sin huecos** por serie (B001 boletas,
  F001 facturas). No depende del autoincrement de la tabla.
- **Todo el dinero se maneja con `BigDecimal`**, escala 2 y redondeo `HALF_UP`.

## 2. Stack

- Java 17 · Spring Boot 4 (webmvc, security, data-jpa, validation, mail, aspectj, actuator)
- Thymeleaf + thymeleaf-layout-dialect + extras de Spring Security
- MySQL 8 (Hibernate con `ddl-auto=validate`: el esquema manda, no las entidades)
- Lombok 1.18.46 (fijado en el `pom.xml` para JDK 24/25)
- ZXing (QR), OpenPDF y Apache POI (exportación de reportes)
- Frontend: JavaScript vanilla (sin framework) + CSS propio; `fetch` contra los endpoints REST

## 3. Puesta en marcha

Requisitos: JDK 17+ y MySQL 8 corriendo.

```bash
mysql -u root -p < bd/MamaTomato_V0.21.sql        # estructura (crea la base y las tablas)
mysql -u root -p erp_mamatomato < bd/data.sql     # datos iniciales: roles, usuarios, catálogo, recetas
./mvnw spring-boot:run                             # levanta en http://localhost:8080
```

Credenciales de BD en `src/main/resources/application.properties`, sobreescribibles con
las variables de entorno `DB_USER` y `DB_PASSWORD`. Usuarios del seed: `admin`, `cajero`
y `cocina` (contraseñas BCrypt en `bd/data.sql`).

Para tener las tres pantallas abiertas en una misma máquina basta con separar cookies:
cada navegador (o ventana de incógnito) guarda su propio `JSESSIONID`.

El envío de boleta electrónica por correo queda inactivo hasta configurar `spring.mail.*`
(instrucciones comentadas en `application.properties`); la venta funciona igual y el
comprobante queda con `email_estado = NO_CONFIGURADO`.

### Tests

```bash
./mvnw test
```

- `LoginAttemptServiceTest` es unitario y corre siempre.
- `PedidoServiceIntegrationTest` es `@SpringBootTest` contra la **BD real con el seed
  cargado** (cada test hace rollback). Si MySQL no está levantado, estos dos tests fallan
  al crear el contexto: es un fallo de entorno, no de código.

## 4. Estructura del código

```
src/main/java/com/erp/pizzeria/
├── PizzeriaErpApplication.java   Arranque
├── audit/        @Audit (anotación) + AuditAspect (AOP de auditoría)
├── config/       SecurityConfig, EmpresaProperties (datos fiscales del emisor)
├── controller/   MVC (vistas Thymeleaf) y REST (JSON para el POS/cocina)
├── dto/          Objetos de transporte: formularios, requests y respuestas JSON
├── event/        PedidoEvent (evento interno de dominio)
├── exception/    Manejadores globales + excepciones propias
├── model/        Entidades JPA (+ enums e ids compuestos)
├── repository/   Spring Data JPA
├── security/     CustomUserDetailsService, LoginAttemptService/Listener
├── service/      Lógica de negocio (transaccional)
└── util/         CodigoUtil (códigos IN/PZ), PageQuery (querystring de paginación)

src/main/resources/
├── application.properties
├── static/js/    Un archivo por pantalla/función (cajero.js, cocina.js, caja.js, …)
├── static/css/   Estilos por módulo
└── templates/    Vistas Thymeleaf: admin/, ventas/, cocina/, auth/, layout/, error/

bd/               MamaTomato_V0.21.sql (estructura), data.sql (datos), migracion_tienda.sql
                  (incremental tienda web), reset_pedidos.sql (utilidad)
```

Convención de capas: el **controller** valida entrada y arma el modelo/respuesta; el
**service** contiene las reglas y las transacciones; el **repository** solo consulta.
Los services son `@Transactional(readOnly = true)` a nivel de clase y cada método de
escritura se marca `@Transactional` (y normalmente `@Audit`).

## 5. Seguridad

`config/SecurityConfig.java` define una sola `SecurityFilterChain`:

- Rutas por rol: `/admin/**` → ADMINISTRADOR, `/cajero/**` → CAJERO, `/cocina/**` → COCINA.
- API: `POST /api/pedidos` es exclusivo del CAJERO (crear ventas); el resto de
  `/api/pedidos/**` lo comparten CAJERO y COCINA (tablero y estados);
  `/api/productos/**` y `/api/stock/**` son de CAJERO y ADMINISTRADOR; `/api/eventos`
  (SSE) lo consumen los tres roles; `/api/account/**` solo pide estar autenticado.
- Para `/api/**`, una sesión expirada responde **401 JSON** en lugar de redirigir al
  login (evita que los `fetch` reciban HTML).
- Login por formulario (`/login`) con redirección por rol en el
  `AuthenticationSuccessHandler` (admin → dashboard, cajero → POS, cocina → tablero).

**Autenticación** (`security/`): `CustomUserDetailsService` carga el usuario desde la
tabla `usuario` (rol → `ROLE_<NOMBRE>` en mayúsculas; `estado = false` deshabilita la
cuenta). `LoginAttemptService` es un freno de fuerza bruta **en memoria**: 5 fallos
seguidos bloquean el username por 15 minutos (`LoginAttemptListener` escucha los eventos
de éxito/fracaso de Spring Security). El login distingue el caso bloqueado
(`/login?locked`) del de credenciales inválidas (`/login?error`).

**CSRF** está activo. Los formularios Thymeleaf lo incluyen solos; los `fetch` lo mandan
por cabecera leyendo los `<meta name="_csrf">` y `<meta name="_csrf_header">` que ponen
las plantillas (ver el helper `sendJson` en cada JS).

Reglas de protección adicionales en código:

- El usuario con `es_admin_supremo = 1` no puede editarse, desactivarse ni eliminarse
  (`PersonaService`).
- Crear un usuario administrador y desbloquear la pantalla exigen re-confirmar la
  contraseña propia contra `POST /api/account/verify-password` (nunca viaja el username:
  se usa el de la sesión).
- Un cajero **no puede cerrar sesión con la caja abierta** (`CajeroSesionController`):
  primero cuadra y cierra el turno.

## 6. Modelo de datos

Esquema oficial en `bd/MamaTomato_V0.21.sql` (Hibernate solo lo **valida**). Vista de conjunto:

```
rol ─< usuario >─ empleado                    proveedor ─< compra >─ usuario
                    │                                        │
cliente ─< pedido >─┘                          detalle_compra >── insumo
             │                                                      │
             ├─< detalle_pedido >── producto ──< producto_insumo >──┤
             │                        │  (receta)                   │
             ├─< pago >── metodo_pago │                     medida ─┘
             │                        ├─< combo_producto (combos)
             └── boleta ── metodo_pago└─< promocion_producto >── promocion
                   │
comprobante_correlativo (contador por serie)   cliente_empresa (RUC ↔ razón social)

movimiento (tipo_movimiento, usuario, compra?) ─< detalle_movimiento >── insumo
auditoria (sin FK: guarda el username como texto)
```

Decisiones importantes:

- **Todas las FK son RESTRICT**: los borrados con dependencias se controlan en la capa
  de servicio con mensajes claros ("tiene ventas registradas, desactívalo…").
- `producto` distingue dos tipos de stock: productos **preparados** (pizzas, `stock`
  NULL, limitados por la receta de insumos) y productos **contables** (bebidas, `stock`
  entero informativo).
- `pago` es el desglose real del cobro: 1 fila = pago simple, 2+ filas = **pago mixto**
  (efectivo + tarjeta/Yape/Plin). `boleta.id_metodopago` guarda el método "principal"
  (la primera parte) por compatibilidad.
- `comprobante_correlativo` lleva la numeración de negocio por serie; el
  `AUTO_INCREMENT` de `boleta` es solo clave técnica.
- Los códigos visibles (`producto.codigo` PZxxxx, `insumo.codigo` INxxxx) los genera el
  backend (`CodigoUtil.siguiente`), son únicos e inmutables; el cliente nunca los define.

## 7. Flujo de venta (el corazón del sistema)

### 7.1 En el navegador (`static/js/cajero.js`)

1. **Catálogo**: `GET /api/productos?categoriaId=` llena la grilla (solo disponibles);
   búsqueda y paginación son locales (30 por página).
2. **Carrito**: cada vez que se agrega/incrementa un producto se consulta
   `GET /api/stock/check?productoId=&cantidad=` (verifica la receta contra el stock de
   insumos) y se muestran los faltantes si no alcanza.
3. **Cotización autoritativa**: ante cualquier cambio del carrito se programa (debounce
   250 ms) un `POST /api/pedidos/cotizar`; la respuesta (subtotal/igv/total y descuento
   por línea) se cachea con una *firma* del carrito (`idProducto x cantidad | …`) para
   saber si sigue vigente. `getTotals()` prefiere esa cotización; si no llegó, cae a la
   suma local. Antes de cobrar, `asegurarCotizacion()` fuerza una fresca: **no se cobra
   sobre un total desactualizado**.
4. **Modal de orden**: mesa/ubicación (BARRA, MESA-1..4 o para llevar), tipo de
   comprobante con sus campos (DNI opcional / RUC + razón social / email), número
   previsto del comprobante (`GET /cajero/comprobante/siguiente`, solo estimación) y
   tiempo estimado de entrega (suma de minutos por tipo de producto + cola actual de
   cocina consultando `GET /api/pedidos/cocina`).
5. **Cobro** según el método: efectivo abre el modal con billetes rápidos y vuelto;
   Yape/Plin muestra un QR de pago (*placeholder*, no hay pasarela real); el **pago
   mixto** arma líneas método+monto y el resto se cobra en efectivo. El desglose se envía
   como `pagos: [{idMetodoPago, monto}, …]`.
6. **Venta**: `POST /api/pedidos` con el `PedidoDTO`; la respuesta es la `BoletaDTO`
   (número formateado, totales, estado del email) y se ofrece imprimir el ticket
   (`/cajero/boleta/{idPedido}`).

Extras del POS: guardar pedidos en espera (sección 10), validación en vivo de
nombre/teléfono/DNI/RUC/email, autocompletado de razón social por RUC
(`/cajero/clientes-empresa/{ruc}`), atajos de teclado (`/` busca, `F9` genera, `Esc`/`Enter`
en modales).

### 7.2 En el backend (`PedidoService.crearPedido`, una sola transacción)

```
1. Cargar usuario cajero y productos del pedido.
2. Acumular el consumo de insumos de TODO el pedido (recetas × cantidades)
   y verificar disponibilidad → StockInsuficienteException (409) con faltantes.
3. Crear Cliente (nombre/teléfono del ticket) y Pedido en estado PENDIENTE.
4. Por línea: calcularLinea(producto, cantidad) → precio × cantidad − descuento
   de promoción, redondeado HALF_UP. ES EL MISMO método que usa cotizar():
   un solo punto de verdad para el precio.
5. total = Σ subtotales de línea; igv = total × 18/118; subtotal = total − igv.
6. resolverPagos(): usa el desglose recibido o un único pago por el total, y
   valida que las partes SUMEN EXACTAMENTE el total (si no, 400).
7. correlativoService.siguiente(serie): UPDATE atómico del contador por serie
   (lock de fila → los cajeros concurrentes se serializan y no hay duplicados).
   Corre con Propagation.MANDATORY: si la venta hace rollback, el número se
   libera → numeración sin huecos.
8. Crear la Boleta (totales, serie+correlativo, mesa, datos del adquiriente
   según el tipo: FACTURA exige RUC de 11 dígitos + razón social y registra la
   empresa para autocompletar; BOLETA_ELECTRONICA exige email; BOLETA acepta
   DNI opcional).
9. Boleta electrónica: intentar el envío por email. NUNCA rompe la venta
   (estados: ENVIADO / PENDIENTE / NO_CONFIGURADO / NO_APLICA).
10. Insertar las filas de Pago.
11. inventarioService.aplicarMovimiento("Venta", "P-0001", …): descuenta el
    stock de insumos y deja el rastro en el kardex.
12. Publicar PedidoEvent("pedido-nuevo"): el EventHub lo reenvía por SSE
    DESPUÉS del commit (TransactionalEventListener AFTER_COMMIT).
```

**Anulación** (`anularPedido`, solo admin, requiere motivo): marca ANULADO, revierte el
stock con un movimiento "Ajuste" (`A-xxxx`) y avisa por SSE para sacar el pedido de la
cola de cocina al instante. El cambio de estado genérico (`actualizarEstado`) rechaza
explícitamente ANULADO: anular solo se puede por su operación propia.

## 8. Comprobantes (boleta, factura, boleta electrónica)

`model/enums/TipoComprobante.java` define serie y código SUNAT por tipo:

| Tipo | Serie | Código SUNAT | Particularidad |
|---|---|---|---|
| BOLETA | B001 | 03 | DNI opcional |
| BOLETA_ELECTRONICA | B001 | 03 | Exige email; se envía por correo. Comparte correlativo con BOLETA |
| FACTURA | F001 | 01 | Exige RUC (11 dígitos) + razón social |

- **Correlativo**: `CorrelativoService` + tabla `comprobante_correlativo` (sección 7.2,
  paso 7). El endpoint `GET /cajero/comprobante/siguiente` solo *estima* el próximo
  número para el modal; el definitivo se reserva al confirmar la venta.
- **Ticket imprimible** (`/cajero/boleta/{idPedido}` → `ventas/boleta.html`):
  `BoletaService.construirImpresion` arma el DTO con datos de la empresa
  (`EmpresaProperties`, prefijo `empresa.*` de `application.properties`), detalle, desglose
  de pagos y el **QR en formato SUNAT**: cadena separada por `|` →
  `RUC | tipoDoc | serie | correlativo | igv | total | fecha | tipoDocAdq | numDocAdq`
  (RUC del cliente = tipo 6, DNI = tipo 1, o el adquiriente genérico configurado).
  `GeneradorQrService` (ZXing) lo devuelve como data-URI PNG incrustado en la vista.
- **Email** (`EmailComprobanteService`): usa `ObjectProvider<JavaMailSender>` para que la
  app funcione sin SMTP configurado; construye un HTML simple con el detalle y responde
  con el estado que queda en `boleta.email_estado`.

## 9. Caja del cajero y cierre de día

**Caja del turno** (`CajaStore` + `CajaCajeroController`, rutas `/cajero/caja/*`):
almacén **en memoria** (`ConcurrentHashMap` por id de cajero — se pierde al reiniciar el
servidor).

- `POST /abrir`: monto inicial (S/ 400 por defecto si no llega o es inválido).
- `GET /reporte`: ventas del turno por método de pago (`PagoRepository.resumenPorMetodo`,
  desde la apertura, excluyendo anulados). `efectivoEsperado = montoInicial + efectivo`.
- `POST /cuadre` con el monto contado:
  - **Falta** (contado < esperado): cuenta como intento fallido; al **3.er intento la
    caja se bloquea** y solo se libera con el PIN de supervisor
    (`caja.supervisor-pin`, `POST /desbloquear`) o desde `/admin/caja`.
  - **Sobra**: pide una segunda confirmación (`confirmar: true`) antes de cerrar.
  - **Cuadra**: cierra el turno directamente.
- El admin ve las cajas bloqueadas y las desbloquea en `/admin/caja`
  (`AdminCajaController`).

**Cierre de día / Z-report** (`CajaController`, `/admin/caja/resumen-dia` y
`POST /admin/caja/cerrar-dia`): consolida ventas del día por método de pago (todas las
cajas, excluyendo anulados). No persiste ni bloquea el día: si algún día se necesita
historial o impedir reapertura, agregar una tabla `cierre_dia`.

## 10. Pedidos guardados (en espera)

`PedidoGuardadoStore`: lista **en memoria por cajero** (vive lo que viva el servidor).
Flujo: el POS guarda el carrito (`POST /cajero/pedidos/guardar`), la vista
`/cajero/guardados` lo lista y al recuperar (`POST …/{id}/recuperar`, que además lo saca
de la lista) lo deja en `sessionStorage["mt-pedido-recuperado"]` y vuelve al POS, donde
`cargarPedidoRecuperado()` lo re-carga al carrito. El badge del POS se refresca con
`GET /cajero/pedidos/guardados/count`.

## 11. Cocina (KDS)

- Vista `/cocina` + `static/js/cocina.js`: pinta las órdenes de
  `GET /api/pedidos/cocina` (estados PENDIENTE y PREPARANDO, orden FIFO por fecha).
- Cambio de estado: `PATCH /api/pedidos/{id}/estado` con
  `PENDIENTE → PREPARANDO → ATENDIDO`. Al marcar ATENDIDO el pedido sale de la cola.
- Sincronización: la pantalla escucha `pedido-nuevo` y `pedido-estado` por SSE y
  re-consulta; el cajero recibe la notificación inversa ("cocina empezó a preparar…").

## 12. Tiempo real (SSE)

- `RealtimeController` expone `GET /api/eventos` (`text/event-stream`).
- `EventHub` guarda los `SseEmitter` (30 min de vida; el `EventSource` del navegador
  reconecta solo) y reenvía cada `PedidoEvent` **después del commit**
  (`@TransactionalEventListener(AFTER_COMMIT)`) para que las pantallas re-consulten y ya
  vean datos persistidos.
- `static/js/realtime.js` abre una única conexión por página y expone
  `window.MammaTomatoRealtime.on(nombre, handler)`.
- Es un registro en memoria de una sola instancia; con varias instancias balanceadas
  habría que mover los emitters a un broker (Redis pub/sub).

## 13. Inventario, compras y kardex

- **Receta** (`producto_insumo`): cuánto insumo consume cada unidad de producto.
  `InventarioService.consumoDeProducto` multiplica por la cantidad vendida y
  `verificarDisponibilidad` acumula el consumo de todo el pedido antes de vender.
- **Movimientos**: todo cambio de stock pasa por `aplicarMovimiento(tipo, documento,
  glosa, usuario, compra, líneas)`, que crea el `movimiento`, ajusta el stock según la
  operación del tipo (Entrada/Salida), recalcula el estado del insumo
  (`bajo` cuando stock ≤ cantidad mínima) y graba el `detalle_movimiento` con el
  **stock resultante** (eso es el kardex).
- Tipos **automáticos** ("Compra", "Venta") no se pueden registrar a mano; los manuales
  (Merma, Ajuste) van por `/admin/movimientos/nuevo`.
- **Compras** (`CompraService.registrarCompra`): crea compra + detalle y genera el
  movimiento de entrada (`C-xxxx`) en la misma transacción.
- Documentos de referencia: `C-xxxx` compra, `P-xxxx` venta, `A-xxxx` reversión por
  anulación.

## 14. Catálogo y promociones

`CatalogService`: CRUD de productos (código PZxxxx generado, inmutable; eliminación
bloqueada si tiene ventas o pertenece a un combo — la alternativa es desactivarlo) y de
promociones.

**Promociones**: se vinculan a productos (`promocion_producto`) y solo aplican si están
activas. `calcularDescuento(producto, cantidad)`:

- `Porcentaje`: `precio × cantidad × valor / 100`
- Monto fijo: `valor × cantidad`
- En ambos casos el descuento **se topa al subtotal de la línea** (nunca deja una línea
  negativa) y se redondea HALF_UP.

El descuento se aplica **solo en el backend** (cotización y venta); el POS únicamente
muestra la fila "Descuento" cuando la cotización lo trae.

## 15. Personas: empleados, usuarios, proveedores

`PersonaService`. Reglas: DNI de empleado y RUC de proveedor únicos; un empleado con
usuario vinculado no se elimina; un usuario con actividad (pedidos/compras/movimientos)
no se elimina (se desactiva); el admin supremo es intocable. Las contraseñas se guardan
con BCrypt y en edición solo se cambian si el campo viene con valor.

## 16. Reportes y dashboard

- **Dashboard** (`/admin/dashboard`): ventas de HOY (excluyendo anulados), pedidos
  pendientes, insumos bajo stock y anulados, más el top de productos.
- **Reportes** (`/admin/reportes`, `ReporteController.buildReportData`): stats globales
  de ventas, top de productos, compras por proveedor, movimientos por tipo y detalle de
  anulados. La misma estructura (`ReporteDataDTO`) alimenta la vista y las exportaciones
  **PDF** (OpenPDF) y **Excel** (POI) de `ReporteExportService`.
- Los agregados se calculan en memoria sobre `findAll()`: suficiente para el volumen de
  un local; si crece, migrar a agregaciones SQL.

## 17. Auditoría

Cualquier método de servicio anotado con `@Audit(accion, entidad)` que termina bien queda
registrado: `AuditAspect` (AOP, `@AfterReturning`) deriva la referencia `#id` del valor
devuelto (o del primer argumento entero en los `void`, típico de eliminar) y
`AuditoriaService.registrar` lo persiste en **transacción propia REQUIRES_NEW** con el
username de la sesión — un fallo de auditoría jamás afecta la operación de negocio, solo
deja un WARN. La vista `/admin/auditoria` muestra los últimos 500. La tabla no tiene FK a
usuario a propósito: conserva el rastro aunque el usuario se elimine.

## 18. Manejo de errores

- **API JSON** (`GlobalExceptionHandler`, `@RestControllerAdvice` sobre
  `@RestController`): `ResourceNotFoundException` → 404, `StockInsuficienteException` →
  409 con la lista de faltantes en `detalle`, `IllegalArgumentException` → 400, errores
  de validación → 400 con `[{campo, mensaje}]`. Formato uniforme:
  `{timestamp, status, error, mensaje, detalle?}`.
- **Vistas MVC** (`MvcExceptionHandler`): mismas excepciones → página `error.html` con
  título y detalle. Además hay `error/403.html` y `error/404.html`.
- En el frontend, `toError()` convierte la respuesta en un `Error` con `payload` para
  poder distinguir (p. ej.) los faltantes de stock del resto.

## 19. Frontend: convenciones

- JavaScript vanilla, un archivo por pantalla. Nada de dependencias de build: lo que hay
  en `static/` es lo que se sirve.
- Toda petición mutadora usa el helper `sendJson(method, url, body)` del propio archivo,
  que agrega el token CSRF desde los `<meta>`.
- Las alertas de UI salen de `MammaTomatoAlert` (`alertas.js`); las tablas del admin usan
  `table-tools.js` (búsqueda/orden) y la paginación server-side arma sus enlaces con
  `PageQuery`.
- El tiempo estimado de entrega es heurístico y vive en `cajero.js`
  (`MINUTOS_POR_TIPO`): combos 40', pizzas 20', panizzas 10', bebidas 3', resto 5' por
  ítem, más la cola pendiente de cocina.

## 20. Convenciones generales del proyecto

- **Código y mensajes en español** (identificadores, comentarios, textos de UI); la
  sintaxis del lenguaje y las APIs del framework quedan en inglés.
- Dinero: `BigDecimal`, escala 2, `HALF_UP`, siempre calculado en backend.
- Los commits siguen `tipo(ámbito): descripción` en español e imperativo.
- El esquema de BD es la fuente de verdad (`ddl-auto=validate`); todo cambio de esquema
  se hace en `bd/MamaTomato_V0.21.sql` (versionando la estructura; sube el número de versión)
  y después se ajustan las entidades.

## 21. Limitaciones conocidas y decisiones asumidas

- **Estado en memoria**: caja del turno, pedidos guardados, bloqueos de login y emitters
  SSE viven en el proceso; un reinicio los pierde. Aceptado para una sola instancia y un
  solo local; los detalles y alternativas están en
  [revision-de-codigo.md](revision-de-codigo.md).
- El QR de pago de billeteras es un placeholder (no hay pasarela integrada).
- No hay tabla de cierre de día: el Z-report se genera al vuelo.
- El comprobante electrónico es una representación (QR con formato SUNAT); no hay
  integración real con OSE/SUNAT.
