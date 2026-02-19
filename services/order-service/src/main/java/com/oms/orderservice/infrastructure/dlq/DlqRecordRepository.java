package com.oms.orderservice.infrastructure.dlq;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DlqRecordRepository extends JpaRepository<DlqRecord, UUID> {
    List<DlqRecord> findAllByOrderByFailedAtDesc();

    List<DlqRecord> findByStatus(DlqStatus status);
}
