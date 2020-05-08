package com.github.pmoerenhout.jsmppmodem;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.github.pmoerenhout.atcommander.api.InitException;
import com.github.pmoerenhout.atcommander.api.SerialException;
import com.github.pmoerenhout.atcommander.basic.exceptions.ResponseException;
import com.github.pmoerenhout.atcommander.basic.exceptions.TimeoutException;
import com.github.pmoerenhout.atcommander.module._3gpp.EtsiModem;
import com.github.pmoerenhout.atcommander.module._3gpp.RegistrationState;
import com.github.pmoerenhout.atcommander.module._3gpp.commands.NetworkRegistrationResponse;
import com.github.pmoerenhout.atcommander.module._3gpp.types.IndexPduMessage;
import com.github.pmoerenhout.atcommander.module.v250.enums.OperatorSelectionMode;
import com.github.pmoerenhout.atcommander.module.v250.enums.PinStatus;
import com.github.pmoerenhout.atcommander.module.v250.exceptions.RegistrationFailedException;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedMessageIndicationEvent;
import com.github.pmoerenhout.jsmppmodem.util.Util;
import com.github.pmoerenhout.pduutils.gsm0340.Pdu;
import com.github.pmoerenhout.pduutils.gsm0340.PduParser;

@Service
public class ModemService {

  private static final Logger log = LoggerFactory.getLogger(ModemService.class);
  private static final int REGISTRATION_TIMEOUT = 300;
  private CountDownLatch latch = new CountDownLatch(1);
  private List<Modem> modems;

  private String manufacturerIdentification;
  private String revisionIdentification;
  private String serialNumber;
  private String productSerialNumberIdentification;

  private String imsi;

  private RegistrationState registrationState;

  private StorageService storageService;

  @Autowired
  public ModemService(final List<Modem> modems, final StorageService storageService) {
    this.modems = modems;
    this.storageService = storageService;
    log.info("Found {} serial connection(s) configurations", modems.size());
  }

  public Modem getFirstModem() {
    return modems.get(0);
  }

  public void init() {
    log.info("Initialize all modems");
    modems.forEach(s -> {
      try {
        init(s);
        s.setInitialized(true);
      } catch (InitException e) {
        log.error("Modem {} at port {} speed {} could not be initialised: {}", s.getId(), s.getPort(), s.getSpeed(), e.getMessage());
      }
    });
    latch.countDown();
  }

  public boolean waitForInitialisation(final long millies) throws InterruptedException {
    return latch.await(millies, TimeUnit.MILLISECONDS);
  }

