package com.pedidos360.orders_service.service;

import com.pedidos360.orders_service.model.Order;
import com.pedidos360.orders_service.repository.OrderRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    public OrderService(OrderRepository orderRepository, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Crea un pedido nuevo.
     * Recibe el pedido con productoId y cantidad, le asigna el cliente (del JWT)
     * y el estado inicial "CREADO", y lo guarda en la base de datos.
     */
    public Order crear(Order pedido, String cliente) {
        pedido.setCliente(cliente);   // El cliente viene del token JWT, no del body
        pedido.setEstado("CREADO");   // Estado inicial siempre es CREADO
        return orderRepository.save(pedido);
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
     * Cambia el estado de un pedido, aplicando reglas de negocio.
     *
     * Recibe el token de autorización del usuario para poder reenviarlo
     * a catalog-service cuando necesite descontar stock.
     *
     * Reglas:
     * - Si el nuevo estado es "DESPACHADO", el pedido DEBE estar en estado "ACEPTADO" primero.
     * - Si el nuevo estado es "ACEPTADO", se descuenta el stock en catalog-service via HTTP.
     */
    public Order cambiarEstado(Long id, String nuevoEstado, String authorizationHeader) {
        // Buscamos el pedido. Si no existe, retornamos null.
        Order pedido = orderRepository.findById(id).orElse(null);
        if (pedido == null) {
            return null;
        }

        // Regla de negocio: para DESPACHAR un pedido, primero debe haber sido ACEPTADO.
        // Si intentan saltar ese paso, lanzamos una excepción que el Controller capturará.
        if ("DESPACHADO".equals(nuevoEstado) && !"ACEPTADO".equals(pedido.getEstado())) {
            throw new RuntimeException("No se puede despachar sin antes aceptar el pedido");
        }

        // Si el nuevo estado es ACEPTADO, debemos descontar el stock en catalog-service.
        // Esto es una llamada HTTP de un microservicio a otro.
        if ("ACEPTADO".equals(nuevoEstado)) {
            descontarStockEnCatalog(pedido.getProductoId(), pedido.getCantidad(), authorizationHeader);
        }

        // Actualizamos el estado y guardamos en base de datos
        pedido.setEstado(nuevoEstado);
        return orderRepository.save(pedido);
    }

    /**
     * ─────────────────────────────────────────────────────────────────
     * LLAMADA HTTP ENTRE MICROSERVICIOS (concepto nuevo)
     * ─────────────────────────────────────────────────────────────────
     *
     * Este método hace una petición HTTP de orders-service hacia catalog-service.
     * Es como si orders-service fuera el "cliente" de catalog-service.
     *
     * ¿Por qué reenviamos el header Authorization?
     * catalog-service también valida el JWT en cada petición. Si no enviamos
     * el token del usuario original, catalog-service rechazará la llamada con 401.
     * Por eso "pasamos" el mismo token que recibimos nosotros del frontend.
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
    private void descontarStockEnCatalog(Long productoId, int cantidad, String authorizationHeader) {
        // Construimos la URL del endpoint de catalog-service
        String url = "http://localhost:8082/api/catalog/products/" + productoId + "/descontar-stock?cantidad=" + cantidad;

        // Preparamos los headers HTTP para la petición saliente
        HttpHeaders headers = new HttpHeaders();
        // Reenviamos el mismo token JWT que recibimos nosotros del frontend
        headers.set("Authorization", authorizationHeader);

        // HttpEntity es el "sobre" que contiene los headers (y opcionalmente un body)
        // Como este PUT no envía body, ponemos null como body
        HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);

        // Hacemos la llamada HTTP a catalog-service
        // Si catalog-service responde con error (ej. 400 por stock insuficiente),
        // RestTemplate lanzará una excepción automáticamente
        restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Void.class);
    }
}
