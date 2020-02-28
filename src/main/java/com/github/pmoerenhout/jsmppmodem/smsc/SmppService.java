package com.github.pmoerenhout.jsmppmodem.smsc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.ajwcc.pduutils.gsm0340.Pdu;
import org.ajwcc.pduutils.gsm0340.SmsDeliveryPdu;
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
import org.jsmpp.session.ServerSession;
import org.jsmpp.session.SessionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pmoerenhout.jsmppmodem.StorageService;
import com.github.pmoerenhout.jsmppmodem.events.BoundReceiverEvent;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedPduEvent;
import com.github.pmoerenhout.jsmppmodem.service.PduService;
import com.github.pmoerenhout.jsmppmodem.util.Util;

@Service
public class SmppService {

  private static final Logger LOG = LoggerFactory.getLogger(SmppService.class);
  private final List<SMPPServerSession> sessions;
  private final TaskExecutor taskExecutor;
  private SMPPServerSessionListener sessionListener = null;
  private SmppConfiguration configuration;
  private ServerMessageReceiverListener serverMessageReceiverListener;
  private ServerResponseDeliveryListener serverResponseDeliveryListener;
  private SessionStateListener sessionStateListener;
  private SmppSmsTranscoding smppSmsTranscoding;
  private StorageService storageService;

  private String address;

  private boolean running;

  @Autowired
  public SmppService(
      @Qualifier("smppConfiguration") final SmppConfiguration smppConfiguration,
      @Qualifier("serverMessageReceiverListener") final ServerMessageReceiverListenerImpl serverMessageReceiverListener,
      @Qualifier("serverResponseDeliveryListener") final ServerResponseDeliveryListenerImpl serverResponseDeliveryListener,
      @Qualifier("sessionStateListener") final SessionStateListenerImpl sessionStateListener,
      @Qualifier("sessions") final List<SMPPServerSession> sessions,
      @Qualifier("smppTaskExecutor") final TaskExecutor taskExecutor,
      @Qualifier("smppSmsTranscoding") final SmppSmsTranscoding smppSmsTranscoding,
      @Qualifier("storageService") final StorageService storageService
  ) {
    this.configuration = smppConfiguration;
    this.serverMessageReceiverListener = serverMessageReceiverListener;
    this.serverResponseDeliveryListener = serverResponseDeliveryListener;
    this.sessionStateListener = sessionStateListener;
    this.sessions = sessions;
    this.taskExecutor = taskExecutor;
    this.smppSmsTranscoding = smppSmsTranscoding;
    this.storageService = storageService;

    this.address = configuration.getAddress();
    LOG.info("SMPP service created");
  }

  @Async
  public void start() throws InterruptedException {
    final int port = configuration.getPort();
    final int transactionTimer = configuration.getTransactionTimer();
    LOG.info("SMPP service started on port {}", port);
    try {
      running = true;
      sessionListener = new SMPPServerSessionListener(port);
      // start the listener
      LOG.info("Listening on port {}", port);
      while (running) {
        LOG.info("Waiting for new connection...");
        final SMPPServerSession serverSession = sessionListener.accept();
        sessions.add(serverSession);
        serverSession.addSessionStateListener(sessionStateListener);
        LOG.info("Accepting connection for session {} with transaction timeout {}", serverSession.getSessionId(), transactionTimer);
        serverSession.setMessageReceiverListener(serverMessageReceiverListener);
        serverSession.setResponseDeliveryListener(serverResponseDeliveryListener);
        serverSession.setTransactionTimer(transactionTimer);
        taskExecutor.execute(new WaitBindTask(serverSession, configuration.getBindTimeout(), configuration.getEnquireLinkTimer()));
      }
      LOG.info("The SMPP server will be stopped");
    } catch (final SocketException e) {
      LOG.info("Socket port {}: {}", port, e.getMessage());
    } catch (final IOException e) {
      LOG.error("Error during listener on port " + port, e);
    } finally {
      try {
        if (sessionListener != null) {
          LOG.info("Finally close listener port {}", sessionListener.getPort());
          sessionListener.close();
        }
      } catch (final IOException e) {
        LOG.error("Could not close listener", e);
      }
    }

    closeAllSessions();

    // Wait for connected sessions to be closed, then terminate
    while (sessions.size() > 0) {
      LOG.info("Wait for {} sessions to be closed", sessions.size());
      // Wait for existing sessions to be closed
      Thread.sleep(50);
    }
    LOG.info("The SMPP service is stopped");
  }

  private void closeAllSessions() {
    // to prevent concurrent access to list in same thread
    final List<SMPPServerSession> sessionsToClose = new ArrayList<>();
    sessions.forEach(s -> sessionsToClose.add(s));
    sessionsToClose.forEach(s -> s.close());
  }

  @EventListener
  public void handleReceivedPduEvent(final ReceivedPduEvent event) throws Exception {
    LOG.info("handleReceivedPduEvent: {}", event.getPdu());
    final DeliverSm deliverSm = getDeliverSm(event.getPdu());
    LOG.info("deliverSm: {}", deliverSm);
    final int delivered = deliver(deliverSm);
    if (delivered == 0) {
      storageService.save(event.getTimestamp(), event.getPdu());
    }
  }

