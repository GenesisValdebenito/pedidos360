¿QUÉ APRENDERÉ?

En esta experiencia de aprendizaje, los y las estudiantes trabajarán en la configuración de servicios de colas y su despliegue en contenedores, enfocándose en el manejo de mensajes asíncronos en sistemas distribuidos. A través de las actividades formativa y sumativa, integrarán dos servicios de colas y un componente para transmitir mensajes entre ellos.

Este proceso responde al resultado de aprendizaje: Construye soluciones en ambiente Cloud, utilizando colas para el trabajo con mensajes asincrónicos, con el propósito de dar respuesta a problemas en que la carga de trabajo dificulta el uso de método de trabajo sincrónico.

RESULTADO DE APRENDIZAJE

RA2 Desarrolla soluciones utilizando colas para trabajar con mensajes asincrónicos, para dar respuesta a problemas en que la carga de trabajo es muy grande para usar el método de trabajo sincrónico

INDICADORES DE LOGRO

IL 2.1 Configura un componente que utilice tecnología de cola para trabajar con mensajes asíncronos.

IL 2.2 Implementa una solución usando mensajes asincrónicos para dar respuesta a los problemas planteados.

IL 2.3 Administra las colas para dar solución a distintos casos de uso.

RabbitMQ. (s.f.-a). Dead Letter Exchanges. Click Aquí RabbitMQ. (s.f.-b). Monitoring. Click Aquí RabbitMQ. (s.f.-c). RabbitMQ Documentation. Click Aquí RabbitMQ. (s.f.-d). RabbitMQ tutorial - "Hello World!" (Java). Click Aquí RabbitMQ. (s.f.-e). RabbitMQ tutorial - "Hello World!" (Python). Click Aquí RabbitMQ. (s.f.-f). RabbitMQ tutorial - "Work Queues" (Java). Click Aquí

## 01 — PREÁMBULO SEMANA 08

RabbitMQ — Introducción y Mensajería Asíncrona

### 1. Propósito de la semana

Semana 08 inicia la segunda etapa del proyecto semestral. Pedidos360 no se reemplaza: se amplía incorporando mensajería asíncrona con RabbitMQ para desacoplar procesos que no necesitan bloquear la respuesta principal del sistema.

IDEA CLAVE No todo debe transformarse en mensajería. Autenticación, validación de permisos, creación del pedido y validación crítica de stock continúan siendo operaciones síncronas. Esta semana usaremos RabbitMQ principalmente para notificaciones y tareas posteriores al cambio de estado.

### 2. Material institucional Duoc base

2.1.1, Introducción a mensajería asíncrona y RabbitMQ, Sesión 1: conceptos y arquitectura.

2.1.2, Crear cola, productor y consumidor básicos (Hello World), Sesión 2: Docker, cola, Producer, Consumer y prueba end-to-end.

2.1.3, Exchanges, bindings y routing keys aplicados al caso, Sesión 3: DirectExchange y enrutamiento.

Videos institucionales de la unidad:

- https://www.youtube.com/watch?v=MGDP-0ipJNE
- https://www.youtube.com/watch?v=GfQGgqeKgVQ

### 3. Ruta de trabajo

Sesión, Foco, Resultado esperado

Sesión 1, De HTTP síncrono a mensajería asíncrona, Distinguir Producer, Broker, Exchange, Queue, Binding y Consumer; decidir qué procesos de Pedidos360 conviene desacoplar.

Sesión 2, RabbitMQ + Docker + Spring AMQP, Levantar RabbitMQ, usar Management UI, crear una cola y verificar Producer → Queue → Consumer.

Sesión 3, DirectExchange + Routing Keys + Bindings, Enrutar eventos de Pedidos360 a colas diferentes mediante routing keys.

### 4. Cómo se conecta con Pedidos360

Arquitectura objetivo de esta semana

USUARIO ↓ ANGULAR + MSAL ↓ HTTP/JWT API GATEWAY ↓ SPRING BOOT — PEDIDOS ├─ persiste pedido / valida stock ← síncrono └─ publica evento pedido.aceptado ← asíncrono ↓ RabbitMQ ↓ notificaciones.queue ↓ Consumer / ms-notify

### 5. Lo que todavía NO corresponde

- ACK/NACK manual en profundidad.
- DLX / DLQ y reintentos.
- TTL, políticas de retención y alertas.
- Clúster RabbitMQ de dos nodos.
- Kafka.

PROYECCIÓN Los puntos anteriores pertenecen a la continuidad de la unidad. Semana 08 construye la base para que en las semanas siguientes la mensajería sea confiable, tolerante a fallos y desplegable en cloud.

### 6. Continuidad de la unidad

Bloque, Conceptos que siguen, Uso posterior

2.2.x, Publish/Subscribe, ACK/NACK, durabilidad, DLX/DLQ, prefetch, TTL, Confiabilidad y tolerancia a fallos.

2.3.x, Clúster RabbitMQ con Docker Compose, Alta disponibilidad y despliegue cloud.

EP3 / EP4, Productores, consumidores, Queue/Exchange/Binding, DLQ y defensa técnica, Evaluación de la etapa RabbitMQ.

## 03 — SESIÓN 1

Introducción a RabbitMQ y arquitectura de mensajería

### 1. Objetivo

Comprender por qué existe la mensajería asíncrona y reconocer los componentes de RabbitMQ antes de escribir código.

### 2. De síncrono a asíncrono

Modelo, Flujo, Consecuencia

Síncrono, Servicio A → HTTP → Servicio B → respuesta, A espera. Si B falla o tarda, A queda afectado.

Asíncrono, Productor → Broker/cola → consumidor, El productor publica y continúa; el consumidor procesa después.

EJEMPLO PEDIDOS360 Aceptar un pedido y validar stock debe seguir siendo síncrono. En cambio, enviar un correo al cliente o generar un ticket de cocina puede ejecutarse después mediante RabbitMQ.

### 3. Componentes

