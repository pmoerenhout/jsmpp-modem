package com.github.pmoerenhout.jsmppmodem.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pmoerenhout.atcommander.api.UnsolicitedResponse;
import com.github.pmoerenhout.atcommander.api.UnsolicitedResponseCallback;
import com.github.pmoerenhout.atcommander.module._3gpp.commands.GprsNetworkRegistrationResponse;

public class UnsolicitedCallback implements UnsolicitedResponseCallback {

  private static final Logger LOG = LoggerFactory.getLogger(UnsolicitedCallback.class);

  public void unsolicited(final UnsolicitedResponse response){
    if (response instanceof GprsNetworkRegistrationResponse){
      GprsNetworkRegistrationResponse gprsNetworkRegistrationResponse = (GprsNetworkRegistrationResponse)response;
      LOG.info("Unsolicited GPRS: mode:{} state:{} lac:{} cid:{}",
          gprsNetworkRegistrationResponse.getMode(),
          gprsNetworkRegistrationResponse.getRegistrationState(),
          gprsNetworkRegistrationResponse.getLac(),
          gprsNetworkRegistrationResponse.getCellId());
      return;
    }
    LOG.info("Received unsolicited response: {}", response);
  }
}
