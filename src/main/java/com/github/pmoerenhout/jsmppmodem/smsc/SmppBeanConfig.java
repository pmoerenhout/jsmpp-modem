package com.github.pmoerenhout.jsmppmodem.smsc;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.session.SMPPSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmppBeanConfig {

  @Value("#{T(com.github.pmoerenhout.jsmppmodem.smsc.CharsetService).getCharset('${smpp.charset}')}")
  private Charset charset;

  @Value("${smsc.simulator.submit-sm-reply-delay:10000}")
  private long submitSmReplyDelay;

  @Bean
  @Qualifier("messageReceiverListener")
  public MessageReceiverListenerImpl getMessageReceiverListenerImpl() {
    return new MessageReceiverListenerImpl();
  }

  @Bean
  @Qualifier("smppSession")
  public SMPPSession getSMPPSession() {
    return new SMPPSession();
  }

  @Bean
  @Qualifier("serverMessageReceiverListener")
  public ServerMessageReceiverListenerImpl getServerMessageReceiverListener() {
    return new ServerMessageReceiverListenerImpl(charset, submitSmReplyDelay);
  }

  @Bean
  @Qualifier("serverResponseDeliveryListener")
  public ServerResponseDeliveryListenerImpl getServerResponseDeliveryListener() {
    return new ServerResponseDeliveryListenerImpl();
  }

  @Bean
  @Qualifier("sessions")
  public List<SMPPServerSession> getSessions() {
    return new ArrayList<>();
  }

  @Bean
  @Qualifier("sessionStateListener")
  public SessionStateListenerImpl getSessionStateListener() {
    return new SessionStateListenerImpl(getSessions());
  }
}
