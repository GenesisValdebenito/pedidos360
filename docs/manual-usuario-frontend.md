# Manual de usuario — Frontend Pedidos360 (EP3)

> Guía de uso de la interfaz rediseñada: Dashboard con reportería, Pedidos con
> cancelación para clientes, Catálogo con refresco automático y Perfil.
>
> Este manual es complementario. No reemplaza ni modifica el manual anterior
> (`docs/manual-usuario.md`).

---

## 1. Acceso

1. Abre `http://localhost:4200`.
2. Pulsa **Iniciar sesión con Entra ID**.
3. Autentícate con tu cuenta de Microsoft.
4. Serás redirigido al **Dashboard**.

La sesión se mantiene en el navegador. El botón **Cerrar sesión** está en la
barra lateral (o en el menú superior en pantallas pequeñas).

---

## 2. Navegación

La barra lateral tiene cuatro secciones:

| Sección | Para qué sirve |
|---|---|
| **Dashboard** | Reportería y auditoría (Admin/Operador) o resumen personal (Cliente). |
| **Pedidos** | Crear pedidos, seguirlos, cancelar (Cliente) o cambiar estado (Admin/Operador). |
| **Catálogo** | Ver productos; Admin además crea y edita. |
| **Perfil** | Datos de la cuenta y claims del token. |

El botón de tema (sol/luna) alterna entre **claro** y **oscuro**. La preferencia
se guarda en el navegador.

---

## 3. Roles y permisos

| Acción | Cliente | Operador | Admin |
|---|:---:|:---:|:---:|
| Ver catálogo | Sí | Sí | Sí |
| Crear pedido | Sí | Sí | Sí |
| Ver todos los pedidos | No | Sí | Sí |
| Cambiar estado de pedidos | No | Sí | Sí |
| Cancelar su propio pedido en CREADO | Sí | — | — |
| Ver KPIs, auditoría y stock | No | Sí | Sí |
| Ver consumer groups de Kafka | No | No | Sí |
| Crear o editar productos | No | No | Sí |

Un Cliente que intenta ver reportería recibe **403**. La interfaz no muestra el
enlace de reportería a quien no tiene permiso, y el backend sigue validando el
scope `Pedidos.Read` y el claim `roles`.

---

## 4. Dashboard

### 4.1 Admin y Operador

Muestra, con actualización automática cada **8 segundos**:

- **KPIs:** pedidos creados, unidades solicitadas, entregados, cancelados, tasa
  de cancelación y promedio de unidades por pedido.
- **Pedidos por estado:** barras con la distribución del flujo.
- **Consumer groups** (solo Admin): miembros y lag total de
  `reporteria-group` y `auditoria-group`. Un lag en 0 indica que el grupo
  procesó todos los eventos.
- **Auditoría:** últimos 25 eventos de `pedidos.eventos` con pedido, estado,
  cliente, producto, cantidad y hora de recepción.
- **Stock actual:** último valor por producto desde el tópico compactado
  `stock.eventos`.

El botón **Actualizar** fuerza una consulta inmediata. La etiqueta
`Actualizado HH:mm:ss` indica la última respuesta del backend.

### 4.2 Cliente

Muestra solo sus pedidos:

- Total histórico, pedidos en curso, entregados y cancelados.
- Aviso si tiene pedidos esperando aceptación.
- Tabla con los pedidos recientes y su estado actual.

---

## 5. Pedidos

### 5.1 Crear un pedido (Cliente)

1. Entra a **Pedidos**.
2. En **Nuevo pedido**, elige un producto de la lista.
3. Indica la cantidad. El total estimado se calcula automáticamente.
4. Pulsa **Crear pedido**.

Detalles importantes:

- El botón se deshabilita mientras la petición está en curso. Esto evita crear
  pedidos duplicados por doble clic o por una respuesta lenta.
- Si la cantidad supera el stock disponible, el botón queda bloqueado.
- El pedido nace en estado **CREADO** y el cliente se toma del token, no del
  formulario.

