package com.nl2sql.query.repository;

import com.nl2sql.query.entity.QueryHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {
    List<QueryHistory> findByConversationIdOrderByCreatedAtDesc(String conversationId);

    Page<QueryHistory> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);
}
