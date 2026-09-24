package com.pedidos360.orders_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Clase de configuración general del servicio.
 *
 * @Configuration le dice a Spring que aquí definimos "beans" — objetos
 * que Spring administra y puede inyectar en otros componentes.
 *
 * ¿Por qué registramos RestTemplate como bean?
 * En lugar de hacer "new RestTemplate()" cada vez que lo necesitamos,
 * lo creamos UNA sola vez aquí y Spring lo reutiliza en todos lados.
 * Esto sigue el principio de inyección de dependencias.
 */
@Configuration
public class AppConfig {

    /**
     * Bean de RestTemplate.
     * RestTemplate es la herramienta de Spring para hacer llamadas HTTP
     * desde este microservicio hacia otros (en nuestro caso, hacia catalog-service).
     * Al declararlo aquí con @Bean, podemos inyectarlo en OrderService con @Autowired
     * o por constructor sin necesidad de instanciarlo manualmente.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
