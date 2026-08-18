package com.portal.certificateservice;

import com.portal.certificateservice.service.PdfGeneratorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PdfGeneratorServiceTest {

    @Test
    void testGeneratePdfCertificate_CreatesPdfFileWithQrCode(@TempDir Path tempDir) {
        PdfGeneratorService pdfGeneratorService = new PdfGeneratorService();
        ReflectionTestUtils.setField(pdfGeneratorService, "storagePath", tempDir.toAbsolutePath().toString());
        ReflectionTestUtils.setField(pdfGeneratorService, "baseUrl", "https://portal.nsbm.ac.lk");

        UUID certId = UUID.randomUUID();
        String verificationCode = "CERT-TEST1234";

        String filePath = pdfGeneratorService.generatePdfCertificate(
                certId, verificationCode, "Jane Student", "Microservices Event", LocalDateTime.now()
        );

        assertNotNull(filePath);
        File pdfFile = new File(filePath);
        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.length() > 0);
        assertTrue(pdfFile.getName().contains("CERT-TEST1234"));
    }
}
