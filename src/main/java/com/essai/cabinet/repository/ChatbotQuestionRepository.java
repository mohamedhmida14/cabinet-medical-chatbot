package com.essai.cabinet.repository;

import com.essai.cabinet.model.ChatbotQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatbotQuestionRepository extends JpaRepository<ChatbotQuestion, Long> {}
