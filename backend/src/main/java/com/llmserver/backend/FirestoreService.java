package com.llmserver.backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.llmserver.backend.entity.Message;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FirestoreService {

  private final Firestore firestore;
  private static final String USER_COLLECTION = "users";
  private static final String CONVERSATION_COLLECTION = "conversations";

  public FirestoreService(Firestore firestore) {
    this.firestore = firestore;
  }

  public Mono<String> createConversation(String userId) {
    String conversationId = UUID.randomUUID().toString();
    return Mono.just(conversationId);
  }

  public Mono<Void> saveMessage(String userId, String conversationId, Message message) {
    CollectionReference conversationCollection =
        firestore
            .collection(USER_COLLECTION)
            .document(userId)
            .collection(CONVERSATION_COLLECTION)
            .document(conversationId)
            .collection("messages");

    ApiFuture<WriteResult> result = conversationCollection.add(message);
    return Mono.fromFuture(result).then();
  }

  public Mono<List<Message>> getConversationHistory(String userId, String conversationId) {
    CollectionReference conversationCollection =
        firestore
            .collection(USER_COLLECTION)
            .document(userId)
            .collection(CONVERSATION_COLLECTION)
            .document(conversationId)
            .collection("messages");

    ApiFuture<QuerySnapshot> querySnapshot =
        conversationCollection
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .get();

    return Mono.fromFuture(querySnapshot)
        .map(snapshot -> snapshot.toObjects(Message.class));
  }
}