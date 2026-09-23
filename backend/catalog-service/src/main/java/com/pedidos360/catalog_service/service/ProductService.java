package com.pedidos360.catalog_service.service;

import com.pedidos360.catalog_service.model.Product;
import com.pedidos360.catalog_service.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Esta clase es el "servicio" — contiene toda la lógica de negocio del catálogo.
 * Es el intermediario entre el Controller (quien recibe las peticiones HTTP)
 * y el Repository (quien habla con la base de datos).
 *
 * @Service le dice a Spring que esta clase es un componente de lógica de negocio,
 * y lo registra para poder inyectarlo con @Autowired o por constructor.
 */
@Service
public class ProductService {

    // El repositorio que usamos para acceder a la base de datos.
    // Spring lo inyecta automáticamente gracias al constructor.
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Retorna todos los productos guardados en la base de datos.
     * Equivale a un SELECT * FROM product.
     */
    public List<Product> listar() {
        return productRepository.findAll();
    }

    /**
     * Busca un producto por su ID.
     * Si existe, lo retorna. Si no existe, retorna null.
     * (El .orElse(null) convierte el Optional de JPA a null si no se encuentra.)
     */
    public Product buscarPorId(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    /**
     * Guarda un producto nuevo en la base de datos y lo retorna con su ID asignado.
     * Equivale a un INSERT INTO product.
     */
    public Product agregar(Product producto) {
        return productRepository.save(producto);
    }

    /**
     * Actualiza un producto existente.
     * Primero busca el producto por ID. Si no existe, retorna null.
     * Si existe, modifica sus campos (name, price, stock) y lo guarda.
     */
    public Product actualizar(Long id, Product productoActualizado) {
        // Buscamos el producto que ya existe en la base de datos
        Product productoExistente = productRepository.findById(id).orElse(null);

        // Si no lo encontramos, avisamos al controller retornando null
        if (productoExistente == null) {
            return null;
        }

        // Actualizamos los campos con los nuevos valores recibidos
        productoExistente.setName(productoActualizado.getName());
        productoExistente.setPrice(productoActualizado.getPrice());
        productoExistente.setStock(productoActualizado.getStock());

        // Guardamos los cambios en la base de datos y retornamos el producto actualizado
        return productRepository.save(productoExistente);
    }

    /**
     * Descuenta una cantidad del stock de un producto.
     * Si el stock actual es menor que la cantidad pedida, lanza una excepción
     * con el mensaje "Stock insuficiente" para que el Controller la maneje.
     * Si hay suficiente stock, resta la cantidad y guarda el producto.
     */
    public void descontarStock(Long id, int cantidad) {
        // Buscamos el producto. Si no existe, lanzamos excepción.
        Product producto = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // Verificamos si hay suficiente stock
        if (producto.getStock() < cantidad) {
            throw new RuntimeException("Stock insuficiente");
        }

        // Restamos la cantidad del stock
        producto.setStock(producto.getStock() - cantidad);

        // Guardamos el nuevo stock en la base de datos
        productRepository.save(producto);
    }
}
