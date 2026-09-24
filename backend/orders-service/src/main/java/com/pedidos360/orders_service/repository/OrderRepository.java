package com.pedidos360.orders_service.repository;

import com.pedidos360.orders_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de pedidos — es quien habla con la base de datos.
 *
 * JpaRepository<Order, Long> nos da gratis: findAll(), findById(), save(), deleteById(), etc.
 *
 * Además, definimos un método personalizado: findByCliente(String cliente).
 * Spring lee el nombre del método y genera automáticamente la consulta SQL:
 *   SELECT * FROM orders WHERE cliente = ?
 * No necesitamos escribir nada más — solo declarar el método aquí.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Busca todos los pedidos que pertenecen a un cliente específico.
     * Spring traduce "findByCliente" en: WHERE cliente = :cliente
     */
    List<Order> findByCliente(String cliente);
}
