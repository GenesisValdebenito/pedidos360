package com.pedidos360.catalog_service.controller;

import com.pedidos360.catalog_service.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ProductControllerTests {

    private ProductService productService;
    private ProductController productController;

    @BeforeEach
    void configurar() {
        productService = mock(ProductService.class);
        productController = new ProductController(productService, "secreto-de-prueba");
    }

    @Test
    void rechazaDescuentoDeStockSinTokenInterno() {
        var respuesta = productController.descontarStock(null, 20L, 2);

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        verifyNoInteractions(productService);
    }

    @Test
    void permiteDescuentoDeStockConTokenInterno() {
        var respuesta = productController.descontarStock("secreto-de-prueba", 20L, 2);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        verify(productService).descontarStock(20L, 2);
    }

    @Test
    void rechazaCantidadCero() {
        var respuesta = productController.descontarStock("secreto-de-prueba", 20L, 0);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        verifyNoInteractions(productService);
    }
}