Componente, Función, Ejemplo Pedidos360

Producer, Crea y publica mensajes., Servicio Pedidos publica pedido.aceptado.

Broker, Intermediario que recibe, enruta y entrega., RabbitMQ.

Exchange, Recibe mensajes del Producer y decide a qué colas enviarlos., pedidos360.direct.exchange.

Binding, Regla que conecta Exchange y Queue., pedido.aceptado → notificaciones.queue.

Queue, Almacena mensajes hasta que sean consumidos., notificaciones.queue.

Consumer, Escucha y procesa mensajes., Servicio de notificaciones.

### 4. Patrones de mensajería

Patrón, Idea, Caso típico

Point-to-Point, Un mensaje termina procesado por un consumidor de la cola., Trabajo distribuido / tareas.

Publish/Subscribe, El mismo evento puede llegar a varias colas/suscriptores., Notificaciones y eventos.

En Semana 08 trabajaremos primero con una cola simple y luego con DirectExchange. Publish/Subscribe, ACK manual y DLQ se profundizan después.

### 5. Tipos de Exchange — mapa conceptual

Exchange, Regla, Uso

Direct, Coincidencia exacta de routing key., Semana 08: pedido.creado, pedido.aceptado, pedido.despachado.

Fanout, Envía a todas las colas enlazadas., Broadcast / pub-sub simple.

Topic, Coincidencia por patrones con * y #., Eventos jerárquicos y filtrado flexible.

Headers, Usa headers del mensaje., Routing por metadatos.

### 6. Actividad de cierre — 8 minutos

- Clasifique como síncrono o asíncrono: login, validar stock, enviar email, generar ticket, cambiar estado, registrar auditoría.
- Dibuje el flujo Producer → Exchange → Queue → Consumer para pedido.aceptado.
- Explique con una frase qué gana Pedidos360 al desacoplar una notificación.

SALIDA ESPERADA El estudiante debe poder explicar la arquitectura sin utilizar todavía código Spring.

### 7. Comprobación rápida

- Si el Consumer está apagado, ¿qué componente conserva el mensaje?
- ¿Qué componente decide a qué Queue llegará un mensaje?
- ¿El Producer necesita conocer el Consumer concreto?
- ¿Qué diferencia conceptual existe entre Queue y Exchange?
- ¿Por qué RabbitMQ y Kafka no cumplen exactamente el mismo rol en Pedidos360?

### SOLUCIÓN DOCENTE — SEMANA 08

### 8. Solución — Actividad de cierre

Operación, Clasificación, Justificación esperada

Login, Síncrona, El usuario necesita saber inmediatamente si fue autenticado antes de continuar.

Validar stock, Síncrona, Es una validación crítica para decidir si la operación principal puede continuar.

Enviar email, Asíncrona, El pedido no necesita esperar a que finalice el envío del correo.

Generar ticket, Asíncrona, Puede ejecutarse después de confirmar la operación principal mediante un consumidor.

Cambiar estado, Síncrona, El sistema debe confirmar inmediatamente que el cambio fue válido y quedó registrado.

Registrar auditoría, Asíncrona, Puede procesarse posteriormente sin bloquear la operación principal; en Pedidos360 se profundizará más adelante con eventos/Kafka.

### Flujo pedido.aceptado

PedidoService / Producer │ │ publica │ routing key = pedido.aceptado ▼ pedidos360.direct.exchange │ │ Binding ▼ notificaciones.queue │ ▼ Consumer de notificaciones │ ▼ Envía / simula email o webpush

Respuesta esperada — Pedidos360 puede aceptar el pedido sin quedar esperando a que el servicio de notificaciones termine. La operación principal queda desacoplada de la velocidad o disponibilidad temporal del servicio secundario.

### 9. Solución — Comprobación rápida

1. Si el Consumer está apagado: la Queue conserva el mensaje hasta que exista un consumidor disponible para procesarlo.

Exchange ↓ Queue ← mensaje esperando ↓ Consumer apagado

2. ¿Quién decide a qué Queue llega el mensaje? El Exchange, usando los Bindings y, según el tipo de exchange, la routing key. En un DirectExchange la coincidencia es exacta.

routing key = pedido.aceptado ↓ pedidos360.direct.exchange ↓ Binding ↓ notificaciones.queue

3. ¿El Producer necesita conocer el Consumer concreto? No. El Producer publica en RabbitMQ; no necesita conocer la clase, proceso o microservicio que terminará consumiendo el mensaje.

Producer → RabbitMQ → Consumer

NO: Producer ─────────► Consumer

4. Queue vs Exchange: el Exchange recibe y enruta; la Queue almacena mensajes hasta que puedan ser consumidos. El Consumer procesa.

IDEA CLAVE Exchange = enruta. Queue = espera/almacena. Consumer = procesa.

5. RabbitMQ vs Kafka en Pedidos360: RabbitMQ se utilizará principalmente para tareas asíncronas desacopladas, como notificaciones o tickets. Kafka se incorporará posteriormente para streaming/eventos orientados a auditoría, analítica y reportería.

RabbitMQ Kafka Mensajería / tareas Eventos / streaming Notificaciones Auditoría Tickets Analítica Trabajo desacoplado Reportería

### 10. Respuesta conceptual esperada

SÍNTESIS DOCENTE En Pedidos360 no todo se convierte en asíncrono. Las operaciones críticas que requieren respuesta inmediata permanecen síncronas. Las tareas posteriores pueden publicarse en RabbitMQ: el Producer publica, el Exchange enruta, la Queue conserva y el Consumer procesa.

## 04 — TALLER CONCEPTUAL

Pedidos360 — ¿Qué dejamos síncrono y qué llevamos a RabbitMQ?

### 1. Escenario

El cliente crea un pedido. El sistema debe validar identidad, persistir el pedido, controlar el stock y luego ejecutar tareas secundarias: avisar al cliente, informar a cocina y preparar el despacho.

### 2. Clasifique las operaciones

