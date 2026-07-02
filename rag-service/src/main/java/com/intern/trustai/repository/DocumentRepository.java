package com.intern.trustai.repository;

import com.intern.trustai.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
