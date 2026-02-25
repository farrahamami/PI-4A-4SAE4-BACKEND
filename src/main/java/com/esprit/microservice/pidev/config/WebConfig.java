package com.esprit.microservice.pidev.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Permet d'accéder aux fichiers uploadés via HTTP.
 * Exemple : GET http://localhost:8089/pidev/uploads/resumes/resume_1_xxx.pdf
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Même logique que ApplicationService.getUploadRootPath()
        // → toujours le même dossier physique
        Path p = Paths.get(uploadDir);
        Path uploadPath = p.isAbsolute()
                ? p.normalize()
                : Paths.get(System.getProperty("user.dir"), uploadDir).normalize();

        // Sur Windows: "file:///C:/Users/.../uploads/"
        // Sur Linux:   "file:///home/.../uploads/"
        String location = "file:///" + uploadPath.toString().replace("\\", "/") + "/";

        System.out.println(">>> WebConfig: /uploads/** → " + location);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}