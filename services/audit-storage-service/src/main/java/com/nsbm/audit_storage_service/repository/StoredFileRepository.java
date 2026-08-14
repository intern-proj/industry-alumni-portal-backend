package com.nsbm.audit_storage_service.repository;

import com.nsbm.audit_storage_service.model.FileType;
import com.nsbm.audit_storage_service.model.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

    List<StoredFile> findByUploaderIdOrderByUploadTimestampDesc(String uploaderId);

    List<StoredFile> findByUploaderIdAndFileTypeOrderByVersionDesc(String uploaderId, FileType fileType);

    List<StoredFile> findByFileTypeOrderByUploadTimestampDesc(FileType fileType);
}
