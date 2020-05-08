package com.github.pmoerenhout.jsmppmodem.events;

import org.springframework.context.ApplicationEvent;

public class ReceivedPduEvent extends ApplicationEvent {

  private String connectionId;
  private byte[] pdu;

  public ReceivedPduEvent(final Object source, final String connectionId, final byte[] pdu) {
    super(source);
    this.connectionId = connectionId;
    this.pdu = pdu;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public byte[] getPdu() {
    return pdu;
  }
}
