package com.pedidos360.orders_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Esta clase representa un Pedido en nuestra base de datos.
 *
 * @Entity le dice a JPA que esta clase es una tabla en la base de datos.
 * @Table(name = "orders") usamos un nombre explícito porque "order" es una
 *   palabra reservada en SQL (causa errores si no la renombramos).
 * @Data, @NoArgsConstructor, @AllArgsConstructor son de Lombok: generan
 *   getters, setters, constructores, etc. sin que tengamos que escribirlos.
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    /**
     * Identificador único del pedido, generado automáticamente por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * El username/email del usuario que creó el pedido.
     * Este campo NO lo envía el cliente en el JSON — lo extraemos
     * directamente del token JWT en el Controller y lo asignamos en el Service.
     */
    private String cliente;

    /**
     * Estado actual del pedido.
     * Valores posibles: CREADO, ACEPTADO, EN_PREPARACION, DESPACHADO, ENTREGADO, CANCELADO.
     * El estado inicial siempre es "CREADO" (se asigna en el Service al crear).
     */
    private String estado;

    /**
     * El ID del producto del catálogo que se está pidiendo.
     * Hace referencia a un Product en catalog-service (no es una FK de JPA
     * porque son servicios separados con bases de datos independientes).
     */
    private Long productoId;

    /**
     * La cantidad de unidades del producto que se quiere pedir.
     * @Min(1) valida que no se pueda pedir 0 o menos unidades.
     */
    @Min(value = 1)
    private int cantidad;
}
