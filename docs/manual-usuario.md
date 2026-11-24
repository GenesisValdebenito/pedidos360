# Manual de usuario — Pedidos360

Manual general para usar Pedidos360 desde el frontend y, cuando se requiera,
desde la API. Está pensado para usuarios finales y para la demostración del
proyecto.

> No incluye credenciales ni tokens reales. Las URLs son ejemplos: usa la URL
> del API Gateway o del entorno que corresponda a tu despliegue.

---

## 1. Qué permite hacer el sistema

Pedidos360 permite:

- Iniciar sesión con una cuenta de Microsoft Entra ID.
- Consultar el catálogo de productos y su stock.
- Crear pedidos.
- Consultar pedidos propios o todos, según el rol.
- Cambiar el estado de un pedido.
- Consultar reportes de pedidos y stock.
- Administrar tópicos de Kafka y revisar el lag de los consumidores.

El frontend incluye las pantallas de **Dashboard**, **Pedidos** y **Catálogo**.
Los reportes y la administración de Kafka se consultan por API, ya que no
tienen una pantalla propia en esta versión.

---

## 2. Requisitos de acceso

- Navegador actualizado (Chrome, Edge o Firefox).
- URL del frontend entregada por el equipo o docente.
- Una cuenta de Microsoft Entra ID registrada en el sistema.
- Un rol asignado: **Admin**, **Operador** o **Cliente**.

Si la cuenta no tiene rol, el inicio de sesión puede funcionar, pero las
acciones protegidas responderán con error **403**.

---

## 3. Roles y permisos

| Acción | Admin | Operador | Cliente |
|---|---|---|---|
| Ver catálogo | Sí | Sí | Sí |
| Crear pedido | Sí* | Sí* | Sí |
| Ver pedidos propios | Sí | Sí | Sí |
| Ver todos los pedidos | Sí | Sí | No |
| Cambiar estado de un pedido | Sí | Sí | No |
| Crear o editar productos | Sí | No | No |
| Ver reportes (KPIs, auditoría y stock) | Sí | Sí | No |
| Administrar Kafka y ver lag | Sí | No | No |

\* El backend permite crear pedidos a cualquier usuario autenticado, pero el
formulario del frontend se muestra al rol **Cliente**. Para la demo se
recomienda usar una cuenta Cliente al crear pedidos.

---

## 4. Iniciar sesión

1. Abre la URL del frontend en el navegador.
2. Haz clic en **Iniciar sesión con Entra ID**.
3. Selecciona o escribe tu cuenta institucional.
4. Acepta el permiso solicitado.
5. Espera la redirección a la pantalla principal.

**Resultado esperado:** el encabezado muestra `Hola, <nombre>` y el botón
**Cerrar sesión**. La navegación muestra **Dashboard**, **Pedidos** y
**Catálogo**.

### Si no puedes iniciar sesión

- Verifica que la cuenta esté asignada a la aplicación en Entra ID.
- Revisa que la URL de redirección coincida con la configurada.
- Si aparece un error de permisos, solicita que te asignen un rol.
- Si el token expiró, cierra sesión y vuelve a entrar.

---

## 5. Navegación general

| Opción | Qué muestra |
|---|---|
| **Dashboard** | Nombre, correo y roles del usuario autenticado |
| **Pedidos** | Lista de pedidos y, según el rol, formulario o cambio de estado |
| **Catálogo** | Productos, precio y stock; creación/edición para Admin |

El botón **Cerrar sesión** termina la sesión y regresa a la pantalla de inicio.

---

## 6. Dashboard

Al entrar en **Dashboard** se muestra:

- Nombre del usuario.
- Nombre de usuario o correo.
- Roles incluidos en el token (`Admin`, `Operador`, `Cliente`).
- Un apartado desplegable con todos los claims del token, útil para depurar.

**Resultado esperado:** los roles coinciden con los asignados en Entra ID.

---

## 7. Pedidos

### 7.1 Consultar pedidos

1. Entra en **Pedidos**.
2. Espera el mensaje **Cargando pedidos...**.
3. Revisa la tabla con ID, cliente, producto, cantidad y estado.