  @Transactional
  @EventListener
  @Async
  public void handleBoundReceiverEvent(final BoundReceiverEvent event) throws Exception {
    LOG.info("handleBoundReceiverEvent");
    storageService.streamAll().forEach(p -> {
      try {
        final DeliverSm deliverSm = getDeliverSm(p.getPdu());
        LOG.info("deliverSm: {}", deliverSm);
        deliver(deliverSm);
      } catch (UnsupportedEncodingException e) {
        LOG.error("Could not send PDU", e);
      }
    });
  }

  @Transactional
  @Async
  public void triggerBoundReceiver() {
    storageService.streamAll().forEach(p -> {
      try {
        final DeliverSm deliverSm = getDeliverSm(p.getPdu());
        LOG.info("deliverSm: {}", deliverSm);
        deliver(deliverSm);
      } catch (UnsupportedEncodingException e) {
        LOG.error("Could not send PDU", e);
      }
    });
  }

  @Transactional
  @Scheduled(initialDelay = 15000, fixedRate = 150000)
  public void sendScheduled() {
    storageService.streamAll().forEach(p -> {
      try {
        final DeliverSm deliverSm = getDeliverSm(p.getPdu());
        LOG.info("deliverSm: {}", deliverSm);
        deliver(deliverSm);
        // applicationEventPublisher.publishEvent(new ReceivedPduEvent(this, p.getPdu())));
      } catch (UnsupportedEncodingException e) {
        LOG.error("Could not send PDU", e);
      }
    });
  }

  // SMPP with Euro
  // 07911356049938002412D1CBA7B408028082C2210000817062319183800F54747A0E4ACF41C5AAF409DA9401
  // 07911346101919F9400B911316240486F90008817062611544808C05000319040103E903E903E9029B029B029B029B03E903E903E903E9029B0020005700610072006400200068006500740020007700650072006B00740021002000400041004200430031003200330034007B007D005B005D00E9006F03E903E903E9029B029B029B029B03E903E903E903E9029B002000570061007200640020006800650074002000770065
  @Transactional
  @Scheduled(fixedRate = 60000)
  public void sendForTesting() throws Exception {
    try {
      final DeliverSm deliverSm = getDeliverSm(Util.hexToByteArray(
          "07911346101919F9040B911316240486F90008817052614190808000480065006C006C006F00200057006F0072006C006400210020005400680069007300200069007300200061002000730068006F007200740020006D006500730073006100670065002E0020004C0065007420190073002000730065006500200077006800610074002000740068006500200050004400550020006900732026"));
      //final DeliverSm deliverSm = getDeliverSm(Util.hexToByteArray("00480065006C006C006F00200057006F0072006C006400210020005400680069007300200069007300200061002000730068006F007200740020006D006500730073006100670065002E0020004C0065007420190073002000730065006500200077006800610074002000740068006500200050004400550020006900732026"));
      LOG.info("deliverSm: {}", deliverSm);
      deliver(deliverSm);
    } catch (UnsupportedEncodingException e) {
      LOG.error("Could not send PDU", e);
    }
    Thread.sleep(10000);
    try {
      final DeliverSm deliverSm = getDeliverSm(Util.hexToByteArray(
          "07911346101919F9400B911316240486F90008817062611544808C05000319040103E903E903E9029B029B029B029B03E903E903E903E9029B0020005700610072006400200068006500740020007700650072006B00740021002000400041004200430031003200330034007B007D005B005D00E9006F03E903E903E9029B029B029B029B03E903E903E903E9029B002000570061007200640020006800650074002000770065"));
      //final DeliverSm deliverSm = getDeliverSm(Util.hexToByteArray("00480065006C006C006F00200057006F0072006C006400210020005400680069007300200069007300200061002000730068006F007200740020006D006500730073006100670065002E0020004C0065007420190073002000730065006500200077006800610074002000740068006500200050004400550020006900732026"));
      LOG.info("deliverSm: {}", deliverSm);
      deliver(deliverSm);
    } catch (UnsupportedEncodingException e) {
      LOG.error("Could not send PDU", e);
    }
    Thread.sleep(10000);
    try {
      final DeliverSm deliverSm = getDeliverSm(Util.hexToByteArray(
          "07911346101919F9400B911316240486F90008817062611554808C0500031904020072006B00740021002000400041004200430031003200330034007B007D005B005D00E9006F03E903E903E9029B029B029B029B03E903E903E903E9029B0020005700610072006400200068006500740020007700650072006B00740021002000400041004200430031003200330034007B007D005B005D00E9006F03E903E903E9029B029B"));
      //final DeliverSm deliverSm = getDeliverSm(Util.hexToByteArray("00480065006C006C006F00200057006F0072006C006400210020005400680069007300200069007300200061002000730068006F007200740020006D006500730073006100670065002E0020004C0065007420190073002000730065006500200077006800610074002000740068006500200050004400550020006900732026"));
      LOG.info("deliverSm: {}", deliverSm);
      deliver(deliverSm);
    } catch (UnsupportedEncodingException e) {
      LOG.error("Could not send PDU", e);
    }
  }

