package com.github.pmoerenhout.jsmppmodem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pmoerenhout.atcommander.api.SerialException;
import com.github.pmoerenhout.atcommander.basic.exceptions.ResponseException;
import com.github.pmoerenhout.atcommander.basic.exceptions.TimeoutException;
import com.github.pmoerenhout.atcommander.module._3gpp.EtsiModem;
import com.github.pmoerenhout.atcommander.module._3gpp.types.IndexMessage;
import com.github.pmoerenhout.atcommander.module._3gpp.types.IndexPduMessage;
import com.github.pmoerenhout.atcommander.module._3gpp.types.Message;
import com.github.pmoerenhout.atcommander.module._3gpp.types.PduMessage;
import com.github.pmoerenhout.atcommander.module.v250.enums.MessageMode;
import com.github.pmoerenhout.atcommander.module.v250.enums.MessageStatus;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedPduEvent;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedSmsDeliveryPduEvent;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedSmsStatusReportPduEvent;
import com.github.pmoerenhout.jsmppmodem.service.PduService;
import com.github.pmoerenhout.jsmppmodem.util.Util;
import com.github.pmoerenhout.pduutils.gsm0340.Pdu;
import com.github.pmoerenhout.pduutils.gsm0340.PduParser;
import com.github.pmoerenhout.pduutils.gsm0340.SmsDeliveryPdu;
import com.github.pmoerenhout.pduutils.gsm0340.SmsStatusReportPdu;
import com.github.pmoerenhout.pduutils.gsm0340.SmsSubmitPdu;
import com.github.pmoerenhout.pduutils.wappush.WapSiPdu;

public class MessageService {

  // https://en.wikipedia.org/wiki/GSM_03.40

  private static final Logger log = LoggerFactory.getLogger(MessageService.class);

  private EtsiModem modem;
  private MessageMode messageMode;
  private String teCharacterSet;

  public MessageService(final EtsiModem modem) {
    this.modem = modem;
  }

  public void setPduMessageMode() throws ResponseException, SerialException, TimeoutException {
    if (messageMode == null || messageMode != MessageMode.PDU) {
      modem.setMessageMode(MessageMode.PDU);
      messageMode = MessageMode.PDU;
    }
  }

  public void setTextMessageMode() throws ResponseException, SerialException, TimeoutException {
    if (messageMode == null || messageMode != MessageMode.TEXT) {
      modem.setMessageMode(MessageMode.TEXT);
      messageMode = MessageMode.TEXT;
    }
  }

  public void setIraTeCharacterSet() throws ResponseException, SerialException, TimeoutException {
    if (teCharacterSet != null && !teCharacterSet.equals("IRA")) {
      modem.setTeCharacterSet("IRA");
      teCharacterSet = modem.getTeCharacterSet();
      if (!"IRA".equals(teCharacterSet)) {
        throw new IllegalStateException("Could not set the TE characterset to IRA");
      }
    }
  }

  public void setHexTeCharacterSet() throws ResponseException, SerialException, TimeoutException {
    if (teCharacterSet != null && !teCharacterSet.equals("HEX")) {
      modem.setTeCharacterSet("HEX");
      teCharacterSet = modem.getTeCharacterSet();
      if (!"HEX".equals(teCharacterSet)) {
        throw new IllegalStateException("Could not set the TE characterset to HEX");
      }
    }
  }

  public void showCurrentTeCharacterSet() throws ResponseException, SerialException, TimeoutException {
    if (teCharacterSet == null) {
      teCharacterSet = modem.getTeCharacterSet();
    }
    log.info("The TE characterset is {}", teCharacterSet);
  }

  public void setSelectTECharacterSet() throws ResponseException, SerialException, TimeoutException {
    log.info("TE: {}", modem.getTeCharacterSet());
    modem.setTeCharacterSet("IRA");
    // [GSM, IRA, 8859-1, PCCP437, UCS2, HEX]
    log.info("test: {}", modem.getTeCharacterSets());
    log.info("TE: {}", modem.getTeCharacterSet());
  }

