package com.github.pmoerenhout.jsmppmodem.app;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pmoerenhout.atcommander.api.SerialException;
import com.github.pmoerenhout.atcommander.basic.exceptions.ResponseException;
import com.github.pmoerenhout.atcommander.basic.exceptions.TimeoutException;
import com.github.pmoerenhout.atcommander.module._3gpp.EtsiModem;
import com.github.pmoerenhout.atcommander.module._3gpp.types.ListMessage;
import com.github.pmoerenhout.atcommander.module.v250.enums.MessageMode;
import com.github.pmoerenhout.atcommander.module.v250.enums.MessageStatus;

public class MessageService {

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
      modem.setSelectTECharacterSet("IRA");
      teCharacterSet = modem.getSelectTECharacterSet();
      if (!"IRA".equals(teCharacterSet)) {
        throw new IllegalStateException("Could not set the TE characterset to IRA");
      }
    }
  }

  public void showCurrentTeCharacterSet() throws ResponseException, SerialException, TimeoutException {
    if (teCharacterSet == null) {
      teCharacterSet = modem.getSelectTECharacterSet();
    }
    LOG.info("The TE characterset is {}", teCharacterSet);
  }

  public void setSelectTECharacterSet() throws ResponseException, SerialException, TimeoutException {
    LOG.info("TE: {}", modem.getSelectTECharacterSet());
    modem.setSelectTECharacterSet("IRA");
    // [GSM, IRA, 8859-1, PCCP437, UCS2, HEX]
    LOG.info("test: {}", modem.testSelectTECharacterSet());
    LOG.info("TE: {}", modem.getSelectTECharacterSet());
  }

  public void sendPduMessage() throws ResponseException, SerialException, TimeoutException {
    setPduMessageMode();
    modem.sendSmsAsPdu("0614240689", "Hello Pim");
  }

  public void showAllMessages() throws ResponseException, SerialException, TimeoutException {
    final List<ListMessage> messages = getAllMessages();
    LOG.info("Found {} messages", messages.size());
    messages.forEach(m -> {
      LOG.info("Message: PDU:{} TEXT:{}", m.getPdu(), m.getText());
    });
  }

  public List<ListMessage> getAllMessages() throws ResponseException, SerialException, TimeoutException {
    return getMessages(MessageStatus.ALL);
  }

  public List<ListMessage> getMessages(final MessageStatus messageStatus) throws ResponseException, SerialException, TimeoutException {
    return modem.getMessagesList(messageStatus);
  }
}
