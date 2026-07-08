package com.example.demo.repository;

import com.example.demo.model.SavedSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SummaryRepository extends JpaRepository<SavedSummary, Long> {
}
