package com.github.pmoerenhout.jsmppmodem;

import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pmoerenhout.atcommander.api.InitException;
import com.github.pmoerenhout.atcommander.api.SerialException;
import com.github.pmoerenhout.atcommander.basic.exceptions.ResponseException;
import com.github.pmoerenhout.atcommander.basic.exceptions.TimeoutException;
import com.github.pmoerenhout.atcommander.module._3gpp.EtsiModem;
import com.github.pmoerenhout.atcommander.module._3gpp.RegistrationState;
import com.github.pmoerenhout.atcommander.module._3gpp.commands.NetworkRegistrationResponse;
import com.github.pmoerenhout.atcommander.module.v250.enums.OperatorSelectionMode;
import com.github.pmoerenhout.atcommander.module.v250.exceptions.RegistrationFailedException;

@Service
public class ModemService {

  private static final Logger LOG = LoggerFactory.getLogger(ModemService.class);

  private static final int REGISTRATION_TIMEOUT = 300;

  private List<Modem> modems;

  private String manufacturerIdentification;
  private String revisionIdentification;
  private String serialNumber;
  private String productSerialNumberIdentification;

  private String imsi;

  private RegistrationState registrationState;

  @Autowired
  public ModemService(final List<Modem> modems) {
    this.modems = modems;
    LOG.info("Found {} serial connection(s) configurations", modems.size());
  }

  public Modem getFirstModem() {
    return modems.get(0);
  }

  public void init() {
    modems.forEach(s -> {
      try {
        init(s);
        s.setInitialized(true);
      } catch (InitException e) {
        LOG.error("Modem {} at port {} could not be initialised", s.getId(), s.getPort(), s.getSpeed());
      }
    });
  }

  public void init(final Modem modem) throws InitException {
    final EtsiModem etsiModem = modem.get3gppModem();
    LOG.info("Initialize 3GPP modem (27.007)");
//    final EtsiModem modem = new EtsiModem(
//        new JsscSerial(port, speed, FLOWCONTROL_RTSCTS_IN | FLOWCONTROL_RTSCTS_OUT,
//            new UnsolicitedCallback()));
    try {
      etsiModem.init();
      //etsiModem.getSimpleCommand(new String(new byte[]{ (byte) 0x1a }));
      etsiModem.getAttention();
      manufacturerIdentification = etsiModem.getManufacturerIdentification();
      revisionIdentification = etsiModem.getRevisionIdentification();
      serialNumber = etsiModem.getSerialNumber();
      productSerialNumberIdentification = etsiModem.getProductSerialNumberIdentification();
      imsi = etsiModem.getInternationalMobileSubscriberIdentity();
      LOG.info("Manufacturer: {}", manufacturerIdentification);
      LOG.info("Revision identification: {}", revisionIdentification);
      LOG.info("Serial number: {}", serialNumber);
      LOG.info("Product serial number identification: {}", productSerialNumberIdentification);
      LOG.info("IMSI: {}", imsi);

      etsiModem.getSimpleCommand("AT#SELINT=2").set();
      etsiModem.getSimpleCommand("AT#SMSMODE=2").set();
      etsiModem.getSimpleCommand("AT+CMER=2").set();
      etsiModem.setMobileEquipmentError(2);
      etsiModem.setCellularResultCodes(true);
      etsiModem.setNetworkRegistration(2);
//      etsiModem.setNewMessageIndications(2, 3, 2, 2, 0);
      //etsiModem.setNewMessageIndications(1, 2, 0, 0, 0);
      etsiModem.setNewMessageIndications(1, 2, 0, 0, 0);
      // etsiModem.setNewMessageIndications(2,3 );
      // modem.setOperatorSelection(OperatorSelectionMode.AUTOMATIC);
      waitForAutomaticRegistration(etsiModem);
      LOG.info("Network registration state: {}", etsiModem.getNetworkRegistration().getRegistrationState());

      final int serviceForMoSmsMessages = etsiModem.getServiceForMoSmsMessages();
      final List<Integer> servicesForMoSmsMessages = etsiModem.getServicesForMoSmsMessages();
      LOG.info("Service for MO SMS message is {}, possible values are {}", serviceForMoSmsMessages, servicesForMoSmsMessages);

      final MessageService messageService = new MessageService(etsiModem);
      // messageService.setTextMessageMode();
      messageService.setPduMessageMode();
      // messageService.showCurrentTeCharacterSet();
      messageService.setIraTeCharacterSet();
      messageService.showAllMessages();

      modem.set3gppModem(etsiModem);
      modem.setMessageService(messageService);
    } catch (final Exception e) {
      throw new InitException("Could not initialise the modem", e);
    }
  }

  public void close() {
    modems.forEach(m -> {
      m.get3gppModem().close();
      LOG.info("3GPP modem {} is closed", m.getId());
    });
  }

  public void send(final Modem modem, final String destination, final int numberOfSms) {
    LOG.info("Send {} messages to {}", numberOfSms, destination);
    try {
      IntStream.rangeClosed(1, numberOfSms).forEach(i -> {
        try {
          modem.getMessageService().sendPduMessage(destination, i);
          Thread.sleep(500);
        } catch (Exception e) {
          LOG.info("Could not send short message", e);
        }
      });
    } catch (final Exception e) {
      LOG.error("The modem had an error", e);
    }
  }

  public void showAllMessages(final Modem modem) throws ResponseException, SerialException, TimeoutException {
    modem.getMessageService().showAllMessages();
  }

  public void sendAllMessagesViaSmpp(final Modem modem) throws ResponseException, SerialException, TimeoutException {
    modem.getMessageService().sendAllMessagesViaSmpp();
  }

  public void deleteAllMessage(final Modem modem) throws ResponseException, SerialException, TimeoutException {
    LOG.info("Delete all messages");
    modem.getMessageService().deleteAllMessages();
  }

  private void waitForAutomaticRegistration(
      final EtsiModem modem) throws SerialException, RegistrationFailedException, ResponseException, TimeoutException, InterruptedException {
    LOG.info("Waiting for registered network");
    // modem.setOperatorSelection(OperatorSelectionMode.AUTOMATIC);
    if (!waitForAutomaticNetworkRegistration(modem, REGISTRATION_TIMEOUT)) {
      //final int error = modem.getExtendedNumericErrorReport();
      //final int networkRejectError = getExtendedNumericErrorReportForNetworkReject();
      throw new RegistrationFailedException(String.format("No network registration in %d seconds", REGISTRATION_TIMEOUT));
    }
  }

  private boolean waitForAutomaticNetworkRegistration(final EtsiModem modem, final int seconds)
      throws SerialException, ResponseException, TimeoutException, InterruptedException {
    final int interval = 5;
    final int iterations = (seconds / interval) + (seconds % interval == 0 ? 0 : 1);
    registrationState = null;
    for (int i = 0; i < iterations; i++) {
      final NetworkRegistrationResponse response = modem.getNetworkRegistration();
      registrationState = response.getRegistrationState();
      if (isRegistered(registrationState)) {
        return true;
      }
      LOG.debug("Network Registration is {}, waiting...", registrationState);
      if (i == 0) {
        LOG.debug("Request automatic operator selection");
        modem.setOperatorSelection(OperatorSelectionMode.AUTOMATIC);
      }
      registrationState = modem.getNetworkRegistration().getRegistrationState();
      Thread.sleep(interval * 1000);
    }
    return false;
  }

  private boolean isRegistered(final RegistrationState state) {
    if (state == RegistrationState.REGISTERED_HOME_NETWORK || state == RegistrationState.REGISTERED_ROAMING) {
      return true;
    }
    return false;
  }

}
