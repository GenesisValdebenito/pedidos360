package com.pedidos360.catalog_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "pedidos360.internal-token=test-token")
@ActiveProfiles("test")
class CatalogServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
