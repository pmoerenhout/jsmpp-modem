package com.github.pmoerenhout.jsmppmodem.smsc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.PDUStringException;
import org.jsmpp.SMPPConstant;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.InterfaceVersion;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.RawDataCoding;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.BindRequest;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.session.SMPPServerSessionListener;
import org.jsmpp.session.ServerMessageReceiverListener;
import org.jsmpp.session.ServerResponseDeliveryListener;
import org.jsmpp.session.SessionStateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pmoerenhout.jsmppmodem.SmppCustomOptionalParameters;
import com.github.pmoerenhout.jsmppmodem.StorageService;
import com.github.pmoerenhout.jsmppmodem.events.BoundReceiverEvent;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedSmsDeliveryPduEvent;
import com.github.pmoerenhout.jsmppmodem.service.PduService;
import com.github.pmoerenhout.jsmppmodem.util.Util;
import com.github.pmoerenhout.pduutils.gsm0340.Pdu;
import com.github.pmoerenhout.pduutils.gsm0340.SmsDeliveryPdu;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableAsync
@Service
public class SmppService {

  private final List<SMPPServerSession> sessions;
  private final TaskExecutor smppTaskExecutor;
  private SMPPServerSessionListener sessionListener = null;
  private SmppConfiguration configuration;
  private ServerMessageReceiverListener serverMessageReceiverListener;
  private ServerResponseDeliveryListener serverResponseDeliveryListener;
  private SessionStateListener sessionStateListener;
  private SmppSmsTranscoding smppSmsTranscoding;
  private StorageService storageService;

  private boolean running;

  @Autowired
  public SmppService(
      @Qualifier("smppConfiguration") final SmppConfiguration smppConfiguration,
      @Qualifier("serverMessageReceiverListener") final ServerMessageReceiverListenerImpl serverMessageReceiverListener,
      @Qualifier("serverResponseDeliveryListener") final ServerResponseDeliveryListenerImpl serverResponseDeliveryListener,
      @Qualifier("sessionStateListener") final SessionStateListenerImpl sessionStateListener,
      @Qualifier("sessions") final List<SMPPServerSession> sessions,
      @Qualifier("smppTaskExecutor") final TaskExecutor smppTaskExecutor,
      @Qualifier("smppSmsTranscoding") final SmppSmsTranscoding smppSmsTranscoding,
      @Qualifier("storageService") final StorageService storageService
  ) {
    this.configuration = smppConfiguration;
    this.serverMessageReceiverListener = serverMessageReceiverListener;
    this.serverResponseDeliveryListener = serverResponseDeliveryListener;
    this.sessionStateListener = sessionStateListener;
    this.sessions = sessions;
    this.smppTaskExecutor = smppTaskExecutor;
    this.smppSmsTranscoding = smppSmsTranscoding;
    this.storageService = storageService;
    log.info("SMPP service created");
  }

  @Async
  public void start() throws InterruptedException {
    final int port = configuration.getPort();
    final int transactionTimer = configuration.getTransactionTimer();
    log.info("SMPP service started on port {}", port);
    try {
      running = true;
      sessionListener = new SMPPServerSessionListener(port);
      // start the listener
      log.info("Listening on port {}", port);
      while (running) {
        log.info("Waiting for new connection...");
        final SMPPServerSession serverSession = sessionListener.accept();
        sessions.add(serverSession);
        serverSession.addSessionStateListener(sessionStateListener);
        log.info("Accepting connection from {}:{} for session {} with transaction timeout {}", serverSession.getInetAddress(), serverSession.getPort(),
            serverSession.getSessionId(), transactionTimer);
        serverSession.setMessageReceiverListener(serverMessageReceiverListener);
        serverSession.setResponseDeliveryListener(serverResponseDeliveryListener);
        serverSession.setTransactionTimer(transactionTimer);
        smppTaskExecutor.execute(new WaitBindTask(serverSession, configuration.getBindTimeout(), configuration.getEnquireLinkTimer()));
      }
      log.info("The SMPP server will be stopped");
    } catch (final SocketException e) {
      log.info("Socket port {}: {}", port, e.getMessage());
    } catch (final IOException e) {
      log.error("Error during listener on port " + port, e);
    } finally {
      try {
        if (sessionListener != null) {
          log.info("Finally close listener port {}", sessionListener.getPort());
          sessionListener.close();
        }
      } catch (final IOException e) {
        log.error("Could not close listener", e);
      }
    }

    closeAllSessions();

    // Wait for connected sessions to be closed, then terminate
    while (sessions.size() > 0) {
      log.info("Wait for {} sessions to be closed", sessions.size());
      // Wait for existing sessions to be closed
      Thread.sleep(50);
    }
    log.info("The SMPP service is stopped");
  }