Operación, Síncrona / Asíncrona, Justificación del estudiante

Validar JWT y rol, Síncrona, Debe determinarse inmediatamente si el usuario está autorizado antes de ejecutar la operación.

Crear pedido en BD, Síncrona, La operación necesita confirmar que el pedido quedó registrado correctamente.

Validar stock, Síncrona, Es una condición crítica para decidir si el pedido puede continuar.

Descontar stock al aceptar, Síncrona, Forma parte del resultado principal de aceptar el pedido y debe quedar consistente con su estado.

Enviar email/webpush, Asíncrona, No es necesario esperar el envío para confirmar la aceptación del pedido.

Generar ticket de cocina, Asíncrona, Puede generarse después mediante un consumidor de RabbitMQ.

Notificar a despacho, Asíncrona, Es una consecuencia posterior de la operación principal.

Registrar evento para auditoría futura, Asíncrona, La auditoría puede recibir y procesar el evento posteriormente; en el proyecto se profundizará con Kafka.

### 3. Diseño propuesto para Semana 08

Flujo sugerido

PedidoService ↓ confirma operación principal PedidoEventoProducer ↓ routing key = pedido.aceptado pedidos360.direct.exchange ├── notificaciones.queue └── cocina.queue

Consumer de notificaciones → simula email/webpush Consumer de cocina → simula ticket

### 4. Preguntas de discusión

- ¿Qué ocurre si el servicio de notificaciones está detenido durante 30 segundos?
- ¿Debe fallar la aceptación del pedido porque no pudo enviarse el correo?
- ¿Qué datos mínimos debería contener el mensaje?
- ¿Qué diferencia existe entre el estado del pedido en BD y el mensaje publicado?
- ¿Qué problemas aparecerán si un mensaje se procesa dos veces? (solo anticipación; se estudia después).

### 5. Propuesta de payload

Mensaje didáctico

{ "pedidoId": 101, "estado": "ACEPTADO", "clienteEmail": "cliente@ejemplo.cl", "timestamp": "2026-09-28T18:30:00" } REGLA DE ESTA SEMANA RabbitMQ se usará para desacoplar tareas posteriores. No moveremos a la cola la validación de seguridad ni la validación crítica de stock.

## 05 — SESIÓN 2

RabbitMQ + Docker + Management UI

### 1. Meta

Levantar un broker RabbitMQ local, comprobar su salud y utilizar Management UI para observar colas, consumidores y mensajes.

### 2. Requisitos

- Docker Desktop operativo.
- Java 21 para el proyecto complementario.
- Puertos locales 5672 (AMQP) y 15672 (Management UI) disponibles.

### 3. docker-compose.yml

services:

rabbitmq:

image: rabbitmq:4.2-management container_name: dsy1107-rabbitmq ports:

- "5672:5672"
- "15672:15672" environment:

RABBITMQ_DEFAULT_USER: guest RABBITMQ_DEFAULT_PASS: guest volumes:

- rabbitmq_data:/var/lib/rabbitmq healthcheck:

test: ["CMD", "rabbitmq-diagnostics", "ping"] interval: 10s timeout: 5s retries: 5

volumes:

rabbitmq_data:

NOTA La guía institucional utiliza guest/guest para el laboratorio local. Es válido para localhost. Cuando RabbitMQ se despliegue en cloud, las credenciales deberán parametrizarse y no quedar escritas en el repositorio.

### 4. Comandos

docker compose up -d docker compose ps docker logs dsy1107-rabbitmq docker exec dsy1107-rabbitmq rabbitmq-diagnostics ping

### 5. Management UI

Abrir http://localhost:15672 y autenticar con guest / guest.

Sección, Qué observar

Overview, Mensajes publicados, consumidos, memoria y conexiones.

Connections, Conexiones de las aplicaciones Spring Boot.

Channels, Canales AMQP abiertos.

Queues and Streams, Colas, mensajes Ready/Unacked, consumidores y acciones de administración.

Exchanges, Exchanges disponibles y bindings.

### 6. Prueba de laboratorio

- Broker aparece healthy.
- Management UI carga correctamente.
- El estudiante identifica la diferencia entre puerto 5672 y 15672.
- Captura o anotación del estado inicial sin colas del proyecto.

ANTES DE CONTINUAR No avanzar a Spring Boot si docker compose ps no muestra RabbitMQ activo/healthy.

### 7. Solución rápida de problemas

Síntoma, Revisión

Connection refused en 5672, Verifique docker compose ps, puerto 5672 y logs del contenedor.

No abre Management UI, Verifique 15672:15672 y que use http://localhost:15672.

Credenciales rechazadas, En laboratorio local use las mismas credenciales definidas en docker-compose.yml.

Queue no aparece, La aplicación Spring debe declarar la Queue y conectarse al broker.

## 06 — SESIÓN 2

Spring Boot — Producer + Queue + Consumer

### 1. Proyecto mínimo

Adaptación del Hello World institucional a Java 21. Primero se verificará una cola simple antes de incorporar exchanges explícitos.

### 2. Dependencias

<dependency> <groupId>org.springframework.boot</groupId> <artifactId>spring-boot-starter-amqp</artifactId> </dependency> <dependency> <groupId>org.springframework.boot</groupId> <artifactId>spring-boot-starter-web</artifactId> </dependency>

### 3. application.yml

spring:

rabbitmq:

host: localhost port: 5672 username: guest password: guest server:

port: 8080

### 4. Declarar la cola

RabbitMQConfig.java

@Configuration public class RabbitMQConfig { public static final String QUEUE = "pedidos360.hello.queue";

@Bean Queue helloQueue() { return new Queue(QUEUE, true); } }

DIFERENCIA CON EL HELLO WORLD DUOC La guía institucional presenta primero una cola hello no durable. En este material complementario usamos durable=true para acercarnos desde el inicio al proyecto acumulativo Pedidos360.

### 5. Productor

MessageProducer.java

