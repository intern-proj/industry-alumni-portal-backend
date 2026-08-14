package com.nsbm.audit_storage_service.service;

import com.nsbm.audit_storage_service.dto.StoredFileResponse;
import com.nsbm.audit_storage_service.model.FileType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface StorageService {

    StoredFileResponse upload(MultipartFile file, String uploaderId, FileType fileType);

    Resource download(UUID fileId);

    StoredFileResponse getMetadata(UUID fileId);

    List<StoredFileResponse> listByUploader(String uploaderId);

    void delete(UUID fileId);
}
