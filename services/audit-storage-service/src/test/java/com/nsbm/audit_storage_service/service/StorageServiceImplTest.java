package com.nsbm.audit_storage_service.service;

import com.nsbm.audit_storage_service.config.StorageProperties;
import com.nsbm.audit_storage_service.dto.StoredFileResponse;
import com.nsbm.audit_storage_service.exception.ResourceNotFoundException;
import com.nsbm.audit_storage_service.exception.StorageException;
import com.nsbm.audit_storage_service.model.FileType;
import com.nsbm.audit_storage_service.model.StoredFile;
import com.nsbm.audit_storage_service.repository.StoredFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageServiceImplTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private StorageProperties storageProperties;

    @Mock
    private StoredFileRepository storedFileRepository;

    @InjectMocks
    private StorageServiceImpl storageService;

    private UUID fileId;
    private StoredFile storedFile;
    private MockMultipartFile multipartFile;

    @BeforeEach
    void setUp() {
        fileId = UUID.randomUUID();

        storedFile = StoredFile.builder()
                .fileId(fileId)
                .originalFilename("resume.pdf")
                .contentType("application/pdf")
                .fileSizeBytes(11L)
                .storageKey("resume/user-1/key_resume.pdf")
                .storageUrl("http://localhost:9000/icu-platform-files/resume/user-1/key_resume.pdf")
                .uploaderId("user-1")
                .uploadTimestamp(Instant.parse("2026-08-14T05:00:00Z"))
                .fileType(FileType.RESUME)
                .version(1)
                .build();

        multipartFile = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );
    }

    @Test
    void upload_storesObjectAndPersistsMetadata() throws Exception {
        when(storageProperties.getBucketName()).thenReturn("icu-platform-files");
        when(storageProperties.getEndpoint()).thenReturn("http://localhost:9000");
        when(storedFileRepository.findByUploaderIdAndFileTypeOrderByVersionDesc("user-1", FileType.RESUME))
                .thenReturn(List.of());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        when(storedFileRepository.save(any(StoredFile.class))).thenAnswer(invocation -> {
            StoredFile toSave = invocation.getArgument(0);
            return StoredFile.builder()
                    .fileId(fileId)
                    .originalFilename(toSave.getOriginalFilename())
                    .contentType(toSave.getContentType())
                    .fileSizeBytes(toSave.getFileSizeBytes())
                    .storageKey(toSave.getStorageKey())
                    .storageUrl(toSave.getStorageUrl())
                    .uploaderId(toSave.getUploaderId())
                    .uploadTimestamp(toSave.getUploadTimestamp())
                    .fileType(toSave.getFileType())
                    .version(toSave.getVersion())
                    .build();
        });

        StoredFileResponse response = storageService.upload(multipartFile, "user-1", FileType.RESUME);

        assertNotNull(response);
        assertEquals(fileId, response.getFileId());
        assertEquals("resume.pdf", response.getOriginalFilename());
        assertEquals(FileType.RESUME, response.getFileType());
        assertEquals(1, response.getVersion());
        assertTrue(response.getStorageUrl().contains("icu-platform-files"));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(storedFileRepository, times(1)).save(any(StoredFile.class));
    }

    @Test
    void upload_incrementsVersionForSameUploaderAndType() {
        StoredFile existing = StoredFile.builder()
                .fileId(UUID.randomUUID())
                .version(2)
                .fileType(FileType.RESUME)
                .uploaderId("user-1")
                .build();

        when(storageProperties.getBucketName()).thenReturn("icu-platform-files");
        when(storageProperties.getEndpoint()).thenReturn("http://localhost:9000/");
        when(storedFileRepository.findByUploaderIdAndFileTypeOrderByVersionDesc("user-1", FileType.RESUME))
                .thenReturn(List.of(existing));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        when(storedFileRepository.save(any(StoredFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoredFileResponse response = storageService.upload(multipartFile, "user-1", FileType.RESUME);

        assertEquals(3, response.getVersion());
        ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
        verify(storedFileRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getVersion());
    }

    @Test
    void upload_rejectsEmptyFile() {
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> storageService.upload(emptyFile, "user-1", FileType.RESUME));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(storedFileRepository, never()).save(any(StoredFile.class));
    }

    @Test
    void upload_rejectsMissingUploaderId() {
        assertThrows(IllegalArgumentException.class,
                () -> storageService.upload(multipartFile, "  ", FileType.RESUME));
        verify(storedFileRepository, never()).save(any(StoredFile.class));
    }

    @Test
    void upload_wrapsS3Failures() {
        when(storageProperties.getBucketName()).thenReturn("icu-platform-files");
        when(storageProperties.getEndpoint()).thenReturn("http://localhost:9000");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("boom").build());

        assertThrows(StorageException.class,
                () -> storageService.upload(multipartFile, "user-1", FileType.RESUME));
        verify(storedFileRepository, never()).save(any(StoredFile.class));
    }

    @Test
    void upload_wrapsIoFailures() throws Exception {
        MultipartFile failingFile = mock(MultipartFile.class);
        when(failingFile.isEmpty()).thenReturn(false);
        when(failingFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(failingFile.getContentType()).thenReturn("application/pdf");
        when(failingFile.getSize()).thenReturn(10L);
        when(failingFile.getBytes()).thenThrow(new IOException("read failed"));
        when(storageProperties.getBucketName()).thenReturn("icu-platform-files");
        when(storageProperties.getEndpoint()).thenReturn("http://localhost:9000");

        StorageException exception = assertThrows(StorageException.class,
                () -> storageService.upload(failingFile, "user-1", FileType.RESUME));

        assertEquals("Failed to read uploaded file bytes", exception.getMessage());
    }

    @Test
    void download_returnsResourceBytes() {
        @SuppressWarnings("unchecked")
        ResponseBytes<GetObjectResponse> responseBytes = mock(ResponseBytes.class);
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.of(storedFile));
        when(storageProperties.getBucketName()).thenReturn("icu-platform-files");
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);
        when(responseBytes.asByteArray()).thenReturn("pdf-content".getBytes());

        Resource resource = storageService.download(fileId);

        assertNotNull(resource);
        assertEquals("resume.pdf", resource.getFilename());
        assertArrayEquals("pdf-content".getBytes(), ((org.springframework.core.io.ByteArrayResource) resource).getByteArray());
        verify(s3Client, times(1)).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    void download_throwsWhenFileMissing() {
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> storageService.download(fileId));
        verify(s3Client, never()).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    void download_wrapsS3Failures() {
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.of(storedFile));
        when(storageProperties.getBucketName()).thenReturn("icu-platform-files");
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("missing").build());

        assertThrows(StorageException.class, () -> storageService.download(fileId));
    }

    @Test
    void getMetadata_returnsStoredFile() {
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.of(storedFile));

        StoredFileResponse response = storageService.getMetadata(fileId);

        assertEquals(fileId, response.getFileId());
        assertEquals("resume.pdf", response.getOriginalFilename());
        assertEquals(FileType.RESUME, response.getFileType());
    }

    @Test
    void getMetadata_throwsWhenMissing() {
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> storageService.getMetadata(fileId));
    }

    @Test
    void listByUploader_mapsResults() {
        when(storedFileRepository.findByUploaderIdOrderByUploadTimestampDesc("user-1"))
                .thenReturn(List.of(storedFile));

        List<StoredFileResponse> responses = storageService.listByUploader("user-1");

        assertEquals(1, responses.size());
        assertEquals(fileId, responses.getFirst().getFileId());
        verify(storedFileRepository, times(1)).findByUploaderIdOrderByUploadTimestampDesc("user-1");
    }

    @Test
    void delete_removesObjectAndMetadata() {
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.of(storedFile));
        when(storageProperties.getBucketName()).thenReturn("icu-platform-files");
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        storageService.delete(fileId);

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
        verify(storedFileRepository, times(1)).delete(storedFile);
    }

    @Test
    void delete_throwsWhenFileMissing() {
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> storageService.delete(fileId));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        verify(storedFileRepository, never()).delete(any(StoredFile.class));
    }

    @Test
    void delete_wrapsS3Failures() {
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.of(storedFile));
        when(storageProperties.getBucketName()).thenReturn("icu-platform-files");
        doThrow(S3Exception.builder().message("delete failed").build())
                .when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        assertThrows(StorageException.class, () -> storageService.delete(fileId));
        verify(storedFileRepository, never()).delete(any(StoredFile.class));
    }
}