### 5.2 Seguimiento

La tabla se actualiza cada 8 segundos. Si el estado cambia, aparece un aviso
flotante, por ejemplo:

> Tu pedido #12 cambió de CREADO a ACEPTADO.

También se actualiza al volver a la pestaña del navegador. No es necesario
recargar la página.

### 5.3 Cancelar un pedido (Cliente)

1. Busca un pedido en estado **CREADO**.
2. Pulsa **Cancelar** y confirma.
3. El pedido pasa a **CANCELADO**.

Solo se puede cancelar antes de que el pedido sea aceptado. Si ya está
`ACEPTADO` o más avanzado, el backend responde **400** y la interfaz lo informa.

### 5.4 Cambiar estado (Admin/Operador)

La columna **Cambiar estado** ofrece únicamente las transiciones válidas:

```
CREADO → ACEPTADO
ACEPTADO → EN PREPARACIÓN
EN PREPARACIÓN → DESPACHADO
DESPACHADO → ENTREGADO
CREADO → CANCELADO
```

Al aceptar un pedido se descuenta stock en `catalog-service`. Los estados
`ENTREGADO` y `CANCELADO` son finales.

---

## 6. Catálogo

- Todos los usuarios autenticados ven los productos, precios y stock.
- La lista se actualiza cada 8 segundos. Si otro usuario crea o modifica un
  producto, aparece el aviso **“Se detectaron cambios en el catálogo”**.
- El buscador filtra por nombre o ID.
- **Admin** puede crear productos y editar nombre, precio y stock.
- Los productos con stock menor o igual a 5 muestran la etiqueta **Stock bajo**.
- Cada cambio de catálogo se publica como evento en `stock.eventos`.

---

## 7. Perfil

Muestra:

- Nombre, correo y roles.
- Si la cuenta tiene acceso a reportería.
- Tenant y Object ID.
- Todos los claims del ID token en modo lectura.

No muestra el access token ni secretos.

---

## 8. Notificaciones y RabbitMQ

Las notificaciones de RabbitMQ se procesan en `notifications-service`. Para la
demo no es necesario abrir la consola de RabbitMQ: el estado del pedido se ve en
la interfaz y la evidencia del consumo se obtiene en el log:

```bash
journalctl -u pedidos360@notifications-service -n 20 --no-pager
```

Debe aparecer una línea similar a:

```
[NOTIFICACION] Evento recibido: {...}
```

RabbitMQ y Kafka cumplen roles distintos:

- **RabbitMQ:** notificación puntual al cliente cuando cambia un pedido.
- **Kafka:** bitácora de eventos para reportería y auditoría.

---

## 9. Preguntas frecuentes

**¿Los cambios de otros usuarios son instantáneos?**
Se reflejan en la siguiente consulta automática, como máximo 8 segundos después.
Para tiempo real estricto se necesitaría WebSocket o Server-Sent Events, que no
forman parte de esta entrega. El botón **Actualizar** fuerza el refresco.

**¿Por qué no veo la sección de reportería?**
Tu cuenta no tiene rol Admin u Operador. El backend responde 403 y la interfaz
oculta esos datos.

**¿Por qué no puedo cancelar?**
Solo se permite mientras el pedido está en `CREADO`. Si ya fue aceptado, debe
seguir el flujo normal o ser cancelado por Admin/Operador antes de aceptarlo.

**¿Qué hago si el panel queda sin datos?**
Verifica que la EC2 esté encendida, que el proxy apunte a su IP actual y que los
cinco servicios estén activos:

```bash
systemctl --no-pager --lines=0 status 'pedidos360@*'
ss -ltnp | grep -E ':(8080|8081|8082|8083|8084)'
```

**¿Se perdió mi tema claro/oscuro?**
La preferencia se guarda por navegador en `localStorage` con la clave
`pedidos360-tema`.
