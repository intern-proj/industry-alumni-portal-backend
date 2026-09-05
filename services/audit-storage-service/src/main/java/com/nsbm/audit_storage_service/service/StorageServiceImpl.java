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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceImpl implements StorageService {

    private final StorageProperties storageProperties;
    private final StoredFileRepository storedFileRepository;

    @Value("${storage.download-base-url:}")
    private String configuredDownloadBaseUrl;
    
    // Directory for local file storage mock instead of S3
    private final Path storageDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "icu-uploads");

    @Override
    @Transactional
    public StoredFileResponse upload(MultipartFile file, String uploaderId, FileType fileType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (!StringUtils.hasText(uploaderId)) {
            throw new IllegalArgumentException("uploaderId is required");
        }
        
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException e) {
            throw new StorageException("Could not create storage directory", e);
        }

        FileType resolvedType = fileType != null ? fileType : FileType.OTHER;
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed"
        );
        String storageKey = buildStorageKey(uploaderId, resolvedType, originalFilename);
        String storageUrl = buildStorageUrl(storageKey);

        try {
            // Write to local file system
            Path targetLocation = storageDirectory.resolve(storageKey.replace("/", "_"));
            Files.write(targetLocation, file.getBytes());
        } catch (IOException ex) {
            throw new StorageException("Failed to upload file to local storage", ex);
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
            Path filePath = storageDirectory.resolve(storedFile.getStorageKey().replace("/", "_"));
            if (!Files.exists(filePath)) {
                throw new ResourceNotFoundException("File not found on local disk: " + storedFile.getStorageKey());
            }
            byte[] objectBytes = Files.readAllBytes(filePath);
            return new ByteArrayResource(objectBytes) {
                @Override
                public String getFilename() {
                    return storedFile.getOriginalFilename();
                }
            };
        } catch (Exception ex) {
            throw new StorageException("Failed to download file from local storage: " + fileId, ex);
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
            Path filePath = storageDirectory.resolve(storedFile.getStorageKey().replace("/", "_"));
            Files.deleteIfExists(filePath);
        } catch (Exception ex) {
            throw new StorageException("Failed to delete file from local storage: " + fileId, ex);
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

    private String buildDownloadUrl(UUID fileId) {
        if (StringUtils.hasText(configuredDownloadBaseUrl)) {
            String base = configuredDownloadBaseUrl.trim();
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            return base + "/" + fileId + "?inline=true";
        }
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String scheme = request.getHeader("X-Forwarded-Proto");
                if (!StringUtils.hasText(scheme)) {
                    scheme = request.getScheme();
                }
                String host = request.getHeader("X-Forwarded-Host");
                if (!StringUtils.hasText(host)) {
                    host = request.getHeader("Host");
                    if (!StringUtils.hasText(host)) {
                        host = request.getServerName() + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
                    }
                }
                String prefix = request.getHeader("X-Forwarded-Prefix");
                String basePath = StringUtils.hasText(prefix) ? prefix.trim() : "";
                if (basePath.endsWith("/")) {
                    basePath = basePath.substring(0, basePath.length() - 1);
                }
                return scheme + "://" + host + basePath + "/api/v1/storage/download/" + fileId + "?inline=true";
            }
        } catch (Exception ignored) {}
        return "/api/v1/storage/download/" + fileId + "?inline=true";
    }

    private StoredFileResponse toResponse(StoredFile storedFile) {
        return StoredFileResponse.builder()
                .fileId(storedFile.getFileId())
                .originalFilename(storedFile.getOriginalFilename())
                .contentType(storedFile.getContentType())
                .fileSizeBytes(storedFile.getFileSizeBytes())
                .storageUrl(storedFile.getStorageUrl())
                .downloadUrl(buildDownloadUrl(storedFile.getFileId()))
                .uploaderId(storedFile.getUploaderId())
                .uploadTimestamp(storedFile.getUploadTimestamp())
                .fileType(storedFile.getFileType())
                .version(storedFile.getVersion())
                .build();
    }
}
