package com.nsbm.audit_storage_service.service;

import com.nsbm.audit_storage_service.config.StorageProperties;
import com.nsbm.audit_storage_service.dto.StoredFileResponse;
import com.nsbm.audit_storage_service.exception.ResourceNotFoundException;
import com.nsbm.audit_storage_service.exception.StorageException;
import com.nsbm.audit_storage_service.model.FileType;
import com.nsbm.audit_storage_service.model.StoredFile;
import com.nsbm.audit_storage_service.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;
    private final StoredFileRepository storedFileRepository;

    @Override
    @Transactional
    public StoredFileResponse upload(MultipartFile file, String uploaderId, FileType fileType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (!StringUtils.hasText(uploaderId)) {
            throw new IllegalArgumentException("uploaderId is required");
        }

        FileType resolvedType = fileType != null ? fileType : FileType.OTHER;
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed"
        );
        String storageKey = buildStorageKey(uploaderId, resolvedType, originalFilename);
        String storageUrl = buildStorageUrl(storageKey);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(storageProperties.getBucketName())
                    .key(storageKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException ex) {
            throw new StorageException("Failed to read uploaded file bytes", ex);
        } catch (Exception ex) {
            throw new StorageException("Failed to upload file to object storage", ex);
        }

        int nextVersion = resolveNextVersion(uploaderId, resolvedType);

        StoredFile storedFile = StoredFile.builder()
                .originalFilename(originalFilename)
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .storageKey(storageKey)
                .storageUrl(storageUrl)
                .uploaderId(uploaderId)
                .uploadTimestamp(Instant.now())
                .fileType(resolvedType)
                .version(nextVersion)
                .build();

        StoredFile saved = storedFileRepository.save(storedFile);
        log.info("Stored file metadata fileId={} key={}", saved.getFileId(), storageKey);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource download(UUID fileId) {
        StoredFile storedFile = findStoredFile(fileId);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(storageProperties.getBucketName())
                    .key(storedFile.getStorageKey())
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return new ByteArrayResource(objectBytes.asByteArray()) {
                @Override
                public String getFilename() {
                    return storedFile.getOriginalFilename();
                }
            };
        } catch (Exception ex) {
            throw new StorageException("Failed to download file from object storage: " + fileId, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFileResponse getMetadata(UUID fileId) {
        return toResponse(findStoredFile(fileId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredFileResponse> listByUploader(String uploaderId) {
        return storedFileRepository.findByUploaderIdOrderByUploadTimestampDesc(uploaderId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID fileId) {
        StoredFile storedFile = findStoredFile(fileId);

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(storageProperties.getBucketName())
                    .key(storedFile.getStorageKey())
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception ex) {
            throw new StorageException("Failed to delete file from object storage: " + fileId, ex);
        }

        storedFileRepository.delete(storedFile);
        log.info("Deleted file fileId={} key={}", fileId, storedFile.getStorageKey());
    }

    private StoredFile findStoredFile(UUID fileId) {
        return storedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Stored file not found: " + fileId));
    }

    private int resolveNextVersion(String uploaderId, FileType fileType) {
        return storedFileRepository
                .findByUploaderIdAndFileTypeOrderByVersionDesc(uploaderId, fileType)
                .stream()
                .findFirst()
                .map(existing -> existing.getVersion() + 1)
                .orElse(1);
    }

    private String buildStorageKey(String uploaderId, FileType fileType, String originalFilename) {
        String safeName = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format(
                "%s/%s/%s_%s",
                fileType.name().toLowerCase(Locale.ROOT),
                uploaderId,
                UUID.randomUUID(),
                safeName
        );
    }

    private String buildStorageUrl(String storageKey) {
        String endpoint = storageProperties.getEndpoint();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + storageProperties.getBucketName() + "/" + storageKey;
    }

    private StoredFileResponse toResponse(StoredFile storedFile) {
        return StoredFileResponse.builder()
                .fileId(storedFile.getFileId())
                .originalFilename(storedFile.getOriginalFilename())
                .contentType(storedFile.getContentType())
                .fileSizeBytes(storedFile.getFileSizeBytes())
                .storageUrl(storedFile.getStorageUrl())
                .uploaderId(storedFile.getUploaderId())
                .uploadTimestamp(storedFile.getUploadTimestamp())
                .fileType(storedFile.getFileType())
                .version(storedFile.getVersion())
                .build();
    }
}
