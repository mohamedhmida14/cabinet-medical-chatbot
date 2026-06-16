package com.essai.cabinet.controller;

import com.essai.cabinet.model.User;
import com.essai.cabinet.service.ChatbotService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
public class ChatbotController {
    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping("/chatbot")
    public String chatbotPage() {
        return "chatbot";
    }

    @PostMapping("/chatbot/ask")
    @ResponseBody
    public Map<String, String> ask(@RequestBody Map<String, String> body, HttpSession session) {
        String question = body.getOrDefault("question", "");
        User user = session.getAttribute("user") instanceof User loggedUser ? loggedUser : null;
        Long patientId = session.getAttribute("patientId") instanceof Long patientSessionId ? patientSessionId : null;
        Long doctorId = session.getAttribute("doctorId") instanceof Long doctorSessionId ? doctorSessionId : null;
        return Map.of("answer", chatbotService.answer(question, user, patientId, doctorId));
    }
}
