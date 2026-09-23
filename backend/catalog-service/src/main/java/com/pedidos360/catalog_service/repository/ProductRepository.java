package com.pedidos360.catalog_service.repository;

import com.pedidos360.catalog_service.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Esta interfaz es el "repositorio" del producto — es quien habla con la base
 * de datos.
 *
 * Al extender JpaRepository<Product, Long>, Spring genera automáticamente
 * métodos
 * como: findAll(), findById(), save(), deleteById(), etc.
 * No necesitamos escribir SQL a mano — Spring lo hace por nosotros.
 *
 * Product = la entidad que maneja.
 * Long = el tipo de dato del ID (clave primaria).
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

}
