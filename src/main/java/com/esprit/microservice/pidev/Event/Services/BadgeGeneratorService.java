package com.esprit.microservice.pidev.Event.Services;

import com.esprit.microservice.pidev.Event.Entities.EventInscription;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;

@Service
public class BadgeGeneratorService {

    private static final String BADGE_OUTPUT_DIR = "badges/";
    private static final String APP_LOGO_PATH = "src/main/resources/static/logo.png";

    public String generateBadge(EventInscription inscription) {
        try {
            // Créer le dossier si inexistant
            new File(BADGE_OUTPUT_DIR).mkdirs();

            int width = 600;
            int height = 400;

            BufferedImage badge = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = badge.createGraphics();

            // Activation de l'antialiasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Fond dégradé
            GradientPaint gradient = new GradientPaint(0, 0, new Color(30, 30, 60),
                    width, height, new Color(60, 60, 120));
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, width, height);

            // Bordure dorée
            g2d.setColor(new Color(255, 215, 0));
            g2d.setStroke(new BasicStroke(4));
            g2d.drawRect(10, 10, width - 20, height - 20);

            // Logo application (top-left)
            try {
                BufferedImage logo = ImageIO.read(new File(APP_LOGO_PATH));
                g2d.drawImage(logo, 20, 20, 80, 80, null);
            } catch (Exception e) {
                // Logo non trouvé, on continue
            }

            // Photo du participant (top-right)
            if (inscription.getImageUrl() != null && !inscription.getImageUrl().isEmpty()) {
                try {
                    BufferedImage userPhoto;
                    if (inscription.getImageUrl().startsWith("http")) {
                        userPhoto = ImageIO.read(new URL(inscription.getImageUrl()));
                    } else {
                        userPhoto = ImageIO.read(new File(inscription.getImageUrl()));
                    }
                    // Photo circulaire
                    BufferedImage circlePhoto = makeCircular(userPhoto, 100);
                    g2d.drawImage(circlePhoto, width - 130, 20, 110, 110, null);
                } catch (Exception e) {
                    // Photo non disponible
                }
            }

            // Titre de l'événement
            g2d.setColor(new Color(255, 215, 0));
            g2d.setFont(new Font("Arial", Font.BOLD, 22));
            String eventTitle = inscription.getEvent().getTitle();
            drawCenteredString(g2d, eventTitle, width, 150);

            // Ligne séparatrice
            g2d.setColor(new Color(255, 215, 0, 150));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawLine(40, 165, width - 40, 165);

            // Nom complet
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 26));
            String fullName = inscription.getParticipantPrenom() + " " + inscription.getParticipantNom();
            drawCenteredString(g2d, fullName, width, 210);

            // Rôle
            g2d.setColor(new Color(173, 216, 230));
            g2d.setFont(new Font("Arial", Font.ITALIC, 18));
            drawCenteredString(g2d, inscription.getParticipantrole().toString(), width, 245);

            // Date
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 15));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String dateStr = "📅 " + inscription.getEvent().getStartDate().format(formatter);
            drawCenteredString(g2d, dateStr, width, 290);

            // Lieu
            String lieu = "📍 " + inscription.getEvent().getLocation();
            drawCenteredString(g2d, lieu, width, 320);

            // Domaine badge
            g2d.setColor(new Color(100, 200, 100));
            g2d.setFont(new Font("Arial", Font.BOLD, 13));
            g2d.fillRoundRect(30, 340, 150, 30, 15, 15);
            g2d.setColor(Color.WHITE);
            drawStringAt(g2d, inscription.getDemaine().toString(), 105, 360);

            g2d.dispose();

            // Sauvegarde
            String fileName = BADGE_OUTPUT_DIR + "badge_" + inscription.getId() + ".png";
            ImageIO.write(badge, "PNG", new File(fileName));

            return fileName;

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du badge", e);
        }
    }

    private void drawCenteredString(Graphics2D g2d, String text, int width, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, y);
    }

    private void drawStringAt(Graphics2D g2d, String text, int centerX, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int x = centerX - fm.stringWidth(text) / 2;
        g2d.drawString(text, x, y);
    }

    private BufferedImage makeCircular(BufferedImage source, int size) {
        BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, size, size));
        g2.drawImage(source, 0, 0, size, size, null);
        g2.dispose();
        return output;
    }
}