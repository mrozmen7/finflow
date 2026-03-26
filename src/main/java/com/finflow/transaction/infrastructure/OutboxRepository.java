package com.finflow.transaction.infrastructure;

import com.finflow.transaction.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for transactional outbox events.
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Returns all events with the given status ordered by creation time ascending,
     * so the oldest pending events are processed first.
     *
     * @param status one of {@code PENDING}, {@code SENT}, {@code FAILED}
     * @return events in creation order
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status);
}
