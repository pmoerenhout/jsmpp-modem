package com.github.pmoerenhout.jsmppmodem;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.github.pmoerenhout.atcommander.api.InitException;
import com.github.pmoerenhout.atcommander.api.SerialException;
import com.github.pmoerenhout.atcommander.basic.exceptions.ResponseException;
import com.github.pmoerenhout.atcommander.basic.exceptions.TimeoutException;
import com.github.pmoerenhout.atcommander.module._3gpp.EtsiModem;
import com.github.pmoerenhout.atcommander.module._3gpp.RegistrationState;
import com.github.pmoerenhout.atcommander.module._3gpp.commands.NetworkRegistrationResponse;
import com.github.pmoerenhout.atcommander.module._3gpp.exceptions.CmeErrorException;
import com.github.pmoerenhout.atcommander.module._3gpp.types.IndexPduMessage;
import com.github.pmoerenhout.atcommander.module._3gpp.types.PduMessage;
import com.github.pmoerenhout.atcommander.module.v250.enums.OperatorSelectionMode;
import com.github.pmoerenhout.atcommander.module.v250.enums.PinStatus;
import com.github.pmoerenhout.atcommander.module.v250.exceptions.RegistrationFailedException;
import com.github.pmoerenhout.jsmppmodem.events.MessageTerminatingIndicationEvent;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedPduEvent;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedSmsDeliveryPduEvent;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedSmsStatusReportPduEvent;
import com.github.pmoerenhout.jsmppmodem.util.Util;
import com.github.pmoerenhout.pduutils.gsm0340.Pdu;
import com.github.pmoerenhout.pduutils.gsm0340.PduParser;
import com.github.pmoerenhout.pduutils.gsm0340.SmsDeliveryPdu;
import com.github.pmoerenhout.pduutils.gsm0340.SmsStatusReportPdu;
import com.github.pmoerenhout.pduutils.gsm0340.SmsSubmitPdu;

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

  private ApplicationEventPublisher applicationEventPublisher;

  @Autowired
  public ModemService(final List<Modem> modems, final StorageService storageService, final ApplicationEventPublisher applicationEventPublisher) {
    this.modems = modems;
    this.storageService = storageService;
    this.applicationEventPublisher = applicationEventPublisher;
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
      // etsiModem.getI
      manufacturerIdentification = etsiModem.getManufacturerIdentification();

      final boolean isSonyEricsson = (manufacturerIdentification.contains("SONY ERICSSON"));

      revisionIdentification = etsiModem.getRevisionIdentification();
      if (!isSonyEricsson) {
        serialNumber = etsiModem.getSerialNumber();
      }
      productSerialNumberIdentification = etsiModem.getProductSerialNumberIdentification();
      etsiModem.setMobileEquipmentError(2);
      if (isSonyEricsson) {
        etsiModem.setCellularResultCodes(true);
      }

      log.info("Phone activity status: {}", etsiModem.getPhoneActivityStatus());

      log.info("Manufacturer: {}", manufacturerIdentification);
      log.info("Revision identification: {}", revisionIdentification);
      log.info("Serial number: {}", serialNumber);
      log.info("Product serial number identification: {}", productSerialNumberIdentification);

      log.info("Phone activity status: {}", etsiModem.getPhoneActivityStatus());

      final PinStatus pinStatus = etsiModem.getPinStatus();
      switch (pinStatus) {
        case SIM_PIN:
          etsiModem.setPin("0000");
          break;
      }

      log.info("Phone activity status: {}", etsiModem.getPhoneActivityStatus());

      if (isSonyEricsson) {
        // +CMER=[<mode>[,<keyp>[,<disp>[,<ind>[,<bfr>]]]]]
        // +CMER: (0,3),(0,2),0,(0-1),0
        etsiModem.getSimpleCommand("AT+CMER=3,0,0,0,0").set();
      } else {
        etsiModem.getSimpleCommand("AT+CMER=2").set();
      }

      while (StringUtils.isBlank(imsi)) {
        try {
          imsi = etsiModem.getInternationalMobileSubscriberIdentity();
          log.info("IMSI: {}", imsi);
        } catch (CmeErrorException e) {
          log.info("Error reading IMSI: {}", e.getMessage());
          Thread.sleep(500);
        }
      }

      log.info("Phone activity status: {}", etsiModem.getPhoneActivityStatus());

      // etsiModem.getSimpleCommand("AT#SELINT=2").set();
      // etsiModem.getSimpleCommand("AT#SMSMODE=2").set();

      if (isSonyEricsson) {
        etsiModem.setNetworkRegistration(1);
      } else {
        etsiModem.setNetworkRegistration(2);
      }
//      etsiModem.setNewMessageIndications(2, 3, 2, 2, 0);
      //etsiModem.setNewMessageIndications(1, 2, 0, 0, 0);

      if (isSonyEricsson) {
        // +CNMI: (2),(0,1,3),(0,2),(0),(0)
        // AT+CNMI=2,3,0,0,0
        etsiModem.setNewMessageIndications(2, 1, 0, 0, 0);
      } else {
        // Quectel: +CNMI: (0-2),(0-3),(0,2),(0-2),(0,1)
        etsiModem.setNewMessageIndications(2, 1, 2, 1, 0);
        // etsiModem.setNewMessageIndications(2, 2, 2, 1, 0);
      }
      // USSD enable the result code presentation to the TE
      etsiModem.setUssd(1);
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

      // TODO: Connection ID
      modems.forEach(m -> {
        try {
          messageService.sendAllMessagesViaSmpp(m.getId());
        } catch (Exception e) {
          log.error("Could not send all messages", e);
        }
      });

      modem.set3gppModem(etsiModem);
      modem.setMessageService(messageService);

    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
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

  @Async
  @EventListener
  public void handleReceivedMessageIndication(final MessageTerminatingIndicationEvent event) throws Exception {
    log.debug("Received MTI: {} {}", event.getStorage(), event.getIndex());
    final String connectionId = event.getConnectionId();
    final Modem modem = modems.stream().filter(m -> m.getId() == connectionId).findFirst().orElseThrow(() -> new RuntimeException("modem not found"));
    final PduMessage message = modem.getMessageService().readSms(event.getConnectionId(), event.getIndex());
    applicationEventPublisher.publishEvent(new ReceivedPduEvent(this, connectionId, Util.hexToByteArray(message.getPdu())));
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

  public void sendText(final Modem modem, final String destination, final String text) {
    log.info("Send text message to {}: {}", destination, text);
    try {
      modem.getMessageService().sendTextMessage(destination, text);
    } catch (Exception e) {
      log.info("Could not send short message", e);
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
//    messages.stream().findFirst().ifPresent(m -> {
//      final PduParser pduParser = new PduParser();
//      final Pdu pdu = pduParser.parsePdu(m.getPdu());
//      if (pdu instanceof SmsDeliveryPdu) {
//        log.debug("DELIVERY:{} SCTS:{} SMSC:{} ADDRESS:{} DCS:{}/{} TEXT:'{}'",
//            ((SmsDeliveryPdu) pdu).getServiceCentreTimestamp(),
//            pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(), pdu.getProtocolIdentifier(),
//            pdu.getDecodedText());
//      }
//    });
    messages.forEach(m -> {
      try {
        final PduParser pduParser = new PduParser();
        log.info("PDU: {}", m.getPdu());
        final Pdu pdu = pduParser.parsePdu(m.getPdu());
        if (pdu instanceof SmsDeliveryPdu) {
          log.debug("DELIVERY:{} SCTS:{} SMSC:{} ADDRESS:{} DCS:{}/{} TEXT:'{}'",
              ((SmsDeliveryPdu) pdu).getServiceCentreTimestamp(),
              pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(), pdu.getProtocolIdentifier(),
              pdu.getDecodedText());
        }
      } catch (Exception e) {
        log.error("Error in parsing PDU", e);
      }
    });
    messages.stream().findFirst().ifPresent(m ->
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

  @EventListener
  public void handleReceivedPdu(final ReceivedPduEvent event) throws Exception {
    final PduParser pduParser = new PduParser();
    log.info("PDU: {}", Util.bytesToHexString(event.getPdu()));
    final Pdu pdu = pduParser.parsePdu(Util.bytesToHexString(event.getPdu()));
    if (pdu instanceof SmsDeliveryPdu) {
      log.info("DELIVERY: SCTS:{} SMSC:{} ADDRESS:{} DCS:{} PID:{} TEXT:'{}'",
          ((SmsDeliveryPdu) pdu).getServiceCentreTimestamp(),
          pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(),
          pdu.getProtocolIdentifier(),
          pdu.getDecodedText());
      applicationEventPublisher.publishEvent(new ReceivedSmsDeliveryPduEvent(this, event.getConnectionId(), event.getPdu()));
    } else if (pdu instanceof SmsStatusReportPdu) {
      log.info("STATUS-REPORT: SMSC:{} ADDRESS:{} DCS:{} PID:{}", pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(),
          pdu.getProtocolIdentifier());
      applicationEventPublisher.publishEvent(new ReceivedSmsStatusReportPduEvent(this, event.getConnectionId(), event.getPdu()));
    } else if (pdu instanceof SmsSubmitPdu) {
      log.info("SUBMIT: SMSC:{} ADDRESS:{} DCS:{} PID:{} TEXT:'{}'", pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(),
          pdu.getProtocolIdentifier(),
          pdu.getDecodedText());
      log.info("SMS-SUBMIT is not handled");
    } else {
      throw new IllegalArgumentException("The class " + pdu.getClass().getName() + " is unknown");
    }
  }
}
