package com.portal.certificateservice.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class PdfGeneratorService {

    @Value("${certificate.storage.path:./uploads/certificates}")
    private String storagePath;

    @Value("${certificate.baseUrl:https://portal.nsbm.ac.lk}")
    private String baseUrl;

    public String generatePdfCertificate(UUID certificateId, String verificationCode, String studentName, String eventTitle, LocalDateTime issuedAt) {
        try {
            Path uploadDir = Paths.get(storagePath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String filename = "CERT_" + verificationCode + ".pdf";
            Path filePath = uploadDir.resolve(filename);

            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, new FileOutputStream(filePath.toFile()));

            document.open();

            // Styling fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, Color.DARK_GRAY);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 14, Color.GRAY);
            Font recipientFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(16, 124, 65));
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 13, Color.BLACK);
            Font eventFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
            Font codeFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 11, Color.GRAY);

            // Title
            Paragraph title = new Paragraph("CERTIFICATE OF PARTICIPATION", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Paragraph subTitle = new Paragraph("NSBM Faculty of Computing - Industry Collaboration Portal", subtitleFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            subTitle.setSpacingAfter(25);
            document.add(subTitle);

            // Content
            Paragraph isPresentedTo = new Paragraph("This is to proudly certify that", bodyFont);
            isPresentedTo.setAlignment(Element.ALIGN_CENTER);
            isPresentedTo.setSpacingAfter(15);
            document.add(isPresentedTo);

            String displayName = (studentName != null && !studentName.isBlank()) ? studentName : "Valued Student";
            Paragraph recipientName = new Paragraph(displayName, recipientFont);
            recipientName.setAlignment(Element.ALIGN_CENTER);
            recipientName.setSpacingAfter(15);
            document.add(recipientName);

            Paragraph forParticipating = new Paragraph("has successfully attended and completed the event / workshop:", bodyFont);
            forParticipating.setAlignment(Element.ALIGN_CENTER);
            forParticipating.setSpacingAfter(15);
            document.add(forParticipating);

            String displayEvent = (eventTitle != null && !eventTitle.isBlank()) ? eventTitle : "Industry Collaboration Workshop";
            Paragraph eventName = new Paragraph("\"" + displayEvent + "\"", eventFont);
            eventName.setAlignment(Element.ALIGN_CENTER);
            eventName.setSpacingAfter(30);
            document.add(eventName);

            // Date & Verification Code
            String formattedDate = issuedAt != null ? issuedAt.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
            Paragraph issueDate = new Paragraph("Issued Date: " + formattedDate, bodyFont);
            issueDate.setAlignment(Element.ALIGN_CENTER);
            issueDate.setSpacingAfter(10);
            document.add(issueDate);

            Paragraph codePara = new Paragraph("Verification Code: " + verificationCode + " | Verify at: " + baseUrl + "/api/v1/certificates/verify/" + verificationCode, codeFont);
            codePara.setAlignment(Element.ALIGN_CENTER);
            codePara.setSpacingAfter(20);
            document.add(codePara);

            document.close();
            return filePath.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF certificate: " + e.getMessage(), e);
        }
    }
}
