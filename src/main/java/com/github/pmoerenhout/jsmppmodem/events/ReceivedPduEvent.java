package com.github.pmoerenhout.jsmppmodem.events;

import org.springframework.context.ApplicationEvent;

public class ReceivedPduEvent extends ApplicationEvent {

  private byte[] pdu;

  public ReceivedPduEvent(final Object source, final byte[] pdu) {
    super(source);
    this.pdu = pdu;
  }

  public byte[] getPdu() {
    return pdu;
  }
}
