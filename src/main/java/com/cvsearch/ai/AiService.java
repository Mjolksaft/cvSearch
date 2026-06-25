package com.cvsearch.ai;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiService {

    private final RestClient restClient;
    private final String defaultModel;

    public AiService(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.model:llama3.2}") String model) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl + "/v1")
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.defaultModel = model;
    }

    
    public String chat(String prompt) {
        return chat(prompt, defaultModel);
    }

    
    public String chat(String prompt, String model) {
        var request = new ChatRequest(model, List.of(new Message("user", prompt)), 0.7);

        var response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(ChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new RuntimeException("No response from Ollama. Is it running? (ollama run " + defaultModel + ")");
        }

        return response.choices().get(0).message().content();
    }

    

    private record ChatRequest(String model, List<Message> messages, double temperature) {
    }

    private record Message(String role, String content) {
    }

    private record ChatResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }
}