Según el rol:

- **Cliente:** solo ve sus propios pedidos.
- **Admin/Operador:** ve todos los pedidos.

Si ocurre un error, aparece un mensaje con un botón **Reintentar**.

### 7.2 Crear un pedido

Disponible en la pantalla para el rol **Cliente**:

1. En **Pedidos**, ubica el bloque **Crear nuevo pedido**.
2. Escribe el **Producto ID**. Debe existir en el catálogo.
3. Escribe la **Cantidad**. Debe ser mayor o igual a 1.
4. Haz clic en **Crear pedido**.
5. Espera el mensaje **Pedido creado correctamente**.

**Resultado esperado:** el pedido aparece en la tabla con estado `CREADO`.

Si el producto no existe, no tiene stock o el servicio no está disponible,
verás un mensaje de error. Revisa el catálogo y vuelve a intentarlo.

### 7.3 Cambiar el estado de un pedido

Disponible para **Admin** y **Operador**:

1. En la columna **Cambiar estado**, selecciona el nuevo estado.
2. Haz clic en **Actualizar**.
3. Confirma que la tabla refleja el nuevo estado.

La secuencia normal es:

```text
CREADO → ACEPTADO → EN_PREPARACION → DESPACHADO → ENTREGADO
```

Reglas importantes:

- Un pedido solo se puede **cancelar** mientras está en `CREADO`.
- `CANCELADO` y `ENTREGADO` son estados finales.
- Al pasar a `ACEPTADO` se descuenta el stock del producto.
- No se puede saltar el orden de la secuencia.
- Si no hay stock suficiente, el cambio de estado falla y se muestra un aviso.

**Resultado esperado:** el estado cambia y, al aceptar, el stock del catálogo
disminuye.

---

## 8. Catálogo

### 8.1 Consultar productos

1. Entra en **Catálogo**.
2. Revisa la tabla con ID, nombre, precio y stock.
3. Si no hay productos, aparece **No hay productos disponibles**.

### 8.2 Crear un producto (solo Admin)

1. En el bloque **Crear nuevo producto**, completa:
   - **Nombre**
   - **Precio**
   - **Stock**
2. Haz clic en **Crear producto**.
3. Espera el mensaje **Producto creado correctamente**.

### 8.3 Editar un producto (solo Admin)

1. En la fila del producto, haz clic en **Editar**.
2. Cambia nombre, precio o stock.
3. Haz clic en **Guardar cambios**.
4. Para descartar, haz clic en **Cancelar**.

**Resultado esperado:** la tabla muestra los datos actualizados.

> Cada cambio de stock en el catálogo también genera un evento en Kafka para
> los reportes. Esta publicación es automática y no requiere una acción del
> usuario.

---

## 9. Reportes por API

Los reportes se consultan con un token válido de un usuario **Admin** u
**Operador** y el scope `Pedidos.Read`.

| Método | Ruta | Qué devuelve |
|---|---|---|
| GET | `/api/reportes/kpis` | Pedidos creados, unidades, cancelaciones, entregas y promedio |
| GET | `/api/reportes/auditoria?limite=50` | Últimos eventos de pedidos y stock |
| GET | `/api/reportes/stock` | Último stock conocido de todos los productos |
| GET | `/api/reportes/stock/{productoId}` | Último stock de un producto |

### Ejemplo con curl

```bash
export TOKEN="TU_TOKEN"

curl -s "$API_URL/api/reportes/kpis" \
  -H "Authorization: Bearer $TOKEN"

curl -s "$API_URL/api/reportes/auditoria?limite=5" \
  -H "Authorization: Bearer $TOKEN"

curl -s "$API_URL/api/reportes/stock/1" \
  -H "Authorization: Bearer $TOKEN"
```

### Ejemplo con Postman

1. Crea una petición `GET`.
2. Usa la URL del API Gateway más la ruta del reporte.
3. En **Authorization**, selecciona **Bearer Token** y pega el token.
4. Envía la petición.

