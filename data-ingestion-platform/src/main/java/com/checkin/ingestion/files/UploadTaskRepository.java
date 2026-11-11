package com.checkin.ingestion.files;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UploadTaskRepository extends JpaRepository<UploadTask, UUID> {

    Page<UploadTask> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
