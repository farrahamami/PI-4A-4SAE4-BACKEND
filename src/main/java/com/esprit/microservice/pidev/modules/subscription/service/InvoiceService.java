package com.esprit.microservice.pidev.modules.subscription.service;

import com.esprit.microservice.pidev.modules.subscription.domain.entities.UserSubscription;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class InvoiceService {

    private static final DeviceRgb PRIMARY = new DeviceRgb(79, 70, 229);
    private static final DeviceRgb GRAY = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb LIGHT_BG = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generateInvoice(UserSubscription sub) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);
            doc.setMargins(40, 40, 40, 40);

            // ── HEADER ──
            doc.add(new Paragraph("PROLANCE")
                    .setFontSize(28).setBold().setFontColor(PRIMARY));
            doc.add(new Paragraph("Plateforme Freelance")
                    .setFontSize(10).setFontColor(GRAY));
            doc.add(new Paragraph("\n"));

            // ── FACTURE TITLE ──
            doc.add(new Paragraph("FACTURE")
                    .setFontSize(22).setBold().setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("N° FAC-" + sub.getId() + "-" + sub.getStartDate().getYear())
                    .setFontSize(10).setFontColor(GRAY).setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("Date : " + sub.getCreatedAt().format(DATE_FMT))
                    .setFontSize(10).setFontColor(GRAY).setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("\n"));

            // ── CLIENT ──
            doc.add(new Paragraph("Facturé à :").setFontSize(10).setBold().setFontColor(GRAY));
            doc.add(new Paragraph(sub.getUser().getName() + " " + sub.getUser().getLastName())
                    .setFontSize(12).setBold());
            doc.add(new Paragraph(sub.getUser().getEmail()).setFontSize(10).setFontColor(GRAY));
            doc.add(new Paragraph("\n"));

            // ── TABLEAU ──
            Table table = new Table(UnitValue.createPercentArray(new float[]{4, 1, 1, 1}));
            table.setWidth(UnitValue.createPercentValue(100));

            addHeaderCell(table, "Description");
            addHeaderCell(table, "Durée");
            addHeaderCell(table, "Prix HT");
            addHeaderCell(table, "TVA 19%");

            String planName = sub.getSubscription().getName();
            String planType = sub.getSubscription().getType().name();
            String cycle = sub.getSubscription().getBillingCycle().name().equals("SEMESTRIELLE")
                    ? "6 mois" : "12 mois";

            BigDecimal total = sub.getAmountPaid();
            BigDecimal ht = total.divide(new BigDecimal("1.19"), 2, RoundingMode.HALF_UP);
            BigDecimal tva = total.subtract(ht);

            addDataCell(table, "Plan " + planName + " (" + planType + ")");
            addDataCell(table, cycle);
            addDataCell(table, ht + " DT");
            addDataCell(table, tva.setScale(2, RoundingMode.HALF_UP) + " DT");

            doc.add(table);
            doc.add(new Paragraph("\n"));

            // ── TOTAUX ──
            Table totals = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
            totals.setWidth(UnitValue.createPercentValue(50));
            totals.setHorizontalAlignment(HorizontalAlignment.RIGHT);

            addTotalRow(totals, "Sous-total HT", ht + " DT", false);
            addTotalRow(totals, "TVA (19%)", tva.setScale(2, RoundingMode.HALF_UP) + " DT", false);
            addTotalRow(totals, "TOTAL TTC", total + " DT", true);

            doc.add(totals);
            doc.add(new Paragraph("\n"));

            // ── PÉRIODE ──
            doc.add(new Paragraph("Période d'abonnement")
                    .setFontSize(10).setBold().setFontColor(GRAY));
            doc.add(new Paragraph("Du " + sub.getStartDate().format(DATE_FMT)
                    + " au " + sub.getEndDate().format(DATE_FMT)).setFontSize(10));

            if (sub.getTransactionId() != null) {
                doc.add(new Paragraph("\nRéf. paiement : " + sub.getTransactionId())
                        .setFontSize(9).setFontColor(GRAY));
            }
            if (sub.getPaymentMethod() != null) {
                doc.add(new Paragraph("Méthode : " + sub.getPaymentMethod())
                        .setFontSize(9).setFontColor(GRAY));
            }

            doc.add(new Paragraph("\n\n"));
            doc.add(new Paragraph("Merci pour votre confiance !")
                    .setFontSize(10).setFontColor(PRIMARY).setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Prolance — support@prolance.tn")
                    .setFontSize(8).setFontColor(GRAY).setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur génération facture: {}", e.getMessage());
            throw new RuntimeException("Impossible de générer la facture", e);
        }
    }

    private void addHeaderCell(Table table, String text) {
        table.addHeaderCell(new Cell()
                .add(new Paragraph(text).setFontSize(10).setBold().setFontColor(WHITE))
                .setBackgroundColor(PRIMARY).setPadding(8));
    }

    private void addDataCell(Table table, String text) {
        table.addCell(new Cell()
                .add(new Paragraph(text).setFontSize(10))
                .setBackgroundColor(LIGHT_BG).setPadding(8));
    }

    private void addTotalRow(Table table, String label, String value, boolean isBold) {
        Paragraph labelP = new Paragraph(label).setFontSize(isBold ? 13 : 10);
        Paragraph valueP = new Paragraph(value).setFontSize(isBold ? 13 : 10)
                .setTextAlignment(TextAlignment.RIGHT);
        if (isBold) {
            labelP.setBold().setFontColor(PRIMARY);
            valueP.setBold().setFontColor(PRIMARY);
        }
        table.addCell(new Cell().add(labelP).setBorder(null).setPaddingBottom(4));
        table.addCell(new Cell().add(valueP).setBorder(null).setPaddingBottom(4));
    }
}