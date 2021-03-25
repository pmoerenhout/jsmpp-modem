package com.github.pmoerenhout.jsmppmodem.events;

import org.springframework.context.ApplicationEvent;

import com.github.pmoerenhout.jsmppmodem.Modem;

public class ReceivedPduEvent extends ApplicationEvent {

  private Modem modem;
  private byte[] pdu;

  public ReceivedPduEvent(final Object source, final Modem modem, final byte[] pdu) {
    super(source);
    this.modem = modem;
    this.pdu = pdu;
  }

  public Modem getModem() {
    return modem;
  }

  public byte[] getPdu() {
    return pdu;
  }
}
