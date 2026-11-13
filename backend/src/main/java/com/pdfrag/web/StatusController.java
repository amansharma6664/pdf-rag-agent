package com.pdfrag.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pdfrag.client.OpenAiClient;
import com.pdfrag.config.AppProperties;
import com.pdfrag.dto.Dtos.StatusDto;

/**
 * Lets the frontend check whether the OpenAI key is set (it shows a warning if not).
 */
@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final OpenAiClient openAi;
    private final AppProperties props;

    public StatusController(OpenAiClient openAi, AppProperties props) {
        this.openAi = openAi;
        this.props = props;
    }

    @GetMapping
    public StatusDto status() {
        return new StatusDto(
                openAi.isConfigured(),
                props.openai().chatModel(),
                props.openai().embeddingModel(),
                props.rag().topK());
    }
}
