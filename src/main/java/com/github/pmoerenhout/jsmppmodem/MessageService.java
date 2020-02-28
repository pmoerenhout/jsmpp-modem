package com.github.pmoerenhout.jsmppmodem;

import java.time.LocalDateTime;
import java.util.List;

import org.ajwcc.pduutils.gsm0340.Pdu;
import org.ajwcc.pduutils.gsm0340.PduParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pmoerenhout.atcommander.api.SerialException;
import com.github.pmoerenhout.atcommander.basic.exceptions.ResponseException;
import com.github.pmoerenhout.atcommander.basic.exceptions.TimeoutException;
import com.github.pmoerenhout.atcommander.module._3gpp.EtsiModem;
import com.github.pmoerenhout.atcommander.module._3gpp.types.ListMessage;
import com.github.pmoerenhout.atcommander.module.v250.enums.MessageMode;
import com.github.pmoerenhout.atcommander.module.v250.enums.MessageStatus;
import com.github.pmoerenhout.jsmppmodem.events.ReceivedPduEvent;
import com.github.pmoerenhout.jsmppmodem.service.PduService;
import com.github.pmoerenhout.jsmppmodem.util.Util;

public class MessageService {

  // https://en.wikipedia.org/wiki/GSM_03.40

  private static final Logger LOG = LoggerFactory.getLogger(MessageService.class);

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
    LOG.info("The TE characterset is {}", teCharacterSet);
  }

  public void setSelectTECharacterSet() throws ResponseException, SerialException, TimeoutException {
    LOG.info("TE: {}", modem.getTeCharacterSet());
    modem.setTeCharacterSet("IRA");
    // [GSM, IRA, 8859-1, PCCP437, UCS2, HEX]
    LOG.info("test: {}", modem.getTeCharacterSets());
    LOG.info("TE: {}", modem.getTeCharacterSet());
  }

  public void sendPduMessage(final String destination, final int sequence) throws ResponseException, SerialException, TimeoutException {
    final String text = "KORE " + String.format("%03d", sequence) + "   " + LocalDateTime.now();
    final byte[] pdu = PduService.createSmsSubmitPdu(destination, text);
    LOG.debug("PDU[{}]: {}", pdu.length, Util.bytesToHexString(pdu));
    setPduMessageMode();
    modem.sendPdu((pdu.length) - (pdu[0] + 1), Util.bytesToHexString(pdu));
    LOG.info("Send {}: '{}'", sequence, text);
  }

  public void showAllMessages() throws ResponseException, SerialException, TimeoutException {
    final List<ListMessage> messages = getAllMessages();
    LOG.info("Found {} messages", messages.size());
    messages.forEach(m -> {
      final PduParser pduParser = new PduParser();
      final Pdu pdu = pduParser.parsePdu(m.getPdu());
      LOG.info("Message: PDU:{}", m.getPdu());
      LOG.info(" SMSC:{} ADDRESS:{} DCS:{} PID:{} TEXT:'{}'", pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(), pdu.getProtocolIdentifier(),
          pdu.getDecodedText());
    });
  }

  public void sendAllMessagesViaSmpp() throws ResponseException, SerialException, TimeoutException {
    final List<ListMessage> messages = getAllMessages();
    LOG.info("Found {} messages", messages.size());
    messages.forEach(m -> {
      final PduParser pduParser = new PduParser();
      final Pdu pdu = pduParser.parsePdu(m.getPdu());
      LOG.info("Message: Index:{} Status:{} PDU:{}", m.getIndex(), m.getStatus(), m.getPdu());
      LOG.info(" SMSC:{} ADDRESS:{} DCS:{} PID:{} TEXT:'{}'", pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(), pdu.getProtocolIdentifier(),
          pdu.getDecodedText());
      ApplicationContextProvider.getApplicationContext().publishEvent(new ReceivedPduEvent(this, Util.hexToByteArray(m.getPdu())));
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

  public List<ListMessage> getAllMessages() throws ResponseException, SerialException, TimeoutException {
    return getMessages(MessageStatus.ALL);
  }

  public List<ListMessage> getMessages(final MessageStatus messageStatus) throws ResponseException, SerialException, TimeoutException {
    return modem.getMessagesList(messageStatus);
  }
}
