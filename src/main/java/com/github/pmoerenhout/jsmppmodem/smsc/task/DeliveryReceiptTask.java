package com.github.pmoerenhout.jsmppmodem.smsc.task;

import java.nio.charset.Charset;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.jsmpp.bean.DataCodings;
import org.jsmpp.bean.DeliveryReceipt;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GSMSpecificFeature;
import org.jsmpp.bean.MessageMode;
import org.jsmpp.bean.MessageType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SubmitMulti;
import org.jsmpp.bean.SubmitSm;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.util.DeliveryReceiptState;
import org.jsmpp.util.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeliveryReceiptTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(DeliveryReceiptTask.class);

  private final SMPPServerSession session;
  private final MessageId messageId;
  private final Charset charset;
  private final long delayMillis;

  private final TypeOfNumber sourceAddrTon;
  private final NumberingPlanIndicator sourceAddrNpi;
  private final String sourceAddress;

  private final TypeOfNumber destAddrTon;
  private final NumberingPlanIndicator destAddrNpi;
  private final String destAddress;

  private final int totalSubmitted;
  private final int totalDelivered;

  private final byte[] shortMessage;

  public DeliveryReceiptTask(final SMPPServerSession session, final SubmitSm submitSm, final MessageId messageId, final Charset charset,
                             final long delayMillis) {
    this.session = session;
    this.messageId = messageId;
    this.charset = charset;
    this.delayMillis = delayMillis;

    // reversing destination to source
    sourceAddrTon = TypeOfNumber.valueOf(submitSm.getDestAddrTon());
    sourceAddrNpi = NumberingPlanIndicator.valueOf(submitSm.getDestAddrNpi());
    sourceAddress = submitSm.getDestAddress();

    // reversing source to destination
    destAddrTon = TypeOfNumber.valueOf(submitSm.getSourceAddrTon());
    destAddrNpi = NumberingPlanIndicator.valueOf(submitSm.getSourceAddrNpi());
    destAddress = submitSm.getSourceAddr();

    totalSubmitted = totalDelivered = 1;

    if (submitSm.getDataCoding() == (byte) 0xf6 && submitSm.getProtocolId() == (byte) 0x7f) {
      // OTA
      // shortMessage = Util.bytesToHexString(response).getBytes(StandardCharsets.US_ASCII);
      shortMessage = submitSm.getShortMessage();
    } else {
      // TEXT
      shortMessage = submitSm.getShortMessage();
    }
  }

  public DeliveryReceiptTask(final SMPPServerSession session, final SubmitMulti submitMulti, final MessageId messageId, final Charset charset,
                             final long delayMillis) {
    this.session = session;
    this.messageId = messageId;
    this.charset = charset;
    this.delayMillis = delayMillis;

    // set to unknown and null, since it was submit_multi
    sourceAddrTon = TypeOfNumber.UNKNOWN;
    sourceAddrNpi = NumberingPlanIndicator.UNKNOWN;
    sourceAddress = null;

    // reversing source to destination
    destAddrTon = TypeOfNumber.valueOf(submitMulti.getSourceAddrTon());
    destAddrNpi = NumberingPlanIndicator.valueOf(submitMulti.getSourceAddrNpi());
    destAddress = submitMulti.getSourceAddr();

    // distribution list assumed only contains single address
    totalSubmitted = totalDelivered = submitMulti.getDestAddresses().length;

    shortMessage = submitMulti.getShortMessage();
  }

  public void run() {
    try {
      // simulate delay of SMS, delay some time
      Thread.sleep(delayMillis);

      final SessionState state = session.getSessionState();
      if (!state.isReceivable()) {
        LOG.debug("Not sending delivery receipt for message id {} since session state is {}", messageId, state);
        return;
      }
      try {
        // final String stringValue = Integer.valueOf(messageId.getValue(), 16).toString();
        // final String stringValue = messageId.getValue().trim();
        // LOG.debug("MessageID: {} => {}", messageId.getValue(), stringValue);
        // TODO: charset in text message is default charset or hex
        final DeliveryReceiptState deliveryReceiptState = DeliveryReceiptState.DELIVRD;
        final DeliveryReceipt delRec = new DeliveryReceipt(messageId.getValue(), totalSubmitted, totalDelivered, new Date(), new Date(),
            deliveryReceiptState,
            "000", StringUtils.left(new String(shortMessage, charset), 20));
        final String deliveryReceiptAsString = delRec.toString();
        LOG.debug("Delivery receipt for message id {}: {}", messageId, deliveryReceiptAsString);
        final OptionalParameter[] optionalParameters = new OptionalParameter[]{
            new OptionalParameter.Message_state((byte) deliveryReceiptState.value()),
            new OptionalParameter.Receipted_message_id(messageId.getValue()),
        };
        session.deliverShortMessage(
            "mc",
            sourceAddrTon, sourceAddrNpi, sourceAddress,
            destAddrTon, destAddrNpi, destAddress,
            new ESMClass(MessageMode.DEFAULT, MessageType.SMSC_DEL_RECEIPT, GSMSpecificFeature.DEFAULT),
            (byte) 0,
            (byte) 0,
            new RegisteredDelivery(0),
            DataCodings.ZERO,
            deliveryReceiptAsString.getBytes(charset),
            optionalParameters);
        LOG.debug("Send delivery receipt for message id {}", messageId);
      } catch (Exception e) {
        LOG.error("Failed sending delivery_receipt for message id " + messageId, e);
      }
    } catch (InterruptedException e) {
      LOG.error("The delivery receipt task was interrupted", e);
      Thread.currentThread().interrupt();
    }
  }
}