  public void sendPduMessage(final String destination, final int sequence) throws ResponseException, SerialException, TimeoutException {
    final String text = "KORE " + String.format("%03d", sequence) + "   " + LocalDateTime.now();
    final byte[] pdu = PduService.createSmsSubmitPdu(destination, text);
    log.debug("PDU[{}]: {}", pdu.length, Util.bytesToHexString(pdu));
    setPduMessageMode();
    modem.sendPdu((pdu.length) - (pdu[0] + 1), Util.bytesToHexString(pdu));
    log.info("Send {}: '{}' to {}", sequence, text, destination);
  }

  public void sendTextMessage(final String destination, final String text) throws ResponseException, SerialException, TimeoutException {
    final byte[] pdu = PduService.createSmsSubmitPdu(destination, text);
    log.debug("PDU[{}]: {}", pdu.length, Util.bytesToHexString(pdu));
    setPduMessageMode();
    modem.sendPdu((pdu.length) - (pdu[0] + 1), Util.bytesToHexString(pdu));
    log.info("Send '{}' to {}", text, destination);
  }

  public void sendBinaryPduMessage(final String destination, final byte[] data) throws ResponseException, SerialException, TimeoutException {
    final byte[] pdu = PduService.createSmsSubmitBinaryPdu(destination, data);
    log.debug("PDU[{}]: {}", pdu.length, Util.bytesToHexString(pdu));
    setPduMessageMode();
    modem.sendPdu((pdu.length) - (pdu[0] + 1), Util.bytesToHexString(pdu));
    log.info("Send: '{}' to {}", Util.bytesToHexString(pdu), destination);
  }

  public void showAllMessages() throws ResponseException, SerialException, TimeoutException {
    final List<IndexPduMessage> messages = getAllMessages();
    log.info("Found {} messages", messages.size());
    messages.forEach(m -> {
      final PduParser pduParser = new PduParser();
      // log.trace("PDU: {}", m.getPdu());
      final Pdu pdu = pduParser.parsePdu(m.getPdu());
      log.trace("{}: {}", getTpduType(pdu), m.getPdu());
      if (pdu instanceof SmsDeliveryPdu) {
        log.info("DELIVERY: SMSC:{} ADDRESS:{} DCS:{} PID:{} TEXT:'{}'", pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(),
            pdu.getProtocolIdentifier(),
            pdu.getDecodedText());
      } else if (pdu instanceof SmsStatusReportPdu) {
        log.info("STATUS-REPORT: SMSC:{} ADDRESS:{} DCS:{} PID:{}", pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(),
            pdu.getProtocolIdentifier());
      } else if (pdu instanceof SmsSubmitPdu) {
        log.info("SUBMIT: SMSC:{} ADDRESS:{} DCS:{} PID:{} TEXT:'{}'", pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(),
            pdu.getProtocolIdentifier(),
            pdu.getDecodedText());
      } else {
        log.info("?? SMSC:{} ADDRESS:{} DCS:{} PID:{} TEXT:'{}'", pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(),
            pdu.getProtocolIdentifier());
      }
    });
  }


  public void getMessage(final String connectionId, final String storage, final int index) throws ResponseException, SerialException, TimeoutException {
    final PduMessage pduMessage = readSms(connectionId, index);
    log.info("Found PDU message: {}", pduMessage.getPdu());
    final PduParser pduParser = new PduParser();
    final Pdu pdu = pduParser.parsePdu(pduMessage.getPdu());
    if (pdu instanceof SmsDeliveryPdu) {
      ApplicationContextProvider.getApplicationContext().publishEvent(new ReceivedPduEvent(this, connectionId, Util.hexToByteArray(pduMessage.getPdu())));
    } else if (pdu instanceof SmsStatusReportPdu) {
      ApplicationContextProvider.getApplicationContext()
          .publishEvent(new ReceivedSmsStatusReportPduEvent(this, connectionId, Util.hexToByteArray(pduMessage.getPdu())));
    }
  }

