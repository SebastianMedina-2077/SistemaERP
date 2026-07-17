# Revisión de código — julio 2026

Revisión completa del backend (services, controllers, repositorios, entidades, esquema),
del frontend (`static/js`) y de los scripts de BD. El proyecto **compila limpio** y los
tres hallazgos críticos encontrados quedaron **corregidos en esta misma revisión**; el
resto son riesgos y mejoras documentados para decidir con calma.

Convención de severidad: **Alta** = comportamiento incorrecto observable o build roto ·
**Media** = falla bajo condiciones reales pero poco frecuentes · **Baja** = deuda,
robustez o mantenibilidad.

---

## 1. Corregidos en esta revisión

### 1.1 [Alta] Los tests no compilaban y asumían el modelo viejo de IGV

`PedidoServiceIntegrationTest` usaba un constructor de `PedidoDTO` de 5 argumentos que
dejó de existir cuando el DTO creció a 11 campos (comprobantes y pago mixto): `mvnw test`
y `mvnw package` **fallaban en compilación**. Además, sus aserciones seguían el modelo
antiguo de IGV *añadido* (`total = precio + 18%`), cuando el sistema pasó a IGV
**incluido** (`total = precio`, `igv = total × 18/118`): aun arreglando el constructor,
los tests habrían fallado.

**Arreglo**: el test ahora construye el DTO con setters (helper `pedidoDe`) y las
aserciones validan el modelo vigente de IGV incluido.
Archivo: `src/test/java/com/erp/pizzeria/service/PedidoServiceIntegrationTest.java`.

### 1.2 [Alta] Se podía anular un pedido por la puerta de atrás, sin revertir stock

`PATCH /api/pedidos/{id}/estado` aceptaba `estado = ANULADO`. Ese camino se saltaba
`anularPedido`: **no pedía motivo y no revertía el inventario**, y estaba al alcance de
los roles CAJERO y COCINA (la anulación real es solo del admin). El resultado era un
pedido anulado con el stock aún descontado y sin rastro del porqué. El admin MVC ya lo
bloqueaba en su controller, pero la regla no estaba en el servicio.

**Arreglo**: `PedidoService.actualizarEstado` rechaza `ANULADO` con un mensaje que dirige
a la operación de anulación. Ningún llamador legítimo pasaba ese valor (verificado en
`AdminController`, `cocina.js` y `PedidoRestController`).
Archivo: `src/main/java/com/erp/pizzeria/service/PedidoService.java`.

### 1.3 [Alta] El cierre de día sumaba las ventas anuladas

`PedidoRepository.sumarVentasPorRango` no filtraba pedidos ANULADOS, pero el desglose por
método de pago del mismo reporte (`resumenPorMetodoRango`) sí los excluye. El
`totalVentas` del Z-report (`/admin/caja/resumen-dia` y `cerrar-dia`) **no cuadraba con
su propio desglose** en cuanto hubiera una anulación en el día.

**Arreglo**: la consulta ahora excluye anulados (mismo criterio que el desglose) y
`CajaController` pasa el estado a excluir.
Archivos: `PedidoRepository.java`, `CajaController.java`.

> Nota relacionada: `contarPorRango` (el "totalPedidos" del mismo reporte) sigue contando
> los anulados. Se dejó así a propósito —"pedidos del día" incluye los anulados—, pero si
> se prefiere la otra semántica es un cambio de una línea.

---

## 2. Riesgos reales pendientes (decidir antes de crecer)

### 2.1 [Media] El email de la boleta electrónica se envía dentro de la transacción de venta

En `crearPedido`, `emailComprobanteService.enviar(...)` corre **antes del commit** y en
la misma transacción. Dos consecuencias:

1. **Rendimiento/bloqueo**: para entonces la venta ya reservó el correlativo (lock de
   fila en `comprobante_correlativo`). Un SMTP lento mantiene ese lock vivo y **serializa
   a los demás cajeros** durante el envío.
2. **Consistencia**: si algo posterior al envío hace rollback, el cliente ya recibió un
   comprobante de una venta que no existe (con un número que además se reutilizará).

El servicio nunca lanza (la venta no se rompe), así que el riesgo es de latencia y de
correo prematuro, no de caída. **Recomendación**: publicar un evento y enviar el correo
en un `@TransactionalEventListener(AFTER_COMMIT)` (como ya hace el SSE), actualizando
`email_estado` en una transacción corta posterior.