  private void closeAllSessions() {
    // to prevent concurrent access to list in same thread
    final List<SMPPServerSession> sessionsToClose = new ArrayList<>();
    sessions.forEach(s -> sessionsToClose.add(s));
    sessionsToClose.forEach(s -> s.close());
  }

  @EventListener
  public void handleReceivedSmsDeliveryPduEvent(final ReceivedSmsDeliveryPduEvent event) throws Exception {
    log.info("handleReceivedSmsDeliveryPduEvent: {}", Util.bytesToHexString(event.getPdu()));
    final DeliverSm deliverSm = getDeliverSm(event.getConnectionId(), event.getSubscriberNumber(), event.getPdu());
    log.info("Deliver_sm:: src {}", deliverSm.getSourceAddr());
    log.info("Deliver_sm:: dst {}", deliverSm.getDestAddress());
    log.info("Send deliverSm to all sessions: {}", deliverSm);
    final int delivered = deliverAllSession(deliverSm);
    log.info("Delivered to {} session(s)", delivered);
    storageService.save(event.getTimestamp(), event.getConnectionId(), event.getPdu());
  }

  @Transactional(readOnly = true)
  @EventListener
  @Async
  public void handleBoundReceiverEvent(final BoundReceiverEvent event) throws Exception {
    log.info("Session {} is bound, send all messages", event.getServerSession().getSessionId());
    // Allow the ESME to handle the bind_resp and change the state from OPEN to BOUND
    storageService.streamAll().forEach(r -> {
      try {
        final DeliverSm deliverSm = getDeliverSm(r.getConnectionId(), "1234", r.getPdu());
        int retry = 3;
        while (retry-- > 0) {
          log.info("deliverSm: {} retry:{}", deliverSm, retry);
          if (deliver(event.getServerSession(), deliverSm)) {
            break;
          }
        }
      } catch (IllegalArgumentException | UnsupportedEncodingException e) {
        log.error("Could not send message", e);
        throw new IllegalStateException("SHOULD NOT HAPPEN!");
      }
    });
  }

  @Transactional
  @Async
  public void triggerBoundReceiver() {
    storageService.streamAll().forEach(r -> {
      try {
        final DeliverSm deliverSm = getDeliverSm(r.getConnectionId(), "1234", r.getPdu());
        log.info("deliverSm: {}", deliverSm);
        deliverAllSession(deliverSm);
      } catch (UnsupportedEncodingException e) {
        log.error("Could not send PDU", e);
      }
    });
  }

//  @Transactional
//  //@Scheduled(initialDelay = 15000, fixedRate = 150000)
//  public void sendScheduled() {
//    storageService.streamAll().forEach(p -> {
//      try {
//        final DeliverSm deliverSm = getDeliverSm(p.getPdu());
//        LOG.info("deliverSm: {}", deliverSm);
//        deliverAllSession(deliverSm);
//        // applicationEventPublisher.publishEvent(new ReceivedPduEvent(this, p.getPdu())));
//      } catch (UnsupportedEncodingException e) {
//        LOG.error("Could not send PDU", e);
//      }
//    });
//  }

