package com.example;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.json.JSONArray;
import org.json.JSONObject;

public class reportGenerator {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final LocalTime START_OF_DAY = LocalTime.of(8, 30);
    private static final LocalTime END_OF_DAY = LocalTime.of(18, 0);
    private static final LocalTime LUNCH_START = LocalTime.of(12, 30);
    private static final LocalTime LUNCH_END = LocalTime.of(14, 0);

    public static void generateReport() {
        try {
            String jsonContent = new String(Files.readAllBytes(Paths.get("daily_commits.json")));
            JSONArray commits = new JSONArray(jsonContent);

            saveReport(commits);
            
            System.out.println("Rapport généré: activity_report.pdf");
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du rapport: " + e.getMessage());
        }
    }

    private static void saveReport(JSONArray commits) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            try {
                PDFont titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDFont tableHeaderFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDFont tableFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                
                int yPosition = 750;
                
                // Titre du rapport
                contentStream.setFont(titleFont, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, yPosition);
                contentStream.showText("RAPPORT DE TRAVAIL QUOTIDIEN - " + java.time.LocalDate.now());
                contentStream.endText();
                
                yPosition -= 30;
                
                // Informations sur les horaires
                contentStream.setFont(tableFont, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, yPosition);
                contentStream.showText("Heure de début: " + START_OF_DAY.format(TIME_FORMATTER) + 
                                      " | Heure de fin: " + END_OF_DAY.format(TIME_FORMATTER) +
                                      " | Pause déjeuner: " + LUNCH_START.format(TIME_FORMATTER) + 
                                      " - " + LUNCH_END.format(TIME_FORMATTER));
                contentStream.endText();
                
                yPosition -= 25;
                
                // Tableau récapitulatif
                contentStream.setFont(titleFont, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, yPosition);
                contentStream.showText("TABLEAU RÉCAPITULATIF DES ACTIVITÉS");
                contentStream.endText();
                
                yPosition -= 20;
                
                // Dessiner le tableau seulement
                drawSummaryTable(contentStream, document, commits, yPosition, tableHeaderFont, tableFont);
                
            } finally {
                contentStream.close();
            }

            document.save("activity_report.pdf");
            System.out.println("Rapport PDF généré avec succès");
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde du PDF: " + e.getMessage());
        }
    }

    private static void drawSummaryTable(PDPageContentStream contentStream, PDDocument document, 
                                       JSONArray commits, int startY, 
                                       PDFont headerFont, PDFont contentFont) throws IOException {
        
        float margin = 50;
        float yPosition = startY;
        float rowHeight = 20;
        float[] columnWidths = {80, 340, 80};
        String[] headers = {"Heure", "Tâche", "Durée"};
        
        // Dessiner l'en-tête du tableau
        contentStream.setFont(headerFont, 10);
        float xPosition = margin;
        
        // Dessiner les bordures de l'en-tête
        contentStream.setLineWidth(1.5f);
        contentStream.moveTo(margin, yPosition + 5);
        contentStream.lineTo(margin + 500, yPosition + 5);
        contentStream.stroke();
        
        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(xPosition + 5, yPosition - 12);
            contentStream.showText(headers[i]);
            contentStream.endText();
            xPosition += columnWidths[i];
        }
        
        yPosition -= rowHeight;
        
        // Remplir le tableau avec les données des commits
        contentStream.setFont(contentFont, 8);
        LocalTime previousTime = START_OF_DAY;
        
        for (int i = 0; i < commits.length(); i++) {
            JSONObject commit = commits.getJSONObject(i);
            String commitTimeStr = commit.getString("time").substring(11, 16);
            String message = commit.getString("message");
            
            // Calculer la durée
            LocalTime commitTime = LocalTime.parse(commitTimeStr, TIME_FORMATTER);
            Duration duration = Duration.between(previousTime, commitTime);
            String durationStr = Math.abs(duration.toMinutes()) + " min";
            
            xPosition = margin;
            
            // Heure
            contentStream.beginText();
            contentStream.newLineAtOffset(xPosition + 5, yPosition - 12);
            contentStream.showText(commitTimeStr);
            contentStream.endText();
            xPosition += columnWidths[0];
            
            // Tâche (tronquée si trop longue)
            String truncatedMessage = message.length() > 50 ? message.substring(0, 47) + "..." : message;
            contentStream.beginText();
            contentStream.newLineAtOffset(xPosition + 5, yPosition - 12);
            contentStream.showText(truncatedMessage);
            contentStream.endText();
            xPosition += columnWidths[1];
            
            // Durée
            contentStream.beginText();
            contentStream.newLineAtOffset(xPosition + 5, yPosition - 12);
            contentStream.showText(durationStr);
            contentStream.endText();
            
            // Ligne de séparation
            contentStream.setLineWidth(0.5f);
            contentStream.moveTo(margin, yPosition - 5);
            contentStream.lineTo(margin + 500, yPosition - 5);
            contentStream.stroke();
            
            yPosition -= rowHeight;
            previousTime = commitTime;
            
            // Nouvelle page si nécessaire
            if (yPosition < 100 && i < commits.length() - 1) {
                contentStream.close();
                
                PDPage newPage = new PDPage();
                document.addPage(newPage);
                contentStream = new PDPageContentStream(document, newPage);
                
                // Réinitialiser les positions pour la nouvelle page
                yPosition = 750;
                
                
                contentStream.setFont(headerFont, 10);
                xPosition = margin;
                for (int j = 0; j < headers.length; j++) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition + 5, yPosition - 12);
                    contentStream.showText(headers[j]);
                    contentStream.endText();
                    xPosition += columnWidths[j];
                }
                yPosition -= rowHeight;
            }
        }
        
        
        
            contentStream.close();
        
    }
}