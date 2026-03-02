package com.esprit.forumservice.controllers;

import com.esprit.forumservice.entities.Publication;
import com.esprit.forumservice.entities.TypePublication;
import com.esprit.forumservice.services.PublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/publications")
@RequiredArgsConstructor
public class PublicationController {

    private final PublicationService publicationService;

    @GetMapping
    public ResponseEntity<List<Publication>> getAllPublications() {
        return ResponseEntity.ok(publicationService.getAllPublications());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Publication>> getByType(@PathVariable TypePublication type) {
        return ResponseEntity.ok(publicationService.getPublicationsByType(type));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Publication>> getByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(publicationService.getPublicationsByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Publication> getById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(publicationService.getPublicationById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestParam("titre") String titre,
            @RequestParam("contenue") String contenue,
            @RequestParam("type") TypePublication type,
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "pdfs", required = false) List<MultipartFile> pdfs,
            @RequestParam(value = "titleColor", required = false, defaultValue = "#2d1f4e") String titleColor,
            @RequestParam(value = "contentColor", required = false, defaultValue = "#6b5e8e") String contentColor,
            @RequestParam(value = "titleFontSize", required = false, defaultValue = "1.1rem") String titleFontSize) {
        try {
            Publication p = publicationService.createPublication(titre, contenue, type, userId, images, pdfs, titleColor, contentColor, titleFontSize);
            return ResponseEntity.status(HttpStatus.CREATED).body(p);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating publication");
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @RequestParam(value = "titre", required = false) String titre,
            @RequestParam(value = "contenue", required = false) String contenue,
            @RequestParam(value = "type", required = false) TypePublication type,
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "images", required = false) List<MultipartFile> newImages,
            @RequestParam(value = "imagesToKeep", required = false) List<String> imagesToKeep,
            @RequestParam(value = "pdfs", required = false) List<MultipartFile> newPdfs,
            @RequestParam(value = "pdfsToKeep", required = false) List<String> pdfsToKeep,
            @RequestParam(value = "titleColor", required = false) String titleColor,
            @RequestParam(value = "contentColor", required = false) String contentColor,
            @RequestParam(value = "titleFontSize", required = false) String titleFontSize) {
        try {
            Publication p = publicationService.updatePublication(id, titre, contenue, type, userId, newImages, imagesToKeep, newPdfs, pdfsToKeep, titleColor, contentColor, titleFontSize);
            return ResponseEntity.ok(p);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating publication");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id, @RequestParam("userId") Integer userId) {
        try {
            publicationService.deletePublication(id, userId);
            return ResponseEntity.ok("Publication deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> adminDelete(@PathVariable Integer id) {
        try {
            publicationService.adminDeletePublication(id);
            return ResponseEntity.ok("Publication deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
