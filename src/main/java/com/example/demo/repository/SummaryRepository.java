package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.SavedSummary;

public interface SummaryRepository extends JpaRepository<SavedSummary, Long> {


}
