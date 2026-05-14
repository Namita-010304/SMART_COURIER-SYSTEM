package com.smartcourier.admin.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    // CORS is handled by the API Gateway (CorsConfig + globalcors in application.yml).
    // Do NOT add a CorsFilter here — it causes duplicate Access-Control-Allow-Origin
    // headers which browsers reject.
}
