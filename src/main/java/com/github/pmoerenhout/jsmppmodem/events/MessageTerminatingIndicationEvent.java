package com.github.pmoerenhout.jsmppmodem.events;

import org.springframework.context.ApplicationEvent;

public class MessageTerminatingIndicationEvent extends ApplicationEvent {

  private String connectionId;
  private String storage;
  private int index;

  public MessageTerminatingIndicationEvent(final Object source, final String connectionId, final String storage, final int index) {
    super(source);
    this.connectionId = connectionId;
    this.storage = storage;
    this.index = index;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public String getStorage() {
    return storage;
  }

  public int getIndex() {
    return index;
  }
}
