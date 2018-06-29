package com.github.pmoerenhout.jsmppmodem.app;

import static jssc.SerialPort.FLOWCONTROL_RTSCTS_IN;
import static jssc.SerialPort.FLOWCONTROL_RTSCTS_OUT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.pmoerenhout.atcommander.jssc.JsscSerial;
import com.github.pmoerenhout.atcommander.module._3gpp.EtsiModem;

@Service
public class ModemService {

  private static final Logger LOG = LoggerFactory.getLogger(ModemService.class);

  private MessageService messageService;

  @Value("/dev/tty.usbserial-00101314B")
  private String port;

  @Value("115200")
  private int speed;

  private String manufacturerIdentification;
  private String revisionIdentification;
  private String serialNumber;
  private String productSerialNumberIdentification;

  private String imsi;

  public ModemService() {
  }

  public void init() {
    LOG.info("Initialize 3GPP modem (27.007)");
    final EtsiModem modem = new EtsiModem(
        new JsscSerial(port, speed, FLOWCONTROL_RTSCTS_IN | FLOWCONTROL_RTSCTS_OUT,
            new UnsolicitedCallback()));
    try {
      modem.init();
      modem.getAttention();
      manufacturerIdentification = modem.getManufacturerIdentification();
      revisionIdentification = modem.getRevisionIdentification();
      serialNumber = modem.getSerialNumber();
      productSerialNumberIdentification = modem.getProductSerialNumberIdentification();
      imsi = modem.getInternationalMobileSubscriberIdentity();
      LOG.info("Manufacturer: {}", manufacturerIdentification);
      LOG.info("Revision identification: {}", revisionIdentification);
      LOG.info("Serial number: {}", serialNumber);
      LOG.info("Product serial number identification: {}", productSerialNumberIdentification);
      LOG.info("IMSI: {}", imsi);

      messageService = new MessageService(modem);
      // messageService.setTextMessageMode();
      messageService.setPduMessageMode();
      messageService.showCurrentTeCharacterSet();
      messageService.setIraTeCharacterSet();
      messageService.showAllMessages();
      //messageService.sendPduMessage();

      Thread.sleep(10000);

      modem.close();
      LOG.info("ETSI modem via jSSC is closed");
    } catch (final Exception e) {
      LOG.error("The modem had an error", e);
    }
  }

}
