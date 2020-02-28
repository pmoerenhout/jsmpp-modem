package com.github.pmoerenhout.jsmppmodem.events;

import org.springframework.context.ApplicationEvent;

public class BoundReceiverEvent extends ApplicationEvent {

  public BoundReceiverEvent(final Object source) {
    super(source);
  }
}