@Service public class MessageProducer { private final RabbitTemplate rabbitTemplate;

public MessageProducer(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate;

}

public void enviar(String mensaje) { rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE, mensaje); } }

### 6. Consumidor

MessageConsumer.java

@Service public class MessageConsumer {

@RabbitListener(queues = RabbitMQConfig.QUEUE) public void recibir(String mensaje) { System.out.println("[CONSUMER] " + mensaje); } }

### 7. Endpoint para probar

MessageController.java

@RestController @RequestMapping("/api/messages") public class MessageController { private final MessageProducer producer;

public MessageController(MessageProducer producer) { this.producer = producer; }

@PostMapping public ResponseEntity<Void> enviar(@RequestBody String mensaje) { producer.enviar(mensaje); return ResponseEntity.accepted().build(); } }

### 8. Prueba

curl -X POST http://localhost:8080/api/messages \

-H "Content-Type: text/plain" \ --data "Pedido 101 aceptado"

Verificación, Resultado esperado

HTTP, 202 Accepted.

Consola Spring, [CONSUMER] Pedido 101 aceptado.

Management UI, Cola visible; si el consumidor está activo, el mensaje puede procesarse inmediatamente.

CONCEPTO El endpoint HTTP responde sin ejecutar directamente la tarea final. La responsabilidad fue delegada al broker y al consumidor.

## 07 — SESIÓN 3

Exchanges + Bindings + Routing Keys

### 1. Problema

Una sola cola ya no basta cuando diferentes componentes necesitan reaccionar de forma distinta a los eventos de Pedidos360.

### 2. Topología

PedidoEventoProducer ↓ pedidos360.direct.exchange ├─ pedido.creado → notificaciones.queue ├─ pedido.aceptado → notificaciones.queue ├─ pedido.aceptado → cocina.queue ├─ pedido.despachado → notificaciones.queue └─ pedido.despachado → despacho.queue

### 3. Conceptos

Concepto, Aplicación

DirectExchange, Coincidencia exacta de routing key.

Queue, Destino persistente de mensajes.

Binding, Une queue con exchange usando una routing key.

Routing key, Etiqueta de enrutamiento del mensaje.

Consumer, Escucha la cola; no necesita conocer la lógica del exchange.

### 4. RabbitMQConfig

@Configuration public class RabbitMQConfig { public static final String EXCHANGE = "pedidos360.direct.exchange"; public static final String NOTIFICACIONES = "notificaciones.queue"; public static final String COCINA = "cocina.queue"; public static final String DESPACHO = "despacho.queue";

@Bean DirectExchange pedidosExchange() { return new DirectExchange(EXCHANGE, true, false); }

@Bean Queue notificacionesQueue() { return new Queue(NOTIFICACIONES, true); } @Bean Queue cocinaQueue() { return new Queue(COCINA, true); } @Bean Queue despachoQueue() { return new Queue(DESPACHO, true); }

@Bean Binding notificaCreado(DirectExchange ex, Queue notificacionesQueue) { return BindingBuilder.bind(notificacionesQueue).to(ex).with("pedido.creado");

}

@Bean Binding notificaAceptado(DirectExchange ex, Queue notificacionesQueue) { return BindingBuilder.bind(notificacionesQueue).to(ex).with("pedido.aceptado"); }

@Bean Binding cocinaAceptado(DirectExchange ex, Queue cocinaQueue) { return BindingBuilder.bind(cocinaQueue).to(ex).with("pedido.aceptado"); }

@Bean Binding notificaDespachado(DirectExchange ex, Queue notificacionesQueue) { return BindingBuilder.bind(notificacionesQueue).to(ex).with("pedido.despachado"); }

@Bean Binding despachoDespachado(DirectExchange ex, Queue despachoQueue) { return BindingBuilder.bind(despachoQueue).to(ex).with("pedido.despachado"); } }

### 5. Publicar usando routing key

rabbitTemplate.convertAndSend( RabbitMQConfig.EXCHANGE, "pedido.aceptado", mensajeJson );

### 6. Qué comprobar en Management UI

- Exchange pedidos360.direct.exchange visible.
- Tres colas visibles.
- Bindings con las routing keys correctas.
- pedido.aceptado llega a notificaciones.queue y cocina.queue.
- pedido.despachado llega a notificaciones.queue y despacho.queue.

ATENCIÓN DirectExchange exige coincidencia exacta. pedido.aceptado y pedido.aceptado.v2 son routing keys distintas.

## 08 — SESIÓN 3

Integración Pedidos360 — pedido.creado / pedido.aceptado / pedido.despachado

### 1. Objetivo

Agregar RabbitMQ al proyecto existente sin reescribir la lógica ya evaluada en EP1/EP2.

### 2. Qué NO cambia

- Angular + MSAL + Entra ID.
- API Gateway y JWT Authorizer.
- Spring Security.
- Persistencia de Pedidos y Catálogo.
- Validación síncrona de stock y reglas de estado.

### 3. Qué se agrega

- Dependencia Spring AMQP.
- Configuración RabbitMQ.
- Producer de eventos de pedido.
- Queues/Bindings.
- Consumers de notificaciones, cocina y despacho.

### 4. Evento simple

public record PedidoEvento( Long pedidoId, String estado, String clienteEmail, LocalDateTime timestamp ) {}

### 5. Producer

@Service public class PedidoEventoProducer { private final RabbitTemplate rabbitTemplate; private final ObjectMapper objectMapper;

public PedidoEventoProducer(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) { this.rabbitTemplate = rabbitTemplate; this.objectMapper = objectMapper; }

public void publicar(String routingKey, PedidoEvento evento) { try { rabbitTemplate.convertAndSend( RabbitMQConfig.EXCHANGE, routingKey, objectMapper.writeValueAsString(evento) ); } catch (JsonProcessingException e) { throw new IllegalStateException("No fue posible serializar el evento", e); } } }

### 6. Integración didáctica en PedidoService

// Después de confirmar la operación principal del pedido:

Pedido guardado = pedidoRepository.save(pedido);

