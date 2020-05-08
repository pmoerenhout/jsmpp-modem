package com.github.pmoerenhout.jsmppmodem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pmoerenhout.atcommander.api.UnsolicitedResponse;
import com.github.pmoerenhout.atcommander.api.UnsolicitedResponseCallback;
import com.github.pmoerenhout.atcommander.module._3gpp.commands.NetworkRegistrationResponse;
import com.github.pmoerenhout.atcommander.module._3gpp.unsolicited.GprsNetworkRegistrationUnsolicited;
import com.github.pmoerenhout.atcommander.module._3gpp.unsolicited.MessageTerminatingIndicationUnsolicited;
import com.github.pmoerenhout.atcommander.module._3gpp.unsolicited.MessageTerminatingUnsolicited;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedMessageIndicationEvent;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedPduEvent;
import com.github.pmoerenhout.jsmppmodem.util.Util;

public class UnsolicitedCallback implements UnsolicitedResponseCallback {

  private static final Logger LOG = LoggerFactory.getLogger(UnsolicitedCallback.class);

  private String id;

  public UnsolicitedCallback(final String id) {
    this.id = id;
  }

  public void unsolicited(final UnsolicitedResponse response) {
    if (response instanceof NetworkRegistrationResponse) {
      final NetworkRegistrationResponse networkRegistrationResponse = (NetworkRegistrationResponse) response;
      LOG.info("[{}] Unsolicited Network Registration: mode:{} state:{} lac:{} cid:{}",
          id,
          networkRegistrationResponse.getMode(),
          networkRegistrationResponse.getRegistrationState(),
          networkRegistrationResponse.getLac(),
          networkRegistrationResponse.getCellId());
      return;
    } else if (response instanceof GprsNetworkRegistrationUnsolicited) {
      final GprsNetworkRegistrationUnsolicited gprsNetworkRegistration = (GprsNetworkRegistrationUnsolicited) response;
      LOG.info("[{}] Unsolicited GPRS: state:{} lac:{} cid:{}",
          id,
          gprsNetworkRegistration.getRegistrationState(),
          gprsNetworkRegistration.getLac(),
          gprsNetworkRegistration.getCellId());
      return;
    } else if (response instanceof MessageTerminatingUnsolicited) {
      final MessageTerminatingUnsolicited messageTerminating = (MessageTerminatingUnsolicited) response;
      LOG.info("[{}] Unsolicited Message Terminating: alpha:{} length:{} pdu:{} ({} bytes)",
          id,
          messageTerminating.getAlpha(),
          messageTerminating.getLength(),
          messageTerminating.getPdu(),
          messageTerminating.getPdu().length());
      final byte[] pdu = Util.hexToByteArray(messageTerminating.getPdu());
      ApplicationContextProvider.getApplicationContext().publishEvent(new ReceivedPduEvent(this, id, pdu));
      return;
    } else if (response instanceof MessageTerminatingIndicationUnsolicited) {
      final MessageTerminatingIndicationUnsolicited mti = (MessageTerminatingIndicationUnsolicited) response;
      LOG.info("[{}] Unsolicited Message Terminating Indication: storage:{} index:{})",
          id, mti.getStorage(), mti.getIndex());
      ApplicationContextProvider.getApplicationContext().publishEvent(new ReceivedMessageIndicationEvent(this, id, mti.getStorage(), mti.getIndex()));
      return;
    }
    LOG.warn("Received unsolicited response: {}", response);
  }
}
