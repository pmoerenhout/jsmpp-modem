package com.github.pmoerenhout.jsmppmodem;

import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;

import com.github.pmoerenhout.atcommander.api.UnsolicitedResponse;
import com.github.pmoerenhout.atcommander.api.UnsolicitedResponseCallback;
import com.github.pmoerenhout.atcommander.module._3gpp.unsolicited.GprsNetworkRegistrationUnsolicited;
import com.github.pmoerenhout.atcommander.module._3gpp.unsolicited.MessageTerminatingIndicationUnsolicited;
import com.github.pmoerenhout.atcommander.module._3gpp.unsolicited.MessageTerminatingUnsolicited;
import com.github.pmoerenhout.atcommander.module._3gpp.unsolicited.NetworkRegistrationUnsolicited;
import com.github.pmoerenhout.atcommander.module._3gpp.unsolicited.UnstructuredSupplementaryServiceDataUnsolicited;
import com.github.pmoerenhout.atcommander.module.v250.unsolicited.RingUnsolicited;
import com.github.pmoerenhout.jsmppmodem.events.MessageTerminatingIndicationEvent;
import com.github.pmoerenhout.jsmppmodem.events.NetworkRegistrationEvent;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedPduEvent;
import com.github.pmoerenhout.jsmppmodem.util.Util;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UnsolicitedCallback implements UnsolicitedResponseCallback {

  private Modem modem;
  private ApplicationEventPublisher applicationEventPublisher;

  public UnsolicitedCallback(final Modem modem) {
    this.modem = modem;
    this.applicationEventPublisher = ApplicationContextProvider.getApplicationContext();
  }

  public void unsolicited(final UnsolicitedResponse response) {
    if (response instanceof NetworkRegistrationUnsolicited) {
      final NetworkRegistrationUnsolicited networkRegistration = (NetworkRegistrationUnsolicited) response;
      if (networkRegistration.getAccessTechnology().isPresent()) {
        log.debug("[{}] Network registration: state:{} lac:{} cid:{} access:{}",
            modem.getId(),
            networkRegistration.getRegistrationState(),
            networkRegistration.getLac().get(),
            networkRegistration.getCellId().get(),
            networkRegistration.getAccessTechnology().get());
      } else if (networkRegistration.getLac().isPresent() && networkRegistration.getCellId().isPresent()) {
        log.debug("[{}] Network registration: state:{} lac:{} cid:{}",
            modem.getId(),
            networkRegistration.getRegistrationState(),
            networkRegistration.getLac().get(),
            networkRegistration.getCellId().get());
      } else {
        log.debug("[{}] Network registration: state:{}",
            modem.getId(), networkRegistration.getRegistrationState());
      }
      applicationEventPublisher.publishEvent(new NetworkRegistrationEvent(this, networkRegistration.getRegistrationState()));
      return;
    }
    if (response instanceof GprsNetworkRegistrationUnsolicited) {
      final GprsNetworkRegistrationUnsolicited gprsNetworkRegistration = (GprsNetworkRegistrationUnsolicited) response;
      if (gprsNetworkRegistration.getLac().isPresent() && gprsNetworkRegistration.getCellId().isPresent()) {
        log.debug("[{}] GPRS: state:{} lac:{} cid:{}",
            modem.getId(),
            gprsNetworkRegistration.getRegistrationState(),
            gprsNetworkRegistration.getLac().get(),
            gprsNetworkRegistration.getCellId().get());
      } else {
        log.debug("[{}] GPRS: state:{}",
            modem.getId(), gprsNetworkRegistration.getRegistrationState());
      }
      return;
    }
    if (response instanceof MessageTerminatingUnsolicited) {
      final MessageTerminatingUnsolicited messageTerminating = (MessageTerminatingUnsolicited) response;
      log.debug("[{}] Message Terminating: alpha:{} length:{} pdu:{} ({} bytes)",
          modem.getId(),
          messageTerminating.getAlpha(),
          messageTerminating.getLength(),
          messageTerminating.getPdu(),
          messageTerminating.getPdu().length());
      final byte[] pdu = Util.hexToByteArray(messageTerminating.getPdu());
      applicationEventPublisher.publishEvent(new ReceivedPduEvent(this, modem, pdu));
      return;
    }
    if (response instanceof MessageTerminatingIndicationUnsolicited) {
      final MessageTerminatingIndicationUnsolicited mti = (MessageTerminatingIndicationUnsolicited) response;
      log.debug("[{}] Message Terminating Indication: storage:{} index:{}",
          modem.getId(), mti.getStorage(), mti.getIndex());
      applicationEventPublisher.publishEvent(new MessageTerminatingIndicationEvent(this, modem, mti.getStorage(), mti.getIndex()));
      return;
    }
    if (response instanceof UnstructuredSupplementaryServiceDataUnsolicited) {
      final UnstructuredSupplementaryServiceDataUnsolicited ussd = (UnstructuredSupplementaryServiceDataUnsolicited) response;
      switch (ussd.getResponse().intValue()) {
        case 0:
          // TODO: Implement USSD Charset
          final Optional<String> ussdString = ussd.getUssdString();
          if (ussdString.isPresent()) {
            log.debug("[{}] USSD {} (DCS:{})", modem.getId(), Util.onlyPrintable(ussdString.get().getBytes()), ussd.getDcs().get());
          } else {
            log.debug("[{}] USSD", modem.getId());
          }
          break;
        case 2:
          log.debug("[{}] USSD terminated by network", modem.getId());
          break;
        case 3:
          log.debug("[{}] USSD other local client has responded", modem.getId());
          break;
        case 4:
          log.debug("[{}] USSD operation not supported", modem.getId());
          break;
        case 5:
          log.debug("[{}] USSD network time out", modem.getId());
          break;
        default:
          log.debug("[{}] USSD: response:{} ussd:{} dcs:{})",
              modem.getId(), ussd.getResponse(), ussd.getUssdString(), ussd.getDcs());
      }
      return;
    }
    if (response instanceof RingUnsolicited) {
      log.info("Received RING");
      return;
    }
    log.info("Received unsolicited response: {} {}", response.getClass().getName(), response);

  }
}
