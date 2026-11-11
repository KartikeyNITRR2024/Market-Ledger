package com.checkin.ingestion.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IngestionErrorRepository extends JpaRepository<IngestionError, Long> {

    Page<IngestionError> findByTaskId(UUID taskId, Pageable pageable);

    List<IngestionError> findByTaskId(UUID taskId);

    List<IngestionError> findByTaskIdOrderByRowNumAsc(UUID taskId);

    long countByTaskId(UUID taskId);

    void deleteByTaskId(UUID taskId);
}