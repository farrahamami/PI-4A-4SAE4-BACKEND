package com.esprit.microservice.pidev.Event.Controllers;

import com.esprit.microservice.pidev.Event.DTOs.EventInscriptionRequestDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventInscriptionResponseDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventInscriptionRequestDTO;
import com.esprit.microservice.pidev.Event.DTOs.EventInscriptionResponseDTO;
import com.esprit.microservice.pidev.Event.Services.EventInscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inscriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InscriptionController {

    private final EventInscriptionService inscriptionService;

    // USER : Soumettre une demande d'inscription
    @PostMapping
    public ResponseEntity<EventInscriptionResponseDTO> submit(@RequestBody EventInscriptionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inscriptionService.submitInscription(request));
    }

    // ADMIN : Accepter
    @PutMapping("/{id}/accept")
    public ResponseEntity<EventInscriptionResponseDTO> accept(@PathVariable Long id) {
        return ResponseEntity.ok(inscriptionService.acceptInscription(id));
    }

    // ADMIN : Refuser
    @PutMapping("/{id}/reject")
    public ResponseEntity<EventInscriptionResponseDTO> reject(@PathVariable Long id) {
        return ResponseEntity.ok(inscriptionService.rejectInscription(id));
    }

    // ADMIN : Toutes les inscriptions d'un événement
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<EventInscriptionResponseDTO>> getByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(inscriptionService.getAllInscriptionsByEvent(eventId));
    }

    // ADMIN : Demandes en attente pour un événement
    @GetMapping("/event/{eventId}/pending")
    public ResponseEntity<List<EventInscriptionResponseDTO>> getPending(@PathVariable Long eventId) {
        return ResponseEntity.ok(inscriptionService.getPendingInscriptions(eventId));
    }

    // USER : Mes inscriptions
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EventInscriptionResponseDTO>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(inscriptionService.getInscriptionsByUser(userId));
    }

    // ADMIN : Télécharger/Imprimer le badge (retourne l'image en bytes)
    @GetMapping("/{id}/badge")
    public ResponseEntity<byte[]> downloadBadge(@PathVariable Long id) {
        try {
            byte[] badgeBytes = inscriptionService.getBadgeBytes(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentDispositionFormData("attachment", "badge_" + id + ".png");
            return new ResponseEntity<>(badgeBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ADMIN : Supprimer une inscription
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inscriptionService.deleteInscription(id);
        return ResponseEntity.noContent().build();
    }
}