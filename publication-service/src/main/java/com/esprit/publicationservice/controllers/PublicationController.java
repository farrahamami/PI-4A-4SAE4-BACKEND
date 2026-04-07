package com.esprit.publicationservice.controllers;

import com.esprit.publicationservice.dto.UserBlockDTO;
import com.esprit.publicationservice.entities.Publication;
import com.esprit.publicationservice.entities.TypePublication;
import com.esprit.publicationservice.services.PublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/publications")
@RequiredArgsConstructor
public class PublicationController {

    private final PublicationService publicationService;



    @GetMapping
    public ResponseEntity<List<Publication>> getAll() {
        return ResponseEntity.ok(publicationService.getAllPublications());
    }


    @GetMapping("/admin/all")
    public ResponseEntity<List<Publication>> getAllAdmin() {
        return ResponseEntity.ok(publicationService.getAllPublicationsAdmin());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Publication>> getByType(@PathVariable TypePublication type) {
        return ResponseEntity.ok(publicationService.getPublicationsByType(type));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Publication>> getByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(publicationService.getPublicationsByUserId(userId));
    }


    @GetMapping("/user/{userId}/archived")
    public ResponseEntity<List<Publication>> getArchivedByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(publicationService.getArchivedByUserId(userId));
    }


    @GetMapping("/admin/pending")
    public ResponseEntity<List<Publication>> getPending() {
        return ResponseEntity.ok(publicationService.getPendingPublications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Publication> getById(@PathVariable Integer id) {
        try { return ResponseEntity.ok(publicationService.getPublicationById(id)); }
        catch (RuntimeException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); }
    }



    /**
     * Retourne le statut de blocage d'un user.
     * warningCount = somme des warnings (ne décrémente jamais lors d'un déarchivage).
     */
    @GetMapping("/user/{userId}/block-status")
    public ResponseEntity<Map<String, Object>> getBlockStatus(@PathVariable Integer userId) {
        boolean blocked      = publicationService.isUserBlocked(userId);
        long    warningCount = publicationService.getWarningCount(userId);
        return ResponseEntity.ok(Map.of(
                "blocked",      blocked,
                "warningCount", warningCount
        ));
    }



    @GetMapping("/admin/blocked-users")
    public ResponseEntity<List<UserBlockDTO>> getBlockedUsers() {
        return ResponseEntity.ok(publicationService.getAllUsersBlockStatus());
    }



    @PostMapping("/admin/users/{userId}/reactiver-compte")
    public ResponseEntity<?> reactiverCompteUser(@PathVariable Integer userId) {
        try {
            publicationService.reactiverCompteUser(userId);
            return ResponseEntity.ok(Map.of("message", "Compte réactivé avec succès."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }



    @PostMapping("/{id}/signaler")
    public ResponseEntity<?> signaler(@PathVariable Integer id, @RequestParam Integer userId) {
        try {
            return ResponseEntity.ok(publicationService.signalerPublication(id, userId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }



    @PostMapping("/{id}/reactiver")
    public ResponseEntity<?> reactiver(@PathVariable Integer id, @RequestParam Integer userId) {
        try {
            return ResponseEntity.ok(publicationService.demanderReactivation(id, userId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }


    /** POST /api/publications/admin/{id}/accepter → ACTIVE */
    @PostMapping("/admin/{id}/accepter")
    public ResponseEntity<?> accepterReactivation(@PathVariable Integer id) {
        try { return ResponseEntity.ok(publicationService.accepterReactivation(id)); }
        catch (IllegalStateException e) { return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); }
        catch (RuntimeException e)      { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); }
    }

    /** POST /api/publications/admin/{id}/refuser → ARCHIVED */
    @PostMapping("/admin/{id}/refuser")
    public ResponseEntity<?> refuserReactivation(@PathVariable Integer id) {
        try { return ResponseEntity.ok(publicationService.refuserReactivation(id)); }
        catch (IllegalStateException e) { return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); }
        catch (RuntimeException e)      { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); }
    }


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestParam("titre")    String titre,
            @RequestParam("contenue") String contenue,
            @RequestParam("type")     TypePublication type,
            @RequestParam("userId")   Integer userId,
            @RequestParam(value = "images",        required = false) List<MultipartFile> images,
            @RequestParam(value = "pdfs",          required = false) List<MultipartFile> pdfs,
            @RequestParam(value = "titleColor",    required = false, defaultValue = "#2d1f4e") String titleColor,
            @RequestParam(value = "contentColor",  required = false, defaultValue = "#6b5e8e") String contentColor,
            @RequestParam(value = "titleFontSize", required = false, defaultValue = "1.1rem")  String titleFontSize) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(publicationService.createPublication(titre, contenue, type, userId, images, pdfs, titleColor, contentColor, titleFontSize));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @RequestParam(value = "titre",         required = false) String titre,
            @RequestParam(value = "contenue",      required = false) String contenue,
            @RequestParam(value = "type",          required = false) TypePublication type,
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "images",        required = false) List<MultipartFile> newImages,
            @RequestParam(value = "imagesToKeep",  required = false) List<String> imagesToKeep,
            @RequestParam(value = "pdfs",          required = false) List<MultipartFile> newPdfs,
            @RequestParam(value = "pdfsToKeep",    required = false) List<String> pdfsToKeep,
            @RequestParam(value = "titleColor",    required = false) String titleColor,
            @RequestParam(value = "contentColor",  required = false) String contentColor,
            @RequestParam(value = "titleFontSize", required = false) String titleFontSize) {
        try {
            return ResponseEntity.ok(publicationService.updatePublication(id, titre, contenue, type, userId,
                    newImages, imagesToKeep, newPdfs, pdfsToKeep, titleColor, contentColor, titleFontSize));
        } catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(e.getMessage()); }
        catch (RuntimeException e)           { return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage()); }
        catch (Exception e)                  { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error"); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id, @RequestParam Integer userId) {
        try { publicationService.deletePublication(id, userId); return ResponseEntity.ok("Deleted"); }
        catch (RuntimeException e) { return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage()); }
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> adminDelete(@PathVariable Integer id) {
        try { publicationService.adminDeletePublication(id); return ResponseEntity.ok("Deleted"); }
        catch (RuntimeException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); }
    }
}