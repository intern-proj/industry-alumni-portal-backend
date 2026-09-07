package com.portal.certificateservice;

import com.portal.certificateservice.service.PdfGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = PdfGeneratorService.class)
class PdfGeneratorServiceTest {

    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    @Test
    void testGeneratePdfCertificate_Success() {
        UUID certId = UUID.randomUUID();
        String verificationCode = "CERT-TEST-1234";
        String studentName = "Jane Doe";
        String eventTitle = "Career Guidance Workshop";
        LocalDateTime issuedAt = LocalDateTime.now();

        String pdfPath = pdfGeneratorService.generatePdfCertificate(
                certId, verificationCode, studentName, eventTitle, issuedAt
        );

        assertNotNull(pdfPath);
        File pdfFile = new File(pdfPath);
        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.length() > 0);

        pdfFile.deleteOnExit();
    }
}
