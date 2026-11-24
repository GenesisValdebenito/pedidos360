package com.pedidos360.orders_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "pedidos360.catalog.internal-token=test-token")
@ActiveProfiles("test")
class OrdersServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
