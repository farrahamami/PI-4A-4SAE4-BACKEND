package com.esprit.microservice.pidev.GestionForum.Controllers;

import com.esprit.microservice.pidev.GestionForum.Entities.Publication;
import com.esprit.microservice.pidev.GestionForum.Entities.TypePublication;
import com.esprit.microservice.pidev.GestionForum.Services.PublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/publications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PublicationController {

    private final PublicationService publicationService;

    @GetMapping
    public ResponseEntity<List<Publication>> getAllPublications() {
        try {
            return ResponseEntity.ok(publicationService.getAllPublications());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Publication>> getPublicationsByType(@PathVariable TypePublication type) {
        try {
            return ResponseEntity.ok(publicationService.getPublicationsByType(type));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Publication>> getPublicationsByUserId(@PathVariable Integer userId) {
        try {
            return ResponseEntity.ok(publicationService.getPublicationsByUserId(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Publication> getPublicationById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(publicationService.getPublicationById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ POST — accepts images + PDFs
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPublication(
            @RequestParam("titre") String titre,
            @RequestParam("contenue") String contenue,
            @RequestParam("type") TypePublication type,
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "pdfs", required = false) List<MultipartFile> pdfs) {
        try {
            Publication publication = publicationService.createPublication(titre, contenue, type, userId, images, pdfs);
            return ResponseEntity.status(HttpStatus.CREATED).body(publication);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating publication");
        }
    }

    // ✅ PUT — accepts new images/PDFs + lists of files to keep
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updatePublication(
            @PathVariable Integer id,
            @RequestParam(value = "titre", required = false) String titre,
            @RequestParam(value = "contenue", required = false) String contenue,
            @RequestParam(value = "type", required = false) TypePublication type,
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "images", required = false) List<MultipartFile> newImages,
            @RequestParam(value = "imagesToKeep", required = false) List<String> imagesToKeep,
            @RequestParam(value = "pdfs", required = false) List<MultipartFile> newPdfs,
            @RequestParam(value = "pdfsToKeep", required = false) List<String> pdfsToKeep) {
        try {
            Publication publication = publicationService.updatePublication(
                    id, titre, contenue, type, userId, newImages, imagesToKeep, newPdfs, pdfsToKeep);
            return ResponseEntity.ok(publication);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating publication");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePublication(@PathVariable Integer id,
                                               @RequestParam("userId") Integer userId) {
        try {
            publicationService.deletePublication(id, userId);
            return ResponseEntity.ok("Publication deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting publication");
        }
    }


    // ✅ ADMIN DELETE — no userId check
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> adminDeletePublication(@PathVariable Integer id) {
        try {
            publicationService.adminDeletePublication(id);
            return ResponseEntity.ok("Publication deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting publication");
        }
    }
}