  public void sendAllMessagesViaSmpp(final String connectionId) throws ResponseException, SerialException, TimeoutException {
    final List<IndexPduMessage> messages = getAllMessages();
    log.info("Send all via SMPP, found {} messages", messages.size());
    messages.forEach(m -> {
      final PduParser pduParser = new PduParser();
      final Pdu pdu = pduParser.parsePdu(m.getPdu());
      log.info("Message: Index:{} Status:{} PDU:{}", m.getIndex(), m.getStatus(), m.getPdu());
      log.info(" SMSC:{} ADDRESS:{} DCS:{} PID:{} TEXT:'{}'", pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(), pdu.getProtocolIdentifier(),
          pdu.getDecodedText());
      if (pdu instanceof SmsDeliveryPdu) {
        log.info("SMS-DELIVERY SCTS:{} SMSC:{} ADDRESS:{} DCS:{} PID:{} TEXT:'{}'",
            ((SmsDeliveryPdu) pdu).getServiceCentreTimestamp(),
            pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(), pdu.getProtocolIdentifier(), pdu.getDecodedText());
        ApplicationContextProvider.getApplicationContext().publishEvent(new ReceivedSmsDeliveryPduEvent(this, connectionId, Util.hexToByteArray(m.getPdu())));
      } else if (pdu instanceof SmsStatusReportPdu) {
        log.info("SMS-STATUS-REPORT SCTS:{} SMSC:{} ADDRESS:{} DCS:{} PID:{} TEXT:'{}'",
            ((SmsStatusReportPdu) pdu).getDischargeTime(),
            pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(), pdu.getProtocolIdentifier(), pdu.getDecodedText());
        ApplicationContextProvider.getApplicationContext()
            .publishEvent(new ReceivedSmsStatusReportPduEvent(this, connectionId, Util.hexToByteArray(m.getPdu())));
      } else if (pdu instanceof SmsSubmitPdu) {
        log.info("Not handle SMS-SUBMIT");
      }
    });
  }

  public void deleteAllMessages() throws ResponseException, SerialException, TimeoutException {
    modem.deleteAllMessages();
  }

//  private Deliver getDeliver(final Pdu pdu) {
//    Deliver deliver = new Deliver();
//    deliver.setOriginatingAddress(pdu.getAddress());
//    deliver.setOriginatingAddressTon((byte) (pdu.getAddressType() & (byte) 0x21));
//    deliver.setOriginatingAddressNpi((byte) (pdu.getAddressType() & (byte) 0x03));
//    deliver.setUserData(pdu.getUDData());
//    deliver.setUserData(pdu.getUserDataAsBytes());
//    return deliver;
//  }

  public List<IndexPduMessage> getAllMessages() throws ResponseException, SerialException, TimeoutException {
    return getMessages(MessageStatus.ALL).stream().map(m -> (IndexPduMessage) m).collect(Collectors.toList());
  }

  public List<IndexMessage> getMessages(final MessageStatus messageStatus) throws ResponseException, SerialException, TimeoutException {
    return modem.getMessagesList(messageStatus);
  }

  public PduMessage readSms(final String connectionId, final int index) throws ResponseException, SerialException, TimeoutException {
    final Message message = modem.readSms(index).getMessage();
    if (message instanceof PduMessage) {
      return (PduMessage) message;
    }
    throw new IllegalStateException("SMS not in PDU mode");
  }

  private String getTpduType(final Pdu pdu) {
    if (pdu instanceof SmsSubmitPdu) {
      return "SMS-SUBMIT";
    }
    if (pdu instanceof SmsStatusReportPdu) {
      return "SMS-STATUS-REPORT";
    }
    if (pdu instanceof SmsDeliveryPdu) {
      return "SMS-DELIVERY";
    }
    if (pdu instanceof WapSiPdu) {
      return "WAP-SI-PUSH";
    }
    throw new IllegalArgumentException("Type is unknown");
  }
}