PedidoEvento evento = new PedidoEvento( guardado.getId(), guardado.getEstado().name(), guardado.getClienteEmail(), LocalDateTime.now() );

producer.publicar("pedido.aceptado", evento);

SIMPLIFICACIÓN DIDÁCTICA Esta semana basta con comprobar el flujo. La consistencia entre la transacción de base de datos y la publicación del mensaje se profundizará cuando trabajemos confiabilidad, ACK/NACK, reintentos y DLQ.

### 7. Consumers

@RabbitListener(queues = RabbitMQConfig.NOTIFICACIONES) public void notificar(String eventoJson) { System.out.println("[NOTIFICACIÓN] " + eventoJson); }

@RabbitListener(queues = RabbitMQConfig.COCINA) public void imprimirTicket(String eventoJson) { System.out.println("[COCINA] " + eventoJson); }

### 8. Pruebas mínimas

Prueba, Qué debe ocurrir

pedido.creado, Llega a notificaciones.queue.

pedido.aceptado, Llega a notificaciones.queue y cocina.queue.

pedido.despachado, Llega a notificaciones.queue y despacho.queue.

Consumer detenido, El mensaje permanece en la queue mientras RabbitMQ esté activo y la cola sea durable.

### 9. Evidencia de la sesión

- Captura o demostración del exchange y bindings.
- Mensaje publicado desde Spring Boot.
- Consumer reaccionando.
- Código separado: configuración / producer / consumer / lógica de negocio.

## 10 — CHECKLIST SEMANA 08

RabbitMQ — base lista para continuar la unidad

### 1. Conceptos

☐ Puedo explicar la diferencia entre comunicación síncrona y asíncrona.

☐ Identifico Producer, Broker, Exchange, Binding, Queue y Consumer.

☐ Distingo DirectExchange de Fanout/Topic a nivel conceptual.

☐ Puedo justificar qué tareas de Pedidos360 conviene desacoplar.

### 2. RabbitMQ local

☐ RabbitMQ levanta con Docker Compose.

☐ Management UI funciona en http://localhost:15672.

☐ Comprendo la función de 5672 y 15672.

☐ Puedo ver queues, exchanges, bindings y consumers.

### 3. Spring Boot

☐ Incluí spring-boot-starter-amqp.

☐ Spring Boot se conecta al broker.

☐ Existe una clase de configuración RabbitMQ separada.

☐ Existe un Producer usando RabbitTemplate.

☐ Existe al menos un Consumer con @RabbitListener.

☐ La aplicación publica y consume mensajes end-to-end.

### 4. Routing

☐ Existe un DirectExchange.

☐ Existen queues durables.

☐ Los bindings utilizan routing keys claras.

☐ pedido.aceptado llega a más de una cola cuando corresponde.

☐ El Consumer conoce su queue, no necesita conocer la lógica completa del routing.

### 5. Integración Pedidos360

☐ No rompí autenticación/JWT/API Gateway.

☐ No moví a RabbitMQ la validación crítica de stock.

☐ La lógica de mensajería está separada de la lógica de negocio.

☐ Los eventos llevan datos suficientes para el Consumer.

☐ El proyecto anterior sigue funcionando además de la nueva mensajería.

SEMANA 08 COMPLETA Si todos los puntos anteriores están demostrados, la base de RabbitMQ está lista. La siguiente etapa incorporará confiabilidad: Publish/Subscribe, ACK/NACK, durabilidad, DLX/DLQ, prefetch y reintentos.

### Archivo zip/Repositorio clone

### 11_Solucion_Semana_08_RabbitMQ.zip

# 12 — LABORATORIO COMPLEMENTARIO SEMANA 08

### Pedidos360 — Publish/Subscribe + Manual ACK + Durabilidad

PROPÓSITO: Extender el trabajo de RabbitMQ de la Semana 08 para comprobar, mediante fallos controlados, cómo un evento puede distribuirse a varios servicios y qué ocurre cuando un consumidor no alcanza a confirmar el mensaje.

### 1. Punto de partida

Este laboratorio comienza después de completar los materiales 05, 06, 07 y 08 de la Semana 08. No reemplaza el ejercicio anterior: lo amplía. Se mantiene Spring Boot + Spring AMQP + RabbitMQ y se agregan tres experimentos: Publish/Subscribe con fanout, ACK manual y durabilidad.

### 2. Resultados esperados

- Crear un FanoutExchange durable para eventos de Pedidos360.
- Enlazar tres colas independientes al mismo exchange.
- Comprobar que un solo evento llega a tres consumidores distintos.
- Configurar ACK manual y observar la redelivery cuando un consumidor cae antes de confirmar.
- Distinguir durable exchange, durable queue y persistent message.
- Comprobar qué mensajes continúan disponibles después de reiniciar RabbitMQ.
- Relacionar los experimentos con la confiabilidad de una arquitectura distribuida.

### 3. Arquitectura del laboratorio

POST /api/eventos/pedido-aceptado │ ▼ PedidoEventoProducer │ ▼ pedidos360.fanout.exchange (durable = true) ┌─────────┼──────────┐ ▼ ▼ ▼ notificaciones auditoria metricas .queue .queue .queue │ │ │ ▼ ▼ ▼ Consumer Consumer Consumer Manual ACK Manual ACK Manual ACK

IDEA CLAVE: En un exchange fanout la routing key no decide el destino. Cada cola enlazada recibe una copia del mensaje.

### 4. Requisitos previos

- Docker Desktop funcionando.
- Java 21 e IntelliJ IDEA o VS Code.
- Postman o herramienta equivalente.
- Material de Semana 08 completado hasta la integración de Pedidos360.

### 5. Parte A — Levantar RabbitMQ local

Utilice el mismo RabbitMQ local de la Semana 08. Si necesita recrearlo, puede usar el siguiente docker-compose.yml:

services:

rabbitmq:

image: rabbitmq:3-management container_name: dsy1107-rabbitmq ports:

- "5672:5672"
- "15672:15672" environment:

RABBITMQ_DEFAULT_USER: guest RABBITMQ_DEFAULT_PASS: guest volumes:

- rabbitmq_data:/var/lib/rabbitmq

volumes:

rabbitmq_data:

1. Ejecute: docker compose up -d
2. Abra http://localhost:15672 e ingrese con guest / guest.
3. Mantenga esta ventana abierta para observar Exchanges, Queues y mensajes Ready / Unacked.

SEGURIDAD: Las credenciales guest/guest de este laboratorio son solo para RabbitMQ local en Docker. No se deben reutilizar para un broker expuesto en Internet.

### 6. Parte B — Crear el proyecto complementario

Cree un proyecto Spring Boot con Java 21 y las dependencias Spring Web y Spring for RabbitMQ. Puede partir desde la solución de la Semana 08 o utilizar el proyecto de solución entregado por el docente al finalizar.

6.1 Configuración base de RabbitMQ

spring:

rabbitmq:

host: localhost port: 5672 username: guest password: guest listener:

simple:

acknowledge-mode: manual prefetch: 1

OBSERVE: acknowledge-mode: manual cambia el comportamiento del consumidor: el mensaje queda Unacked hasta que el código ejecuta basicAck(...).

6.2 Configuración del exchange y las tres colas

Complete una clase RabbitMQConfig con estos nombres. Todas las colas y el exchange deben ser durables.

EXCHANGE = "pedidos360.fanout.exchange" NOTIFICACIONES = "pedidos360.notificaciones.queue" AUDITORIA = "pedidos360.auditoria.queue" METRICAS = "pedidos360.metricas.queue"

FanoutExchange → durable = true Queue → durable = true Binding → cada cola enlazada al mismo exchange

NO COPIAR SIN PENSAR: En este laboratorio usted debe completar los tres Queue y los tres Binding. Verifique en Management UI que el exchange muestre exactamente tres bindings.

### 7. Parte C — Evento y Producer

Utilice un evento coherente con Pedidos360. Ejemplo:

{ "tipo": "PEDIDO_ACEPTADO", "pedidoId": 105, "clienteId": 27,

"total": 28990, "fecha": "2026-09-29T10:45:00" }

El producer debe publicar al fanout exchange con routing key vacía. Agregue además una opción para indicar si el mensaje será persistente o no persistente.

rabbitTemplate.convertAndSend( RabbitMQConfig.EXCHANGE, "", json, message -> { message.getMessageProperties().setDeliveryMode( persistent ? MessageDeliveryMode.PERSISTENT : MessageDeliveryMode.NON_PERSISTENT ); return message; } );

PERSISTENTE ≠ DURABLE: durable describe la definición de exchange/cola; persistent describe el mensaje publicado.

### 8. Parte D — Tres consumidores

El docente entrega como referencia el consumidor de notificaciones. Usted debe crear los consumidores de auditoría y métricas siguiendo la misma idea, pero sin la espera de 10 segundos.

@RabbitListener(queues = RabbitMQConfig.NOTIFICACIONES) public void consumir(String body, Message message, Channel channel) throws Exception {

long tag = message.getMessageProperties().getDeliveryTag();

System.out.println("[NOTIFICACIONES] recibido: " + body); System.out.println("Procesando durante 10 segundos...");

Thread.sleep(10000);

channel.basicAck(tag, false); System.out.println("[NOTIFICACIONES] ACK enviado"); }

DESAFÍO: Cree AuditoriaConsumer y MetricasConsumer. Cada uno debe escuchar su propia cola y enviar ACK manual cuando termine.

### 9. Experimento 1 — Publish/Subscribe

1. Inicie RabbitMQ y luego la aplicación Spring Boot.
2. Abra Management UI → Queues and Streams y confirme la existencia de las tres colas.
3. Publique un PEDIDO_ACEPTADO persistente desde Postman.
4. Observe la consola: NOTIFICACIONES, AUDITORIA y METRICAS deben recibir una copia del mismo evento.
5. En Exchanges → pedidos360.fanout.exchange compruebe que existen tres bindings.

POST http://localhost:8080/api/eventos/pedido-aceptado?persistent=true

Content-Type: application/json

{ "pedidoId": 105, "clienteId": 27,

"total": 28990 }

REGISTRE EVIDENCIA: una captura de los tres consumers procesando el mismo pedido y otra captura del fanout exchange con sus tres bindings.

### 10. Experimento 2 — Manual ACK y caída del consumidor

1. Publique un nuevo mensaje.
2. Observe que Notificaciones comienza a procesarlo y espera 10 segundos.
3. Durante esa espera, revise en Management UI la columna Unacked de pedidos360.notificaciones.queue.
4. Antes de que aparezca “ACK enviado”, detenga la aplicación con CTRL+C.
5. Vuelva a iniciar la aplicación.
6. Observe si Notificaciones recibe nuevamente el evento. Compare el indicador redelivered en los logs/propiedades cuando esté disponible.

PREGUNTA CENTRAL: ¿Por qué el mensaje vuelve a entregarse si el consumidor alcanzó a recibirlo?

10.1 Comparación con ACK automático

Cambie temporalmente acknowledge-mode a auto, repita la prueba y compare. Después del experimento vuelva a manual.

spring:

rabbitmq:

listener:

simple:

acknowledge-mode: auto

IMPORTANTE: El objetivo no es concluir que “auto es malo” y “manual es bueno”. El objetivo es observar en qué momento RabbitMQ considera procesado un mensaje y cómo esa decisión cambia el comportamiento frente a fallos.

### 11. Experimento 3 — Durabilidad y mensajes persistentes

11.1 Preparar la aplicación sin consumidores activos

Ejecute temporalmente la aplicación con los listeners detenidos. El Producer y el endpoint HTTP seguirán disponibles, pero las colas acumularán mensajes.

java -jar target/pedidos360-fanout-0.0.1-SNAPSHOT.jar --spring.rabbitmq.listener.simple.auto-startup=false