**Resultado esperado:** HTTP 200 y un JSON con los datos. Si el reporte está
vacío, aún no se han publicado eventos o los consumidores no los han procesado.

---

## 10. Administración de Kafka por API

Solo para el rol **Admin**. Estas rutas no reemplazan a Kafka UI; permiten
administrar el clúster desde el sistema.

| Método | Ruta | Acción |
|---|---|---|
| POST | `/api/kafka/topicos` | Crear un tópico |
| GET | `/api/kafka/topicos` | Listar tópicos |
| GET | `/api/kafka/topicos/{nombre}` | Ver detalle de un tópico |
| PUT | `/api/kafka/topicos/{nombre}/config` | Cambiar retención o compactación |
| DELETE | `/api/kafka/topicos/{nombre}` | Eliminar un tópico |
| GET | `/api/kafka/grupos` | Ver grupos, offsets y lag |

### Ejemplo: consultar grupos y lag

```bash
curl -s "$API_URL/api/kafka/grupos" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** los grupos `reporteria-group` y `auditoria-group` con
su offset actual, offset final y lag. Un lag cercano a 0 indica que los
consumidores están al día.

> No elimines los tópicos `pedidos.eventos` ni `stock.eventos` durante la
> operación normal. Si lo haces, se pierden los eventos y los reportes dejan de
> recibir datos hasta que se vuelvan a crear.

---

## 11. Errores frecuentes

| Código o mensaje | Significado | Qué hacer |
|---|---|---|
| `401 Unauthorized` | No hay token, expiró o el issuer/audience no coincide | Inicia sesión nuevamente o revisa la configuración |
| `403 Forbidden` | El usuario no tiene el rol necesario | Solicita el rol Admin, Operador o Cliente según corresponda |
| `400 Bad Request` | Datos inválidos, por ejemplo cantidad 0 | Corrige el formulario o el JSON |
| `404 Not Found` | El pedido, producto o tópico no existe | Verifica el ID o el nombre |
| `503 Service Unavailable` | Un servicio no está disponible | Espera y reintenta; avisa al administrador |
| `No se pudo crear el pedido` | Producto inexistente, sin stock o servicio caído | Revisa el catálogo y reintenta |
| `No se pudo cambiar el estado` | Transición no permitida o stock insuficiente | Respeta la secuencia y revisa el stock |

---

## 12. Cerrar sesión

1. Haz clic en **Cerrar sesión** en el encabezado.
2. Espera la redirección a la pantalla de inicio.
3. Verifica que ya no aparezca tu nombre.

Si usas un equipo compartido, cierra también la sesión del navegador y no
dejes tokens guardados en herramientas como Postman.

---

## 13. Buenas prácticas de seguridad

- No compartas tu token ni lo pegues en capturas o repositorios.
- No uses cuentas de otros usuarios para la demo.
- Cierra sesión al terminar.
- No intentes acceder a reportes o administración de Kafka sin el rol correcto.
- Reporta cualquier comportamiento extraño al administrador del sistema.

---

## 14. Glosario

| Término | Significado |
|---|---|
| **Pedido** | Solicitud de uno o más productos |
| **Estado** | Etapa del pedido: CREADO, ACEPTADO, etc. |
| **Stock** | Unidades disponibles de un producto |
| **KPI** | Indicador de reporte, por ejemplo pedidos creados o unidades |
| **Auditoría** | Registro cronológico de eventos |
| **Topic** | Canal de eventos de Kafka |
| **Consumer group** | Grupo de consumidores que comparte offsets |
| **Lag** | Mensajes pendientes de procesar por un grupo |
| **Token** | Credencial temporal que identifica al usuario |

---

## 15. Documentación relacionada

- `README.md`: descripción general, ejecución y endpoints.
- `docs/kafka-guia.md`: arquitectura y operación de Kafka.
- `docs/despliegue-ec2.md`: despliegue en AWS EC2.
- `docs/configuracion-variables.md`: variables y secretos.
- `docs/pruebas-seguridad.md`: casos de autenticación y autorización.