  public void stop() {
    LOG.info("Set running to false and close listener");
    running = false;
    // Close the listener will interrupt the accept()
    try {
      LOG.info("Close listener");
      sessionListener.close();
    } catch (final IOException e) {
      LOG.error("Could not close listener", e);
    }
    sessionListener = null;
  }

  private DeliverSm getDeliverSm(final byte[] pduBytes) throws UnsupportedEncodingException {
    final Pdu pdu = PduService.decode(pduBytes);
    if (pdu instanceof SmsDeliveryPdu) {
      return getDeliverSm((SmsDeliveryPdu) pdu, pduBytes);
    }
    throw new IllegalArgumentException("The PDU bytes are no an SMS-DELIVER PDU");
  }

  private DeliverSm getDeliverSm(final SmsDeliveryPdu pdu, final byte[] bytes) throws UnsupportedEncodingException {
    final DataCodedMessage dataCodedMessage = smppSmsTranscoding.toSmpp(pdu.getUDData(), (byte) (pdu.getDataCodingScheme() & (byte) 0xff));

    LOG.info("UD  {} -> {}", Util.bytesToHexString(pdu.getUDData()), Util.bytesToHexString(dataCodedMessage.getMessage()));
    LOG.info("DCS {} -> {}", String.format("%02X", pdu.getDataCodingScheme()), String.format("%02X", dataCodedMessage.getCodingScheme()));

    final DeliverSm deliverSm = new DeliverSm();
    deliverSm.setServiceType(configuration.getServiceType());
    deliverSm.setSourceAddr(pdu.getAddress());
    deliverSm.setSourceAddrTon(getTypeOfNumber(pdu.getAddressType()));
    deliverSm.setSourceAddrNpi(getNumberPlanIndicator(pdu.getAddressType()));
    deliverSm.setDestAddress(address);
    deliverSm.setDestAddrTon(TypeOfNumber.INTERNATIONAL.value());
    deliverSm.setDestAddrNpi(NumberingPlanIndicator.ISDN.value());
    deliverSm.setDataCoding((byte) (pdu.getDataCodingScheme() & (byte) 0xff));
    deliverSm.setShortMessage(pdu.getUDData());

    final List<OptionalParameter> optionalParameters = new ArrayList<>();
    optionalParameters.add(new OptionalParameter.More_messages_to_send(new byte[]{ pdu.hasTpMms() ? (byte) 0x01 : (byte) 0x00 }));

    // Add the complete PDU
    optionalParameters.add(new OptionalParameter.OctetString((short) 8200, bytes));

    final String smscAddress = pdu.getSmscAddress();
    if (smscAddress != null) {
      optionalParameters.add(new OptionalParameter.OctetString((short) 8193, pdu.getSmscAddress(), "US-ASCII"));
      optionalParameters.add(new OptionalParameter.Byte((short) 8194, getTypeOfNumber(pdu.getSmscAddressType())));
      optionalParameters.add(new OptionalParameter.Byte((short) 8195, getNumberPlanIndicator(pdu.getSmscAddressType())));
    }
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

  public int deliver(final DeliverSm deliverSm) {
    final AtomicInteger i = new AtomicInteger(0);
    sessions.forEach(s -> {
      if (deliver(s, deliverSm)) {
        i.incrementAndGet();
      }
    });
    final int delivered = i.get();
    LOG.info("The message was delivered to {} sessions", delivered);
    return delivered;
  }

  public boolean deliver(final ServerSession serverSession, final DeliverSm deliverSm) {
    try {
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
    } catch (PDUException | ResponseTimeoutException |
        InvalidResponseException | NegativeResponseException | IOException e) {
      LOG.warn("Could not send message", e);
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
        LOG.info("Wait for bind request (timeout {}) from session {}", timeout, serverSession.getSessionId());
        final BindRequest bindRequest = serverSession.waitForBind(timeout);
        LOG.info("Received bind for session {} with bindType:{} systemId:'{}' password:'{}' systemType:'{}'", serverSession.getSessionId(),
            bindRequest.getBindType(), bindRequest.getSystemId(), bindRequest.getPassword(), bindRequest.getSystemType());
        try {
          LOG.info("Received and accepting bind for session {}, interface version {}", serverSession.getSessionId(), bindRequest.getInterfaceVersion());
          bindRequest.accept("sys", InterfaceVersion.IF_34);
        } catch (PDUStringException e) {
          LOG.error("PDU string exception", e);
          bindRequest.reject(SMPPConstant.STAT_ESME_RSYSERR);
        }
        serverSession.setEnquireLinkTimer(enquireLinkTimer);
      } catch (final IllegalStateException e) {
        LOG.error("System error", e);
      } catch (final TimeoutException e) {
        LOG.warn("Wait for bind has reached timeout", e);
      } catch (final IOException e) {
        LOG.error("Failed accepting bind request for session", e);
      }
      LOG.info("WaitBindTask ended");
    }
  }

}