  // SMPP with Euro
  @Transactional
  //  @Scheduled(fixedRate = 35000)
  public void sendForTesting() throws Exception {
    try {
      final DeliverSm deliverSm = getDeliverSm("test", "1234", Util.hexToByteArray(
          "07911346101919F9040B911316240486F90008817052614190808000480065006C006C006F00200057006F0072006C006400210020005400680069007300200069007300200061002000730068006F007200740020006D006500730073006100670065002E0020004C0065007420190073002000730065006500200077006800610074002000740068006500200050004400550020006900732026"));
      //final DeliverSm deliverSm = getDeliverSm(Util.hexToByteArray("00480065006C006C006F00200057006F0072006C006400210020005400680069007300200069007300200061002000730068006F007200740020006D006500730073006100670065002E0020004C0065007420190073002000730065006500200077006800610074002000740068006500200050004400550020006900732026"));
      log.info("deliverSm: {}", deliverSm);
      deliverAllSession(deliverSm);
    } catch (UnsupportedEncodingException e) {
      log.error("Could not send PDU", e);
    }
    Thread.sleep(10000);
    try {
      final DeliverSm deliverSm = getDeliverSm("test", "1234", Util.hexToByteArray(
          "07911346101919F9400B911316240486F90008817062611544808C05000319040103E903E903E9029B029B029B029B03E903E903E903E9029B0020005700610072006400200068006500740020007700650072006B00740021002000400041004200430031003200330034007B007D005B005D00E9006F03E903E903E9029B029B029B029B03E903E903E903E9029B002000570061007200640020006800650074002000770065"));
      //final DeliverSm deliverSm = getDeliverSm(Util.hexToByteArray("00480065006C006C006F00200057006F0072006C006400210020005400680069007300200069007300200061002000730068006F007200740020006D006500730073006100670065002E0020004C0065007420190073002000730065006500200077006800610074002000740068006500200050004400550020006900732026"));
      log.info("deliverSm: {}", deliverSm);
      deliverAllSession(deliverSm);
    } catch (UnsupportedEncodingException e) {
      log.error("Could not send PDU", e);
    }
    Thread.sleep(10000);
    try {
      final DeliverSm deliverSm = getDeliverSm("test", "1234", Util.hexToByteArray(
          "07911346101919F9400B911316240486F90008817062611554808C0500031904020072006B00740021002000400041004200430031003200330034007B007D005B005D00E9006F03E903E903E9029B029B029B029B03E903E903E903E9029B0020005700610072006400200068006500740020007700650072006B00740021002000400041004200430031003200330034007B007D005B005D00E9006F03E903E903E9029B029B"));
      //final DeliverSm deliverSm = getDeliverSm(Util.hexToByteArray("00480065006C006C006F00200057006F0072006C006400210020005400680069007300200069007300200061002000730068006F007200740020006D006500730073006100670065002E0020004C0065007420190073002000730065006500200077006800610074002000740068006500200050004400550020006900732026"));
      log.info("deliverSm: {}", deliverSm);
      deliverAllSession(deliverSm);
    } catch (UnsupportedEncodingException e) {
      log.error("Could not send PDU", e);
    }
  }

  public void stop() {
    log.info("Set running to false and close listener");
    running = false;
    // Close the listener will interrupt the accept()
    try {
      if (sessionListener != null) {
        log.info("Close listener");
        sessionListener.close();
        sessionListener = null;
      }
    } catch (final IOException e) {
      log.error("Could not close listener", e);
    }
  }

  private DeliverSm getDeliverSm(final String connectionId, final String destination, final byte[] pduBytes) throws UnsupportedEncodingException {
    final Pdu pdu = PduService.decode(pduBytes);
    if (pdu instanceof SmsDeliveryPdu) {
      return getDeliverSm(connectionId, destination, (SmsDeliveryPdu) pdu, pduBytes);
    }
    throw new IllegalArgumentException("The PDU bytes are not SMS-DELIVER PDU");
  }

