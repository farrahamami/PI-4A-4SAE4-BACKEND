package com.esprit.forumservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Fonctionne sur Windows — user.dir = répertoire de lancement du projet
        String uploadPath = System.getProperty("user.dir").replace("\\", "/");
        String location = "file:///" + uploadPath + "/uploads/";

        System.out.println(">>> Serving uploads from: " + location);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}