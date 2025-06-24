package com.llmserver.backend.service;

import com.llmserver.backend.entity.Message;
import com.llmserver.backend.dto.LlmDto.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class LlmService {

  private final WebClient webClient;
  private final String apiKey;
  private final FirestoreService firestoreService;
  private static final String USER_ID = "datatrigger";

  public LlmService(
      WebClient.Builder webClientBuilder,
      @Value("${llm.api.url}") String apiUrl,
      @Value("${llm.api.key}") String apiKey,
      FirestoreService firestoreService) {
    this.webClient = webClientBuilder.baseUrl(apiUrl).build();
    this.apiKey = apiKey;
    this.firestoreService = firestoreService;
  }

  public Mono<PromptResponse> promptLlm(PromptRequest request) {
    Mono<String> conversationIdMono =
        request.conversationId() == null
            ? firestoreService.createConversation(USER_ID)
            : Mono.just(request.conversationId());

    return conversationIdMono.flatMap(
        conversationId -> {
          Mono<List<Message>> historyMono =
              request.conversationId() == null
                  ? Mono.just(Collections.emptyList())
                  : firestoreService.getConversationHistory(USER_ID, conversationId);

          return historyMono.flatMap(
              history -> {
                List<Content> contents = new ArrayList<>();
                history.stream()
                    .map(msg -> new Content(List.of(new Part(msg.text)), msg.role))
                    .forEach(contents::add);
                contents.add(new Content(List.of(new Part(request.prompt())), "user"));

                LlmRequest llmRequest = new LlmRequest(contents);

                return webClient
                    .post()
                    .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(llmRequest)
                    .retrieve()
                    .bodyToMono(LlmResponse.class)
                    .flatMap(
                        llmResponse -> {
                          String llmText = extractTextFromResponse(llmResponse);
                          Message userMessage = new Message(request.prompt(), "user");
                          Message llmMessage = new Message(llmText, "model");

                          return firestoreService
                              .saveMessage(USER_ID, conversationId, userMessage)
                              .then(firestoreService.saveMessage(USER_ID, conversationId, llmMessage))
                              .thenReturn(new PromptResponse(llmText, conversationId));
                        });
              });
        });
  }

  private String extractTextFromResponse(LlmResponse response) {
    if (response != null
        && response.candidates() != null
        && !response.candidates().isEmpty()) {
      Candidate firstCandidate = response.candidates().get(0);
      if (firstCandidate.content() != null
          && firstCandidate.content().parts() != null
          && !firstCandidate.content().parts().isEmpty()) {
        return firstCandidate.content().parts().get(0).text();
      }
    }
    return "Unable to get a response.";
  }
}