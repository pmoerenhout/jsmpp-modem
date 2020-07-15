package com.github.pmoerenhout.jsmppmodem.events;

import org.springframework.context.ApplicationEvent;

import com.github.pmoerenhout.atcommander.module._3gpp.RegistrationState;

public class NetworkRegistrationEvent extends ApplicationEvent {

  private RegistrationState registrationState;

  public NetworkRegistrationEvent(final Object source, final RegistrationState registrationState) {
    super(source);
    this.registrationState = registrationState;
  }

  public RegistrationState getRegistrationState() {
    return registrationState;
  }
}
