SISTEMA DE VENTAS E INVENTARIO
Este sistema comercial optimizará la gestión de ventas e inventario a las pequeñas empresas de comida, fusionando módulos de ventas, cocina, compras e inventario. 

## Usar las 3 pantallas a la vez (Admin, Cajero, Cocina)

Las tres vistas corren sobre una sola instancia en http://localhost:8080 y se
sincronizan en tiempo real por SSE (un pedido nuevo o un cambio de estado se ve
al instante en las demás pantallas).

Para tener las tres sesiones abiertas en la misma máquina, basta con separar las
cookies del navegador (no hace falta levantar varios puertos):

- Admin:  Chrome
- Cajero: Edge (o una ventana de incógnito de Chrome)
- Cocina: Firefox (o una segunda ventana de incógnito)

Cada navegador guarda su propio JSESSIONID, así que los tres logins conviven sin
pisarse.
