package com.pedidos360.orders_service.service;

import com.pedidos360.orders_service.model.Order;
import com.pedidos360.orders_service.repository.OrderRepository;
import com.pedidos360.orders_service.messaging.PedidoEvento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de pedidos — contiene toda la lógica de negocio.
 * Es el intermediario entre el Controller (peticiones HTTP) y el Repository (base de datos).
 *
 * También se comunica con catalog-service usando RestTemplate para descontar stock.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    /*
     * RestTemplate es la herramienta de Spring para hacer llamadas HTTP
     * desde un microservicio hacia otro. Lo inyectamos por constructor
     * (el bean lo registramos en AppConfig).
     */
    private final RestTemplate restTemplate;

    private final String tokenInternoCatalogo;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            OrderRepository orderRepository,
            RestTemplate restTemplate,
            @Value("${pedidos360.catalog.internal-token}") String tokenInternoCatalogo,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
        this.tokenInternoCatalogo = tokenInternoCatalogo;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Crea un pedido nuevo.
     * Recibe el pedido con productoId y cantidad, le asigna el cliente (del JWT)
     * y el estado inicial "CREADO", y lo guarda en la base de datos.
     */
    @Transactional
    public Order crear(Order pedido, String cliente) {
        pedido.setCliente(cliente);   // El cliente viene del token JWT, no del body
        pedido.setEstado("CREADO");   // Estado inicial siempre es CREADO
        Order guardado = orderRepository.save(pedido);
        publicarEvento(guardado);
        return guardado;
    }

    /**
     * Lista los pedidos según el perfil del usuario:
     * - Si es Admin u Operador (esAdminOperador = true): retorna TODOS los pedidos de todos los clientes.
     * - Si es un Cliente normal (esAdminOperador = false): retorna solo sus propios pedidos.
     */
    public List<Order> listar(String cliente, boolean esAdminOperador) {
        if (esAdminOperador) {
            // Admin y Operador ven todos los pedidos del sistema
            return orderRepository.findAll();
        } else {
            // El cliente solo ve sus propios pedidos (filtramos por su username)
            return orderRepository.findByCliente(cliente);
        }
    }

    /**
     * Busca un pedido por su ID.
     * Si no existe, retorna null (el Controller responderá 404).
     */
    public Order buscarPorId(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    /**
     * Cambia el estado solo si el paso está permitido.
     * El stock se descuenta únicamente al pasar de CREADO a ACEPTADO.
     */
    @Transactional
    public Order cambiarEstado(Long id, String nuevoEstado) {
        // Bloqueamos el pedido para que dos solicitudes no procesen el mismo cambio a la vez.
        Order pedido = orderRepository.findByIdParaActualizar(id).orElse(null);
        if (pedido == null) {
            return null;
        }

        if (!esTransicionPermitida(pedido.getEstado(), nuevoEstado)) {
            throw new RuntimeException("La transición de estado solicitada no está permitida");
        }

        // Solo se descuenta stock una vez: al aceptar un pedido que estaba CREADO.
        if ("CREADO".equals(pedido.getEstado()) && "ACEPTADO".equals(nuevoEstado)) {
            descontarStockEnCatalog(pedido.getProductoId(), pedido.getCantidad());
        }

        // Actualizamos el estado y guardamos en base de datos
        pedido.setEstado(nuevoEstado);
        Order guardado = orderRepository.save(pedido);
        publicarEvento(guardado);
        return guardado;
    }

    private void publicarEvento(Order pedido) {
        PedidoEvento evento = new PedidoEvento(
                pedido.getId(),
                pedido.getEstado(),
                pedido.getCliente(),
                LocalDateTime.now());
        eventPublisher.publishEvent(evento);
    }

    private boolean esTransicionPermitida(String estadoActual, String nuevoEstado) {
        if (estadoActual == null || nuevoEstado == null) {
            return false;
        }

        return switch (estadoActual) {
            case "CREADO" -> "ACEPTADO".equals(nuevoEstado) || "CANCELADO".equals(nuevoEstado);
            case "ACEPTADO" -> "EN_PREPARACION".equals(nuevoEstado);
            case "EN_PREPARACION" -> "DESPACHADO".equals(nuevoEstado);
            case "DESPACHADO" -> "ENTREGADO".equals(nuevoEstado);
            default -> false;
        };
    }

    /**
     * ─────────────────────────────────────────────────────────────────
     * LLAMADA HTTP ENTRE MICROSERVICIOS (concepto nuevo)
     * ─────────────────────────────────────────────────────────────────
     *
     * Este método hace una petición HTTP de orders-service hacia catalog-service.
     * Es como si orders-service fuera el "cliente" de catalog-service.
     *
     * La llamada usa una credencial interna compartida por ambos servicios.
     * Así, un usuario con un JWT válido no puede descontar stock directamente.
     *
     * ¿Cómo funciona RestTemplate?
     * 1. Construimos los headers HTTP (incluyendo Authorization: Bearer <token>)
     * 2. Creamos un HttpEntity con esos headers (sin body, porque es un PUT sin body)
     * 3. Llamamos a restTemplate.exchange() indicando:
     *    - La URL de catalog-service
     *    - El método HTTP (PUT)
     *    - Los headers
     *    - El tipo de respuesta esperada (Void = no nos importa el body)
     */
    private void descontarStockEnCatalog(Long productoId, int cantidad) {
        // Construimos la URL del endpoint de catalog-service
        String url = "http://localhost:8082/api/catalog/products/" + productoId + "/descontar-stock?cantidad=" + cantidad;

        // El catálogo valida este token antes de modificar el stock.
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Service-Token", tokenInternoCatalogo);

        // Este PUT no envía body; la cantidad ya va en la URL.
        HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);

        // Hacemos la llamada HTTP a catalog-service
        // Si catalog-service responde con error (ej. 400 por stock insuficiente),
        // RestTemplate lanzará una excepción automáticamente
        restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Void.class);
    }
}