  public void init(final Modem modem) throws InitException {
    final EtsiModem etsiModem = modem.get3gppModem();

    log.info("Initialize 3GPP modem (27.007)");
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
      etsiModem.setMobileEquipmentError(2);
      etsiModem.setCellularResultCodes(true);
      etsiModem.getSimpleCommand("AT+CMER=2").set();

      final PinStatus pinStatus = etsiModem.getPinStatus();
      switch (pinStatus) {
        case SIM_PIN:
          etsiModem.setPin("0000");
          break;
      }

      imsi = etsiModem.getInternationalMobileSubscriberIdentity();
      log.info("Manufacturer: {}", manufacturerIdentification);
      log.info("Revision identification: {}", revisionIdentification);
      log.info("Serial number: {}", serialNumber);
      log.info("Product serial number identification: {}", productSerialNumberIdentification);
      log.info("IMSI: {}", imsi);

      // etsiModem.getSimpleCommand("AT#SELINT=2").set();
      // etsiModem.getSimpleCommand("AT#SMSMODE=2").set();
      etsiModem.setNetworkRegistration(2);
//      etsiModem.setNewMessageIndications(2, 3, 2, 2, 0);
      //etsiModem.setNewMessageIndications(1, 2, 0, 0, 0);
      etsiModem.setNewMessageIndications(1, 2, 0, 0, 0);
      // USSD enable the result code presentation to the TE
      etsiModem.setUssd(1);
      // etsiModem.setNewMessageIndications(2,3 );
      // modem.setOperatorSelection(OperatorSelectionMode.AUTOMATIC);
      waitForAutomaticRegistration(etsiModem);
      log.info("Network registration state: {}", etsiModem.getNetworkRegistration().getRegistrationState());

      final int serviceForMoSmsMessages = etsiModem.getServiceForMoSmsMessages();
      final List<Integer> servicesForMoSmsMessages = etsiModem.getServicesForMoSmsMessages();
      log.info("Service for MO SMS message is {}, possible values are {}", serviceForMoSmsMessages, servicesForMoSmsMessages);

      final MessageService messageService = new MessageService(etsiModem);
      // messageService.setTextMessageMode();
      messageService.setPduMessageMode();
      // messageService.showCurrentTeCharacterSet();
      messageService.setIraTeCharacterSet();
      messageService.showAllMessages();

      modem.set3gppModem(etsiModem);
      modem.setMessageService(messageService);
    } catch (final Exception e) {
      log.error("Init error", e);
      throw new InitException("Could not initialise the modem", e);
    }
  }

  public void close() {
    modems.forEach(m -> {
      m.get3gppModem().close();
      log.info("3GPP modem {} is closed", m.getId());
    });
  }

  @EventListener
  public void handleReceivedMTEvent(final ReceivedMessageIndicationEvent event) throws Exception {
    log.debug("Received MT: {} {}", event.getStorage(), event.getStorage());
    final String connectionId = event.getConnectionId();
    final Modem modem = modems.stream().filter(m -> m.getId() == connectionId).findFirst().orElseThrow(() -> new RuntimeException("modem not found"));
    modem.getMessageService().readSms(event.getConnectionId(), event.getIndex());
  }

  public void send(final Modem modem, final String destination, final int numberOfSms) {
    log.info("Send {} messages to {}", numberOfSms, destination);
    try {
      IntStream.rangeClosed(1, numberOfSms).forEach(i -> {
        try {
          modem.getMessageService().sendPduMessage(destination, i);
          Thread.sleep(500);
        } catch (Exception e) {
          log.info("Could not send short message", e);
        }
      });
    } catch (final Exception e) {
      log.error("The modem had an error", e);
    }
  }

  public void sendBinary(final Modem modem, final List<String> destinations) {
    log.info("Send 1 binary message to {} destinations", destinations.size());
    try {
      destinations.forEach(d -> {
        try {
          final byte[] data = Util.hexToByteArray("08411181BC3048320035A001AD800D1454EDFD95C0");
          modem.getMessageService().sendBinaryPduMessage(d, data);
          Thread.sleep(500);
        } catch (Exception e) {
          log.info("Could not send short message", e);
        }
      });
    } catch (final Exception e) {
      log.error("The modem had an error", e);
    }
  }

  public void showAllMessages(final Modem modem) throws ResponseException, SerialException, TimeoutException {
    modem.getMessageService().showAllMessages();
  }

  public void sendAllMessagesViaSmpp(final Modem modem) throws ResponseException, SerialException, TimeoutException {
    modem.getMessageService().sendAllMessagesViaSmpp(modem.getId());
  }

  public void storeAllMessages(final Modem modem) throws ResponseException, SerialException, TimeoutException {
    final List<IndexPduMessage> messages = modem.getMessageService().getAllMessages();
    messages.forEach(m -> {
      final PduParser pduParser = new PduParser();
      final Pdu pdu = pduParser.parsePdu(m.getPdu());
      log.info("SMSC:{} ADDRESS:{} DCS:{}/{} TEXT:'{}'", pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(), pdu.getProtocolIdentifier(),
          pdu.getDecodedText());
    });
    messages.forEach(m ->
        storageService.save(Instant.now().toEpochMilli(), modem.getId(), Util.hexToByteArray(m.getPdu()))
    );
  }


  public void deleteAllMessage(final Modem modem) throws ResponseException, SerialException, TimeoutException {
    log.info("Delete all messages");
    modem.getMessageService().deleteAllMessages();
  }

  private void waitForAutomaticRegistration(
      final EtsiModem modem) throws SerialException, RegistrationFailedException, ResponseException, TimeoutException, InterruptedException {
    log.info("Waiting for registered network");
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
      log.debug("Network Registration is {}, waiting...", registrationState);
      if (i == 0) {
        log.debug("Request automatic operator selection");
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