  private DeliverSm getDeliverSm(final String connectionId, final String destinationAddress, final SmsDeliveryPdu pdu,
                                 final byte[] bytes) throws UnsupportedEncodingException {
    final DataCodedMessage dataCodedMessage = smppSmsTranscoding.toSmpp(pdu.getUDData(), (byte) (pdu.getDataCodingScheme() & (byte) 0xff));

    log.debug("UD  {} -> {}", Util.bytesToHexString(pdu.getUDData()), Util.bytesToHexString(dataCodedMessage.getMessage()));
    log.debug("DCS {} -> {}", String.format("%02X", pdu.getDataCodingScheme()), String.format("%02X", dataCodedMessage.getCodingScheme()));

    final DeliverSm deliverSm = new DeliverSm();
    deliverSm.setServiceType(configuration.getServiceType());
    deliverSm.setSourceAddr(pdu.getAddress());
    deliverSm.setSourceAddrTon(getTypeOfNumber(pdu.getAddressType()));
    deliverSm.setSourceAddrNpi(getNumberPlanIndicator(pdu.getAddressType()));
    deliverSm.setDestAddress(destinationAddress);
    deliverSm.setDestAddrTon(TypeOfNumber.INTERNATIONAL.value());
    deliverSm.setDestAddrNpi(NumberingPlanIndicator.ISDN.value());
    deliverSm.setDataCoding((byte) (pdu.getDataCodingScheme() & 0xff));
    deliverSm.setProtocolId((byte) (pdu.getProtocolIdentifier() & 0xff));
    deliverSm.setSmDefaultMsgId((byte) (pdu.getMpRefNo() & 0xff));
    deliverSm.setShortMessage(pdu.getUDData());

    final List<OptionalParameter> optionalParameters = new ArrayList<>();
    optionalParameters.add(new OptionalParameter.More_messages_to_send(new byte[]{ pdu.hasTpMms() ? (byte) 0x01 : (byte) 0x00 }));
    // SMS
    optionalParameters.add(new OptionalParameter.Source_bearer_type((byte) 0x01));
    // GSM
    optionalParameters.add(new OptionalParameter.Source_network_type((byte) 0x01));
    // Protocol ID
    optionalParameters.add(new OptionalParameter.Source_telematics_id((byte) pdu.getProtocolIdentifier()));
    // optionalParameters.add(new OptionalParameter.((byte)0x01));

    // Add the connectId
    optionalParameters.add(new OptionalParameter.OctetString(SmppCustomOptionalParameters.OPTIONAL_TAG_CONNECTION_ID, connectionId));
    // Add the complete PDU
    optionalParameters.add(new OptionalParameter.OctetString(SmppCustomOptionalParameters.OPTIONAL_TAG_PDU, bytes));

    // Add the SMSC ADDRESS
    final String smscAddress = pdu.getSmscAddress();
    if (smscAddress != null) {
      optionalParameters.add(new OptionalParameter.OctetString(SmppCustomOptionalParameters.OPTIONAL_TAG_SMSC_ADDRESS, pdu.getSmscAddress(), "US-ASCII"));
      optionalParameters.add(new OptionalParameter.Byte(SmppCustomOptionalParameters.OPTIONAL_TAG_SMSC_ADDRESS_TON, getTypeOfNumber(pdu.getSmscAddressType())));
      optionalParameters
          .add(new OptionalParameter.Byte(SmppCustomOptionalParameters.OPTIONAL_TAG_SMSC_ADDRESS_NPI, getNumberPlanIndicator(pdu.getSmscAddressType())));
    }
    optionalParameters.add(new OptionalParameter.OctetString(SmppCustomOptionalParameters.OPTIONAL_TAG_SERVICE_CENTRE_TIMESTAMP,
        pdu.getServiceCentreTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
    deliverSm.setOptionalParameters(optionalParameters.toArray(new OptionalParameter[optionalParameters.size()]));
    deliverSm.setEsmClass((byte) 0x00);
    if (pdu.hasTpUdhi()) {
      deliverSm.setUdhi();
    }
    if (pdu.hasTpRp()) {
      deliverSm.setReplyPath();
    }
    return deliverSm;
  }

  private byte getTypeOfNumber(final int type) {
    return (byte) ((type & (byte) 0x70) >> 4);
  }

  private byte getNumberPlanIndicator(final int type) {
    return (byte) (type & (byte) 0x0f);
  }

  public int deliverAllSession(final DeliverSm deliverSm) {
    log.info("Deliver message to {} session(s)", sessions.size());
    final AtomicInteger i = new AtomicInteger(0);
    sessions.forEach(s -> {
      log.info("Deliver message to session {}", s.getSessionId());
      if (s.getSessionState().isBound()) {
        if (deliver(s, deliverSm)) {
          i.incrementAndGet();
        }
      } else {
        log.warn("Wrong state, not sending deliver_sm to session {} in state {}", s.getSessionId(), s.getSessionState());
      }
    });
    final int delivered = i.get();
    log.info("The message was delivered to {} session(s)", delivered);
    return delivered;
  }

  public boolean deliver(final SMPPServerSession serverSession, final DeliverSm deliverSm) {
    if (!serverSession.getSessionState().isTransmittable()){
      log.warn("Send deliver_sm on session {} in state {}, is not transmittable", serverSession.getSessionId(), serverSession.getSessionState());
      return false;
    }
    try {
      log.info("Send deliver_sm on session {} in state {}", serverSession.getSessionId(), serverSession.getSessionState());
      serverSession.deliverShortMessage(
          deliverSm.getServiceType(),
          TypeOfNumber.valueOf(deliverSm.getSourceAddrTon()),
          NumberingPlanIndicator.valueOf(deliverSm.getSourceAddrNpi()),
          deliverSm.getSourceAddr(),
          TypeOfNumber.valueOf(deliverSm.getDestAddrTon()),
          NumberingPlanIndicator.valueOf(deliverSm.getDestAddrNpi()), deliverSm.getDestAddress(),
          new ESMClass(deliverSm.getEsmClass()),
          deliverSm.getProtocolId(),
          deliverSm.getPriorityFlag(),
          new RegisteredDelivery(deliverSm.getRegisteredDelivery()),
          new RawDataCoding(deliverSm.getDataCoding()),
          deliverSm.getShortMessage(),
          deliverSm.getOptionalParameters()
      );
      return true;
    } catch (NegativeResponseException e) {
      log.warn("Deliver_sm on session {} not accepted: {}", serverSession.getSessionId(), e.getMessage());
      return false;
    } catch (ResponseTimeoutException | IOException e) {
      log.warn("Could not send deliver_sm on session " + serverSession.getSessionId() + ", close", e);
      serverSession.unbindAndClose();
      return false;
    } catch (PDUException | InvalidResponseException e) {
      log.warn("Could not send deliver_sm on session " + serverSession.getSessionId(), e);
      return false;
    }
  }

  private class WaitBindTask implements Runnable {
    private final SMPPServerSession serverSession;
    private final long timeout;
    private final int enquireLinkTimer;

    public WaitBindTask(final SMPPServerSession serverSession, final long timeout, final int enquireLinkTimer) {
      this.serverSession = serverSession;
      this.timeout = timeout;
      this.enquireLinkTimer = enquireLinkTimer;
    }

    public void run() {
      try {
        log.info("Wait for bind request (timeout {}) from session {}", timeout, serverSession.getSessionId());
        final BindRequest bindRequest = serverSession.waitForBind(timeout);
        log.info("Received bind for session {} with bindType:{} systemId:'{}' password:'{}' systemType:'{}'", serverSession.getSessionId(),
            bindRequest.getBindType(), bindRequest.getSystemId(), bindRequest.getPassword(), bindRequest.getSystemType());
        try {
          log.info("Received and accepting bind for session {}, interface version {}", serverSession.getSessionId(), bindRequest.getInterfaceVersion());
          bindRequest.accept("sys", InterfaceVersion.IF_34);
        } catch (PDUStringException e) {
          log.error("PDU string exception", e);
          bindRequest.reject(SMPPConstant.STAT_ESME_RSYSERR);
        }
        log.info("Set enquireLink time to {}ms", enquireLinkTimer);
        serverSession.setEnquireLinkTimer(enquireLinkTimer);
      } catch (final IllegalStateException e) {
        log.error("System error", e);
      } catch (final TimeoutException e) {
        log.warn("Wait for bind has reached timeout", e);
      } catch (final IOException e) {
        log.error("Failed accepting bind request for session", e);
      }
    }
  }

}