### 2.2 [Media] Carrera de stock entre ventas concurrentes

La verificación (`verificarDisponibilidad`) y el descuento (`aplicarMovimiento`) leen el
stock a memoria y escriben el resultado calculado. Dos ventas simultáneas del mismo
insumo pueden leer ambas "stock 5", descontar 3 cada una y persistir 2 (última escritura
gana): stock final incorrecto e incluso ventas aceptadas sin stock real. Con un solo
cajero no se manifiesta; con dos cajas activas es cuestión de tiempo.

**Recomendación** (cualquiera de las dos):
- UPDATE atómico condicionado: `update insumo set stock = stock - :c where id = :id and
  stock >= :c` y tratar 0 filas como stock insuficiente; o
- bloqueo pesimista (`@Lock(PESSIMISTIC_WRITE)`) al cargar los insumos involucrados,
  ordenados por id para evitar deadlocks.

### 2.3 [Media] El cuadre de caja depende de los nombres de los métodos de pago

`CajaCajeroController.construirReporte` clasifica por descripción literal ("Efectivo",
"Tarjeta", "Yape", "Plin") y el POS detecta efectivo/billetera con regex sobre la
etiqueta visible (`esEfectivo()`, `esBilleteraDigital()` en `cajero.js`). Si alguien
renombra un método en la tabla `metodo_pago` (p. ej. "EFECTIVO SOLES"), sus ventas
desaparecen del cuadre y el POS deja de abrir el modal correcto, sin error visible.

**Recomendación**: columna estable en `metodo_pago` (p. ej. `codigo` o flags
`es_efectivo` / `es_billetera`) y clasificar por ella en backend y frontend.

### 2.4 [Media] Estado operativo en memoria: un reinicio borra la caja abierta

`CajaStore` (cajas del turno), `PedidoGuardadoStore` (pedidos en espera),
`LoginAttemptService` (bloqueos) y los emitters SSE viven en el proceso. El caso que más
duele: reinicio con **caja abierta** → la sesión de caja desaparece y el turno ya no se
puede cuadrar (las ventas persisten, pero el monto inicial y la apertura se pierden).
Está documentado como decisión de diseño; si se vuelve un problema real, persistir al
menos la sesión de caja (tabla pequeña `caja_sesion`) es el primer candidato.

### 2.5 [Baja] Promociones: si un producto tiene varias activas, cuál aplica es azar

`getPromocionActiva` hace `findFirst()` sobre la lista sin orden definido. Con dos
promociones activas sobre el mismo producto, el descuento aplicado depende del orden que
devuelva la BD. **Recomendación**: definir la regla (mayor descuento, o la más reciente)
y ordenarlo en la consulta; o impedir en el alta que un producto quede en dos promos
activas.

### 2.6 [Baja] Referencia ambigua en la auditoría de la venta

`AuditAspect.idDe` toma por reflexión el **primer** getter `getId*` que devuelva un
número, y `getMethods()` no garantiza orden. `crearPedido` devuelve `BoletaDTO`, que
tiene `getIdBoleta` y `getIdPedido`: la referencia auditada (`#n`) puede ser el id de la
boleta o el del pedido según la JVM. **Recomendación**: permitir indicar el campo en la
anotación (`@Audit(ref = "idPedido")`) o estandarizar que el primer getter declarado sea
el id de la entidad auditada.

### 2.7 [Baja] Primera venta concurrente de una serie nueva puede chocar

`CorrelativoService.siguiente`: si la serie no existe, dos ventas simultáneas ven
`incrementar() == 0` e intentan insertar la misma PK; una falla. En la práctica no ocurre
porque el seed pre-crea B001 y F001 — **regla operativa**: toda serie nueva debe
insertarse en `comprobante_correlativo` antes de usarse (dejarlo dicho en la migración).

### 2.8 [Baja] Desbordes de precisión decimal a futuro

- `boleta.subtotal` es `decimal(6,2)` (tope 9 999.99) mientras `total` es
  `decimal(8,2)`: una venta grande rompería primero el subtotal.
- `detalle_compra.subtotal` es `decimal(6,2)`: una línea de compra > S/ 9 999.99 falla.

Para el volumen actual no es problema; si se toca el esquema por otra razón, unificar a
`decimal(8,2)` (con su script de migración).