Desde IntelliJ puede lograr lo mismo agregando como Program arguments:

--spring.rabbitmq.listener.simple.auto-startup=false

11.2 Escenario A — Mensaje persistente

1. Publique 3 mensajes utilizando persistent=true.
2. Compruebe en Management UI que las tres colas contienen mensajes Ready.
3. Reinicie solo RabbitMQ con: docker compose restart rabbitmq
4. Espere que RabbitMQ vuelva a estar disponible.
5. Verifique si los mensajes continúan en las colas.
6. Reinicie Spring Boot normalmente y observe el consumo.

11.3 Escenario B — Mensaje no persistente

1. Vuelva a iniciar Spring Boot con auto-startup=false.
2. Publique nuevos mensajes utilizando persistent=false.
3. Confirme que estén Ready.
4. Reinicie RabbitMQ.
5. Compare el resultado con el escenario A.

ATENCIÓN AL RESULTADO: La persistencia mejora la capacidad de recuperación, pero no debe interpretarse como una garantía absoluta aislada. En este laboratorio estamos observando el comportamiento básico de RabbitMQ con exchange/colas durables y mensajes marcados como persistentes.

### 12. Tabla de observaciones

Prueba, ACK / Persistencia, Resultado observado, Explicación del estudiante

Fanout, 3 colas enlazadas, ,

Caída antes del ACK, Manual ACK, ,

Reinicio después del ACK, ACK enviado, ,

Reinicio del broker, Mensaje persistente, ,

Reinicio del broker, Mensaje no persistente, ,

### 13. Evidencias mínimas

- Fanout exchange con tres bindings.
- Tres colas durables.
- Un mismo evento recibido por tres consumidores.
- Mensaje visible como Unacked mientras Notificaciones espera.
- Reentrega después de detener la aplicación antes del ACK.
- Mensajes Ready antes de reiniciar RabbitMQ.
- Resultado posterior al reinicio para mensajes persistentes y no persistentes.
- Código de RabbitMQConfig, Producer y los tres Consumer.

### 14. Preguntas de análisis

1. ¿Qué diferencia práctica observó entre DirectExchange (material anterior) y FanoutExchange?
2. ¿Por qué cada servicio necesita su propia cola si todos reciben el mismo evento?
3. ¿Qué representa el estado Unacked en RabbitMQ?
4. ¿Qué ocurrió cuando el proceso terminó antes de basicAck(...)?
5. ¿Qué diferencia observó entre ACK automático y ACK manual?
6. ¿Por qué una cola durable no significa automáticamente que todos sus mensajes sobrevivirán a un reinicio?
7. ¿Qué función cumple MessageDeliveryMode.PERSISTENT?
8. ¿Qué costo o trade-off puede tener usar persistencia?
9. ¿En qué parte de Pedidos360 sería razonable usar Publish/Subscribe?
10. ¿Qué problema nuevo aparece si un mensaje falla cada vez que se intenta procesar?

PUENTE A LA SIGUIENTE ETAPA: La última pregunta prepara conceptos como reintentos, NACK y Dead Letter Queue. En este laboratorio no es necesario implementarlos todavía.

### 15. Cierre conceptual

Concepto, Qué debe quedar claro

Publish/Subscribe, Un evento puede interesar a varios servicios independientes.

Fanout, Publica una copia a cada cola enlazada, sin filtrar por routing key.

Manual ACK, El consumidor confirma explícitamente que terminó de procesar.

Unacked, El broker entregó el mensaje, pero todavía no recibió confirmación.

Redelivery, Un mensaje sin confirmar puede volver a entregarse.

Durable Exchange / Queue, La definición del recurso puede sobrevivir al reinicio del broker.

Persistent Message, El productor solicita persistir el mensaje para favorecer su recuperación ante reinicios.

RESULTADO FINAL: Al terminar, el estudiante debe poder explicar no solo cómo publicar y consumir mensajes, sino también qué ocurre cuando el consumidor o el broker fallan en momentos distintos.

### Archivo zip/Repositorio clone

### 12A_Proyecto_Inicial_Laboratorio.zip

### 13_Solucion_Laboratorio_Pedidos360_PublishSubscribe_ACK_Durabilidad.zip

Evaluación Parcial 3_DSY1107_Encargo (12%)

En esta etapa deberás incorporar RabbitMQ al sistema desarrollado previamente, implementando colas, exchanges y DLQs para gestionar tareas asíncronas como notificaciones, generación de tickets o documentos. El desafío es demostrar que la mensajería desacoplada funciona correctamente, sin afectar la lógica existente del sistema.

El encargo corresponde al código de los componentes frontend y backend del sistema, entregado vía GitHub. Debido a entrega atrasada, se estima enviar la evaluación lo más pronto posible antes de las 12:10 pm

Instrucciones específicas de la Evaluación:

- Los componentes desarrollados, frontend y backend se entregan mediante el envío a el/la docente del código fuente.
- El código de todos los componentes del backend debe compilar, seguir buenas prácticas y responder a pruebas básicas.
- El código del frontend debe estar completo, modular, sin errores de compilación y con vistas funcionales.
- La integración del backend con la base de datos cloud debe estar configurada correctamente mediante entidades, repositorios y propiedades de conexión.
- El backend debe incluir filtros que validen el JWT recibido desde el IDaaS para autorizar las peticiones.
- El frontend debe implementar el flujo de login con IDaaS y utilizar el JWT en las llamadas al backend.
- Para ambos contextos, front y back, solo debe subirse a los repositorios lo que corresponda a la tecnología utilizada, configurando los archivos .gitignore necesarios para lograr esto.
- Los microservicios deben incluir en su código productores y consumidores que se conecten a colas RabbitMQ.
- Los microservicios deben manejar mensajes no entregados enviándolos a DLQ y registrándolos en logs.
- El microservicio administrador de RabbitMQ debe permitir la creación y eliminación de colas, exchanges y bindings.

