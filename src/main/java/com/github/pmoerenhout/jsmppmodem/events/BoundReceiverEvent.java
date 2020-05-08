package com.github.pmoerenhout.jsmppmodem.events;

import org.jsmpp.session.SMPPServerSession;
import org.springframework.context.ApplicationEvent;

public class BoundReceiverEvent extends ApplicationEvent {

  private SMPPServerSession serverSession;

  public BoundReceiverEvent(final Object source, final SMPPServerSession serverSession) {
    super(source);
    this.serverSession = serverSession;
  }

  public SMPPServerSession getServerSession() {
    return serverSession;
  }
}