---

## 3. Seguridad

- **[Media] PIN de supervisor impreso en el HTML del admin**: `AdminCajaController` mete
  `supervisorPin` al modelo y la vista lo usa; queda en texto plano en el DOM de
  `/admin/caja`. Solo lo ve un admin autenticado, pero es un secreto que no debería
  viajar al navegador. Recomendación: validar el PIN server-side también en el flujo del
  admin y sacar el atributo del modelo.
- **[Baja] Valores por defecto inseguros**: contraseña de BD `12345` y PIN `1234` en
  `application.properties`. Correcto para desarrollo; en el equipo del local definirlos
  por variables de entorno (`DB_PASSWORD`, `CAJA_SUPERVISOR_PIN`).
- **[Baja] `spring.jpa.show-sql=true`**: útil en desarrollo, ruido y leve fuga de
  información en producción. Mover a un perfil `dev`.
- **[Info] Bloqueo de login por username y en memoria**: un tercero puede bloquear a
  propósito la cuenta de otro (DoS de cuenta, 15 min). Ya está anotado en el propio
  código como riesgo aceptado para un ERP interno.
- Lo que está **bien resuelto** y conviene no romper: CSRF activo también para los
  `fetch` (token por cabecera desde los `<meta>`), 401 JSON para `/api/**`, roles por
  ruta coherentes con los módulos, re-verificación de contraseña para operaciones
  sensibles sin recibir nunca el username del cliente, admin supremo intocable,
  contraseñas BCrypt, y el correlativo de comprobantes con lock de fila (sin duplicados
  ni huecos).

---

## 4. Calidad y consistencia (no urgente)

- **[Baja] Idioma de los identificadores.** La regla del proyecto es código en español y
  en general se cumple (`crearPedido`, `aplicarMovimiento`, `faltantesDe`…), pero
  sobreviven nombres en inglés: `CatalogService`, `getKitchenOrders`,
  `listPedidos`/`listProductos` (vs `listarCajeros`), `money()`, `StatDTO`,
  `PageQuery.of`. Nada de esto es un error funcional; si se decide homogeneizar,
  hacerlo como refactor dedicado (rename mecánico + build verde), nunca mezclado con
  features.
- **[Baja] Reportes sobre `findAll()`**: `ReporteService`, `getTopProductos`,
  `getBoletasPorPedido` (en el detalle de pedido del admin) cargan tablas completas a
  memoria. Con años de ventas se sentirá; la salida natural son consultas de agregación
  (`sum/group by`) y un mapa por ids como ya hace `getBoletasDe`.
- **[Baja] `PedidoGuardadoStore.recuperar` no es atómico** (busca y luego remueve): dos
  pestañas del mismo cajero podrían recuperar el mismo pedido a la vez. Riesgo mínimo en
  la práctica; `removeIf` + retorno del elemento eliminado lo cerraría.
- **[Info] `cajero.js`**: `validarCliente(true)` pasa un argumento que la función ignora
  (resto de una versión anterior). Inofensivo; limpiar cuando se toque el archivo.
- **[Info] Prefijos de código de producto**: `generarCodigoProducto` siempre genera
  `PZxxxx`, pero el seed trae también `BB0001` (bebidas) y `CB0001` (combos). Los nuevos
  productos de cualquier categoría saldrán con `PZ`; si el prefijo por categoría importa,
  hay que parametrizarlo. Además el formato `%04d` revienta el `char(6)` en el código
  10 000 — límite lejano pero existente.

---

## 5. Cobertura de la revisión y verificación

- **Backend**: 100 % de services, controllers, seguridad, excepciones, aspecto de
  auditoría, repositorios con consultas propias y entidades con lógica (`Boleta`,
  `Insumo`), contra el esquema real de `bd/MamaTomato_V0.21.sql` y el `data.sql`.
- **Frontend**: `cajero.js` completo (el más grande y crítico), `realtime.js`, y revisión
  dirigida (CSRF, endpoints, estados) del resto.
- **Verificación**: `mvnw compile` limpio tras los arreglos; `mvnw test` compila y los
  3 tests unitarios pasan. Los 2 tests de integración requieren MySQL levantado con el
  seed (fallan por conexión en el entorno de esta revisión, no por código) — pendiente de
  ejecutarlos con la BD activa.
