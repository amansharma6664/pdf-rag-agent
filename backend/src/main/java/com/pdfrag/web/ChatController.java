package com.pdfrag.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pdfrag.dto.Dtos.ChatRequest;
import com.pdfrag.dto.Dtos.ChatResponse;
import com.pdfrag.service.ChatService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return service.ask(request);
    }
}
