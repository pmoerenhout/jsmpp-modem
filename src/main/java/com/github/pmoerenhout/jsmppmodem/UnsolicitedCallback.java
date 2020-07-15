package com.github.pmoerenhout.jsmppmodem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import com.github.pmoerenhout.atcommander.api.UnsolicitedResponse;
import com.github.pmoerenhout.atcommander.api.UnsolicitedResponseCallback;
import com.github.pmoerenhout.atcommander.module._3gpp.unsolicited.GprsNetworkRegistrationUnsolicited;
import com.github.pmoerenhout.atcommander.module._3gpp.unsolicited.MessageTerminatingIndicationUnsolicited;
import com.github.pmoerenhout.atcommander.module._3gpp.unsolicited.MessageTerminatingUnsolicited;
import com.github.pmoerenhout.atcommander.module._3gpp.unsolicited.NetworkRegistrationUnsolicited;
import com.github.pmoerenhout.atcommander.module._3gpp.unsolicited.UnstructuredSupplementaryServiceDataUnsolicited;
import com.github.pmoerenhout.jsmppmodem.events.MessageTerminatingIndicationEvent;
import com.github.pmoerenhout.jsmppmodem.events.NetworkRegistrationEvent;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedPduEvent;
import com.github.pmoerenhout.jsmppmodem.util.Util;

public class UnsolicitedCallback implements UnsolicitedResponseCallback {

  private static final Logger log = LoggerFactory.getLogger(UnsolicitedCallback.class);

  private String id;
  private ApplicationEventPublisher applicationEventPublisher;

  public UnsolicitedCallback(final String id) {
    this.id = id;
    this.applicationEventPublisher = ApplicationContextProvider.getApplicationContext();
  }

  public void unsolicited(final UnsolicitedResponse response) {
    if (response instanceof NetworkRegistrationUnsolicited) {
      final NetworkRegistrationUnsolicited networkRegistration = (NetworkRegistrationUnsolicited) response;
      if (networkRegistration.getAccessTechnology() != null) {
        log.debug("[{}] Network registration: state:{} lac:{} cid:{} access:{}",
            id,
            networkRegistration.getRegistrationState(),
            networkRegistration.getLac(),
            networkRegistration.getCellId(),
            networkRegistration.getAccessTechnology());
      } else {
        log.debug("[{}] Network registration: state:{} lac:{} cid:{}",
            id,
            networkRegistration.getRegistrationState(),
            networkRegistration.getLac(),
            networkRegistration.getCellId());
      }
      applicationEventPublisher.publishEvent(new NetworkRegistrationEvent(this, networkRegistration.getRegistrationState()));
      return;
    }
    if (response instanceof GprsNetworkRegistrationUnsolicited) {
      final GprsNetworkRegistrationUnsolicited gprsNetworkRegistration = (GprsNetworkRegistrationUnsolicited) response;
      log.debug("[{}] GPRS: state:{} lac:{} cid:{}",
          id,
          gprsNetworkRegistration.getRegistrationState(),
          gprsNetworkRegistration.getLac(),
          gprsNetworkRegistration.getCellId());
      return;
    }
    if (response instanceof MessageTerminatingUnsolicited) {
      final MessageTerminatingUnsolicited messageTerminating = (MessageTerminatingUnsolicited) response;
      log.debug("[{}] Message Terminating: alpha:{} length:{} pdu:{} ({} bytes)",
          id,
          messageTerminating.getAlpha(),
          messageTerminating.getLength(),
          messageTerminating.getPdu(),
          messageTerminating.getPdu().length());
      final byte[] pdu = Util.hexToByteArray(messageTerminating.getPdu());
      applicationEventPublisher.publishEvent(new ReceivedPduEvent(this, id, pdu));
      return;
    }
    if (response instanceof MessageTerminatingIndicationUnsolicited) {
      final MessageTerminatingIndicationUnsolicited mti = (MessageTerminatingIndicationUnsolicited) response;
      log.debug("[{}] Message Terminating Indication: storage:{} index:{}",
          id, mti.getStorage(), mti.getIndex());
      applicationEventPublisher.publishEvent(new MessageTerminatingIndicationEvent(this, id, mti.getStorage(), mti.getIndex()));
      return;
    }
    if (response instanceof UnstructuredSupplementaryServiceDataUnsolicited) {
      final UnstructuredSupplementaryServiceDataUnsolicited ussd = (UnstructuredSupplementaryServiceDataUnsolicited) response;
      switch (ussd.getResponse().intValue()) {
        case 0:
          // TODO: Implement USSD Charset
          log.debug("[{}] USSD {} (DCS:{})", id, Util.onlyPrintable(ussd.getUssdString().getBytes()), ussd.getDcs());
          break;
        case 2:
          log.debug("[{}] USSD terminated by network", id);
          break;
        case 3:
          log.debug("[{}] USSD other local client has responded", id);
          break;
        case 4:
          log.debug("[{}] USSD operation not supported", id);
          break;
        case 5:
          log.debug("[{}] USSD network time out", id);
          break;
        default:
          log.debug("[{}] USSD: response:{} ussd:{} dcs:{})",
              id, ussd.getResponse(), ussd.getUssdString(), ussd.getDcs());
      }
      return;
    }
    log.info("Received unsolicited response: {} {}", response.getClass().getName(), response);

  }
}
