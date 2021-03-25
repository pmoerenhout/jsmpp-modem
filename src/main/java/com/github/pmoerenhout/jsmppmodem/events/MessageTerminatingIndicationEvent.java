package com.github.pmoerenhout.jsmppmodem.events;

import org.springframework.context.ApplicationEvent;

import com.github.pmoerenhout.jsmppmodem.Modem;

public class MessageTerminatingIndicationEvent extends ApplicationEvent {

  private Modem modem;
  private String storage;
  private int index;

  public MessageTerminatingIndicationEvent(final Object source, final Modem modem, final String storage, final int index) {
    super(source);
    this.modem = modem;
    this.storage = storage;
    this.index = index;
  }

  public Modem getModem() {
    return modem;
  }

  public String getStorage() {
    return storage;
  }

  public int getIndex() {
    return index;
  }
}
