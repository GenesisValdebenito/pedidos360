package com.pedidos360.catalog_service.controller;

import com.pedidos360.catalog_service.model.Product;
import com.pedidos360.catalog_service.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Este es el Controller del catálogo de productos.
 * Recibe las peticiones HTTP del cliente (frontend u otros servicios),
 * llama al Service para ejecutar la lógica, y devuelve la respuesta con su código HTTP.
 *
 * @RestController indica que esta clase maneja peticiones HTTP y devuelve JSON.
 * @RequestMapping define la URL base para todos los endpoints de este controller.
 */
@RestController
@RequestMapping("/api/catalog/products")
public class ProductController {

    // Inyectamos el servicio por constructor (buena práctica en Spring)
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /api/catalog/products
     * Devuelve la lista completa de productos.
     * Accesible para cualquier usuario autenticado.
     * Responde con HTTP 200 (OK) siempre.
     */
    @GetMapping
    public ResponseEntity<List<Product>> listar() {
        return ResponseEntity.ok(productService.listar());
    }

    /**
     * GET /api/catalog/products/{id}
     * Busca un producto por su ID.
     * Si lo encuentra → responde 200 con el producto.
     * Si no lo encuentra → responde 404 (Not Found).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> buscarPorId(@PathVariable Long id) {
        Product producto = productService.buscarPorId(id);

        // Si el servicio retornó null, el producto no existe
        if (producto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(producto);
    }

    /**
     * POST /api/catalog/products
     * Crea un nuevo producto.
     * Solo lo puede hacer un usuario con el rol "Admin" en su JWT.
     * Si el usuario NO es Admin → responde 403 (Forbidden).
     * Si hay errores de validación en el JSON → responde 400 (Bad Request) automáticamente.
     * Si todo está bien → responde 201 (Created) con el producto creado.
     *
     * @AuthenticationPrincipal Jwt jwt → Spring extrae el token JWT del usuario autenticado
     *                                     y lo inyecta aquí para que podamos leer sus claims.
     * @Valid → activa las validaciones (@NotBlank, @Min) definidas en el modelo Product.
     */
    @PostMapping
    public ResponseEntity<Product> agregar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody Product producto) {

        // Leemos la lista de roles del token JWT del usuario
        List<String> roles = jwt.getClaimAsStringList("roles");

        // Si no tiene el rol "Admin", rechazamos la petición con 403
        if (roles == null || !roles.contains("Admin")) {
            return ResponseEntity.status(403).build();
        }

        // Guardamos el producto y respondemos 201 Created
        Product creado = productService.agregar(producto);
        return ResponseEntity.status(201).body(creado);
    }

    /**
     * PUT /api/catalog/products/{id}
     * Actualiza un producto existente.
     * Solo lo puede hacer un usuario con el rol "Admin".
     * Si no es Admin → 403.
     * Si el producto no existe → 404.
     * Si se actualizó correctamente → 200 con el producto actualizado.
     *
     * @Valid activa las validaciones del modelo al recibir el JSON.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> actualizar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody Product productoActualizado) {

        // Verificamos que el usuario sea Admin
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null || !roles.contains("Admin")) {
            return ResponseEntity.status(403).build();
        }

        // Intentamos actualizar el producto
        Product producto = productService.actualizar(id, productoActualizado);

        // Si el servicio retornó null, el producto no existía
        if (producto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(producto);
    }

    /**
     * PUT /api/catalog/products/{id}/descontar-stock?cantidad=N
     * Descuenta N unidades del stock de un producto.
     * NO requiere rol Admin — cualquier usuario autenticado puede llamarlo
     * (lo usará el orders-service cuando se cree un pedido).
     *
     * Si hay suficiente stock → 200 OK.
     * Si no hay suficiente stock → 400 Bad Request (capturamos la excepción del service).
     *
     * @RequestParam Long cantidad → lee el parámetro "cantidad" de la URL (?cantidad=5).
     */
    @PutMapping("/{id}/descontar-stock")
    public ResponseEntity<Void> descontarStock(
            @PathVariable Long id,
            @RequestParam int cantidad) {

        try {
            // Intentamos descontar el stock
            productService.descontarStock(id, cantidad);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            // Si el service lanzó una excepción (stock insuficiente o producto no encontrado),
            // respondemos con 400 Bad Request
            return ResponseEntity.badRequest().build();
        }
    }
}
