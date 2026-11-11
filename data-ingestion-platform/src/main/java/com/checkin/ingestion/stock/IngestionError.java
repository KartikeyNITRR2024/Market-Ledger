package com.checkin.ingestion.stock;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
    name = "ingestion_errors",
    indexes = {
        @Index(name = "idx_ingestion_errors_task", columnList = "task_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "row_num", nullable = false)
    private long rowNum;

    @Column(name = "raw_row", length = 500)
    private String rawRow;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;
}
