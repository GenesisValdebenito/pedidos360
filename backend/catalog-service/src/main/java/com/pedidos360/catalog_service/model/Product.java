package com.pedidos360.catalog_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Esta clase representa un Producto en nuestra base de datos.
 *
 * @Entity le dice a Spring (JPA) que esta clase es una tabla en la base de datos.
 * @Data (Lombok) genera automáticamente getters, setters, toString, equals y hashCode.
 * @NoArgsConstructor (Lombok) genera un constructor sin parámetros (requerido por JPA).
 * @AllArgsConstructor (Lombok) genera un constructor con todos los campos.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    /**
     * Identificador único del producto.
     * @Id indica que es la llave primaria de la tabla.
     * @GeneratedValue hace que la base de datos genere el valor automáticamente.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre del producto.
     * @NotBlank valida que el campo no venga vacío ni con solo espacios.
     */
    @NotBlank
    private String name;

    /**
     * Precio del producto en unidades enteras (ej: 1500 pesos).
     * @Min(1) valida que el precio sea al menos 1 (no puede ser gratis ni negativo).
     */
    @Min(value = 1)
    private int price;

    /**
     * Cantidad disponible en bodega.
     * @Min(0) valida que el stock no sea negativo.
     */
    @Min(value = 0)
    private int stock;
}