Los aspectos formales son:

- Formato: el backend debe corresponder a varios microservicios construidos en Java con Spring Boot, el frontend debe ser un componente Angular. Ambos tipos de componentes deben ser entregados como enlaces a Github.
- Medio de entrega: Copiar los enlaces de los repositorios de Github a AVA y enviar copia al correo del docente, en el plazo de entrega establecido.

Los materiales, herramientas o insumos que se requieren para realizar esta evaluación:

- Materiales y contenidos académicos proporcionados durante el curso, incluyendo especificaciones técnicas, análisis previos y guías de referencia.

Evaluación Parcial 4_DSY1107_Presentación (18%)

En esta etapa deberás incorporar RabbitMQ al sistema desarrollado previamente, implementando colas, exchanges y DLQs para gestionar tareas asíncronas como notificaciones, generación de tickets o documentos. Tu desafío es demostrar que la mensajería desacoplada funciona correctamente, sin afectar la lógica existente del sistema.

El encargo corresponde al código de los componentes frontend y backend del sistema, entregado vía GitHub. El tiempo asignado es el mismo para el anterior. Se entregan el mismo dia.

Instrucciones específicas de la Evaluación:

- La presentación del proyecto se realiza de manera grupal, respondiendo dudas que el docente realice. Cada estudiante fundamenta la propuesta desarrollada, justificando el diseño, tecnologías utilizadas en el sistema y mostrando su funcionamiento. La presentación debe incluir los siguientes elementos:
- El estudiante debe mostrar en la plataforma cloud la creación de la instancia de API Manager en funcionamiento.
- El estudiante debe mostrar la configuración del API Manager que permite llamar a los endpoints del backend.
- El estudiante debe demostrar que el frontend consume los endpoints a través del API Manager correctamente configurado.
- El estudiante debe mostrar que el API Manager valida JWT, rechazando peticiones inválidas y aceptando las correctas.
- El estudiante debe mostrar la creación del tenant en IDaaS y la existencia de usuarios registrados.
- El estudiante debe demostrar que el frontend utiliza OAuth 2.0/OpenID Connect para iniciar sesión y obtener un JWT válido.
- El estudiante debe mostrar en la nube que el backend y el frontend están desplegados, activos e integrados.
- El estudiante debe mostrar la existencia de dos nodos RabbitMQ configurados y su integración en un clúster.
- El estudiante debe demostrar que existen tres colas de mensajes con sus respectivas DLQ funcionando.
- El estudiante debe mostrar la existencia de exchanges de tipo direct y topic correctamente configurados en RabbitMQ.
- El estudiante debe demostrar en la nube la ejecución de docker-compose levantando servicios de RabbitMQ.

Los aspectos formales son:

- La presentación debe tener entre 5 y 10 minutos de duración.
- La información presentada debe tener un orden e hilo conductor, haciendo uso de un lenguaje técnico.

Los materiales, herramientas o insumos que se requieren para realizar esta evaluación:

- Materiales y contenidos académicos proporcionados durante el curso, incluyendo especificaciones técnicas, análisis previos y guías de referencia.

PAUTA DE EVALUACIÓN DE AMBAS ENTREGAS

Descripción niveles de logro. Muy buen desempeño 100%. Demuestra un desempeño destacado, evidenciando el logro de todos los aspectos evaluados en el indicador.

Indicador de Evaluación (Muy buen desempeño 100%)

Define en application.yml o clase @Configuration los nombres de colas, exchanges y bindings de forma centralizada. Todos los nombres de colas, exchanges y bindings están definidos de manera centralizada en application.yml o en una clase de configuración única. No hay valores sueltos en el código. La organización es clara y consistente para todos los microservicios. 12%

Declara beans de configuración de RabbitMQ con Queue, Exchange y Binding para cada caso de uso. Existen clases de configuración que declaran beans de Queue, Exchange y Binding para cada caso de uso definido en el sistema. Cada ruta de mensajería está claramente identificada y documentada en código. 13%

El código del microservicio no mezcla la lógica de negocio con la configuración de mensajería. Hay clases de configuración separadas. La configuración de mensajería está completamente aislada en clases específicas. Los servicios de negocio solo consumen interfaces o componentes ya configurados. No se observa lógica de configuración en controladores o servicios de dominio. 10%

Los consumidores están implementados con @RabbitListener u otra anotación equivalente y se agrupan por dominio funcional. Todos los consumidores están correctamente implementados con @RabbitListener u otra anotación adecuada. Se organizan de manera clara por dominio funcional o contexto de negocio. La estructura del código facilita el mantenimiento y refleja una separación coherente de responsabilidades. 15%

En los consumidores se maneja la confirmación de mensajes. Se ve uso de ACK y manejo explícito de errores. El manejo de confirmación está completamente implementado. Cada consumidor gestiona ACK de forma explícita. Los errores se manejan correctamente con rutas diferenciadas y decisiones claras entre reintentos, NACK y envío a DLQ. La lógica de error está documentada y es consistente. 20%

El microservicio administrador expone endpoints REST bien definidos por ejemplo POST /queues o DELETE /queues/{name}. El microservicio administrador expone una API REST clara y coherente. Los endpoints tienen rutas, verbos HTTP y nombres de recursos bien definidos. La estructura sigue buenas prácticas de diseño REST y está documentada. 13%

La lógica para crear colas y exchanges está encapsulada en un servicio por ejemplo RabbitAdminService. Toda la lógica de creación, actualización y eliminación de colas y exchanges está encapsulada en un servicio dedicado. Los controladores llaman a métodos de alto nivel y no conocen detalles internos de RabbitMQ. 10%

La API valida los parámetros de entrada para evitar crear colas con nombres vacíos o configuraciones inválidas. Los endpoints del microservicio administrador validan todos los parámetros de entrada. Se usan DTO con anotaciones de validación o lógica equivalente. No es posible crear colas con nombres vacíos ni con configuraciones inválidas. Los mensajes de error son claros. 7%
