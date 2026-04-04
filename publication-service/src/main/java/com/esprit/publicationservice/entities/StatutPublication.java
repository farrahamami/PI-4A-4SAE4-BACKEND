package com.esprit.publicationservice.entities;

public enum StatutPublication {
    ACTIVE,    // visible normalement
    ARCHIVED,  // archivé (3 signalements ou action admin)
    PENDING    // en attente de réactivation par l'admin
}
