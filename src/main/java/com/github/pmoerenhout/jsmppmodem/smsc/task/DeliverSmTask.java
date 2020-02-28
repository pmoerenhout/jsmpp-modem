package com.github.pmoerenhout.jsmppmodem.smsc.task;

import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GSMSpecificFeature;
import org.jsmpp.bean.MessageMode;
import org.jsmpp.bean.MessageType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RawDataCoding;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SubmitMulti;
import org.jsmpp.bean.SubmitSm;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.SMPPServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pmoerenhout.jsmppmodem.util.Util;

public class DeliverSmTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(DeliverSmTask.class);

  private final SMPPServerSession session;
  private final long delayMillis;

  private final TypeOfNumber sourceAddrTon;
  private final NumberingPlanIndicator sourceAddrNpi;
  private final String sourceAddress;

  private final TypeOfNumber destAddrTon;
  private final NumberingPlanIndicator destAddrNpi;
  private final String destAddress;

  private final byte[] shortMessage;

  public DeliverSmTask(final SMPPServerSession session, final SubmitSm submitSm, final byte[] responsePacketData, final long delayMillis) {
    this.session = session;
    this.delayMillis = delayMillis;

    // reversing destination to source
    sourceAddrTon = TypeOfNumber.valueOf(submitSm.getDestAddrTon());
    sourceAddrNpi = NumberingPlanIndicator.valueOf(submitSm.getDestAddrNpi());
    sourceAddress = submitSm.getDestAddress();

    // reversing source to destination
    destAddrTon = TypeOfNumber.valueOf(submitSm.getSourceAddrTon());
    destAddrNpi = NumberingPlanIndicator.valueOf(submitSm.getSourceAddrNpi());
    destAddress = submitSm.getSourceAddr();

    if (submitSm.getDataCoding() == (byte) 0xf6 && submitSm.getProtocolId() == (byte) 0x7f) {
      // OTA
      // final PacketBuilder packetBuilder = new PacketBuilderImpl();
      // packetBuilder.buildResponsePacket()
      // TODO: send actual response
      LOG.info("Sending the reponse: {}", Util.bytesToHexString(responsePacketData));
      // Set binary response including the User Data Header (027100)
      shortMessage = responsePacketData;
    } else {
      // TEXT
      shortMessage = submitSm.getShortMessage();
    }
  }

  public DeliverSmTask(final SMPPServerSession session, final SubmitMulti submitMulti,
                       final long delayMillis) {
    this.session = session;
    this.delayMillis = delayMillis;

    // set to unknown and null, since it was submit_multi
    sourceAddrTon = TypeOfNumber.UNKNOWN;
    sourceAddrNpi = NumberingPlanIndicator.UNKNOWN;
    sourceAddress = null;

    // reversing source to destination
    destAddrTon = TypeOfNumber.valueOf(submitMulti.getSourceAddrTon());
    destAddrNpi = NumberingPlanIndicator.valueOf(submitMulti.getSourceAddrNpi());
    destAddress = submitMulti.getSourceAddr();

    shortMessage = submitMulti.getShortMessage();
  }

  public void run() {
    try {
      // simulate delay of SMS, delay some time
      Thread.sleep(delayMillis);

      final SessionState state = session.getSessionState();
      if (!state.isReceivable()) {
        LOG.debug("Not sending delivery_sm for destination {} since session state is {}", destAddress, state);
        return;
      }
      try {
        session.deliverShortMessage(
            "mc",
            sourceAddrTon, sourceAddrNpi, sourceAddress,
            destAddrTon, destAddrNpi, destAddress,
            new ESMClass(MessageMode.DEFAULT, MessageType.DEFAULT, GSMSpecificFeature.UDHI),
            (byte) 0x00,
            (byte) 0x00,
            new RegisteredDelivery(0),
            new RawDataCoding((byte) 0xf6),
            shortMessage);
        LOG.debug("Send deliver_sm to {}", destAddress);
      } catch (Exception e) {
        LOG.error("Failed sending deliver_sm to " + destAddress, e);
      }
    } catch (InterruptedException e) {
      LOG.error("The delivery_sm task was interrupted", e);
    }
  }
}