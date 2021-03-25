package com.github.pmoerenhout.jsmppmodem.events;

import org.springframework.context.ApplicationEvent;

public class ReceivedSmsDeliveryPduEvent extends ApplicationEvent {

  private String connectionId;
  private String subscriberNumber;
  private byte[] pdu;

  public ReceivedSmsDeliveryPduEvent(final Object source, final String connectionId, final String subscriberNumber, final byte[] pdu) {
    super(source);
    this.connectionId = connectionId;
    this.subscriberNumber = subscriberNumber;
    this.pdu = pdu;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public String getSubscriberNumber() {
    return subscriberNumber;
  }

  public byte[] getPdu() {
    return pdu;
  }
}
