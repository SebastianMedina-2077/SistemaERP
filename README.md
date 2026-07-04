# Mamma Tomato — Sistema de ventas e inventario

Mini ERP comercial para **Mamma Tomato** (STARFOOD PERU S.A.C.), una pizzería en Perú.
Nació para ordenar el día a día de un local de comida pequeño: cobrar rápido en caja,
mandar los pedidos a cocina sin papeles y no perder de vista el stock ni la plata del turno.

Es un monolito Spring Boot con vistas server-side (Thymeleaf) y una sola instancia que
sirve las tres pantallas del negocio, sincronizadas en tiempo real.

## Qué hace

- **Punto de venta (Cajero).** Arma el pedido desde el catálogo, aplica promociones,
  cobra en efectivo, tarjeta o billetera (Yape/Plin) con soporte de **pago mixto** y
  vuelto, y abre/cierra caja con cuadre por método de pago.
- **Comprobantes.** Emite **boleta, factura y boleta electrónica** con numeración
  correlativa sin huecos, ticket imprimible con logo y **QR en formato SUNAT**. La
  boleta electrónica se puede enviar por correo al cliente.
- **Cocina (KDS).** Las órdenes llegan a la pantalla de cocina en el momento y cambian
  de estado (pendiente → en preparación → atendido) sin recargar.
- **Inventario y compras.** Control de insumos, kardex de movimientos, descuento
  automático de stock por venta y registro de compras a proveedores.
- **Administración.** Productos, promociones, empleados, usuarios y reportes del negocio
  (ventas del día, más vendidos, etc.), con auditoría de las operaciones sensibles.

Los precios se manejan con **IGV incluido**: el total es lo que paga el cliente y el
impuesto se desglosa a partir de ese total.

## Cómo funciona

Tres roles, una misma aplicación: **Admin**, **Cajero** y **Cocina**. Un pedido nuevo o
un cambio de estado se propaga al instante a las demás pantallas mediante **SSE**
(server-sent events), así que caja y cocina siempre ven lo mismo.

### Las tres pantallas a la vez en una máquina

Todo corre sobre una sola instancia en `http://localhost:8080`. Para tener las tres
sesiones abiertas sin levantar varios puertos, basta con separar las cookies del
navegador (cada uno guarda su propio `JSESSIONID`):

- **Admin:** Chrome
- **Cajero:** Edge (o incógnito de Chrome)
- **Cocina:** Firefox (o una segunda ventana de incógnito)

## Stack

Spring Boot 4 · Java 17 · Thymeleaf · Spring Security · Spring Data JPA (Hibernate) ·
MySQL 8 · ZXing (QR) · OpenPDF y Apache POI (reportes).

## Puesta en marcha

Requisitos: **JDK 17+** y **MySQL 8** en marcha.

1. Crea la base de datos y su esquema:
   ```bash
   mysql -u root -p < bd/schema.sql
   ```
2. Ajusta credenciales si hace falta (por defecto `root` / `12345`,
   base `erp_mamatomato`) en `src/main/resources/application.properties` o vía las
   variables de entorno `DB_USER` y `DB_PASSWORD`.
3. Levanta la app:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Entra en `http://localhost:8080` y accede según tu rol.

> El envío de la boleta electrónica por correo queda desactivado hasta configurar un
> SMTP (`spring.mail.*`); mientras tanto el comprobante se genera igual y el envío se
> marca como pendiente.
