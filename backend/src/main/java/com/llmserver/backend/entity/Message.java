package com.llmserver.backend.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import java.util.Date;
import org.springframework.lang.Nullable;

// Message acts primarily as an entity,
// But also as a DTO
public class Message {

  @DocumentId @Nullable public String id;
  public String text;
  public String role;
  @ServerTimestamp @Nullable public Date timestamp;

  public Message() {}

  public Message(String text, String role) {
    this.text = text;
    this.role = role;
  }
}