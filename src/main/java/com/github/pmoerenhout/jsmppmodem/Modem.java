package com.github.pmoerenhout.jsmppmodem;

import static jssc.SerialPort.FLOWCONTROL_NONE;
import static jssc.SerialPort.FLOWCONTROL_RTSCTS_IN;
import static jssc.SerialPort.FLOWCONTROL_RTSCTS_OUT;
import static jssc.SerialPort.FLOWCONTROL_XONXOFF_IN;
import static jssc.SerialPort.FLOWCONTROL_XONXOFF_OUT;

import java.util.Objects;

import com.github.pmoerenhout.atcommander.jssc.JsscSerial;
import com.github.pmoerenhout.atcommander.module._3gpp.EtsiModem;

public class Modem {

  private String id;
  private boolean initialized = false;
  private EtsiModem threegppModem;
  private Sim sim;
  private MessageService messageService;
  private String port;
  private int speed;

  public Modem(final String id, final String port, final int speed, final FlowControl flowControl) {
    this.id = id;
    this.port = port;
    this.speed = speed;
    switch (flowControl) {
      case NONE:
        this.threegppModem = new EtsiModem(
            new JsscSerial(port, speed, FLOWCONTROL_NONE, new UnsolicitedCallback(this)));
        break;
      case RTSCTS:
        this.threegppModem = new EtsiModem(
            new JsscSerial(port, speed, FLOWCONTROL_RTSCTS_IN | FLOWCONTROL_RTSCTS_OUT, new UnsolicitedCallback(this)));
        break;
      case XONXOFF:
        this.threegppModem = new EtsiModem(
            new JsscSerial(port, speed, FLOWCONTROL_XONXOFF_IN | FLOWCONTROL_XONXOFF_OUT, new UnsolicitedCallback(this)));
        break;
    }
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public boolean isInitialized() {
    return initialized;
  }

  public void setInitialized(final boolean initialized) {
    this.initialized = initialized;
  }

  public EtsiModem get3gppModem() {
    return threegppModem;
  }

  public void set3gppModem(final EtsiModem threegppModem) {
    this.threegppModem = threegppModem;
  }

  public Sim getSim() {
    return sim;
  }

  public void setSim(final Sim sim) {
    this.sim = sim;
  }

  public String getPort() {
    return port;
  }

  public void setPort(final String port) {
    this.port = port;
  }

  public int getSpeed() {
    return speed;
  }

  public void setSpeed(final int speed) {
    this.speed = speed;
  }

  public MessageService getMessageService() {
    return messageService;
  }

  public void setMessageService(final MessageService messageService) {
    this.messageService = messageService;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Modem modem1 = (Modem) o;
    return speed == modem1.speed &&
        Objects.equals(id, modem1.id) &&
        Objects.equals(threegppModem, modem1.threegppModem) &&
        Objects.equals(port, modem1.port);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, threegppModem, port, speed);
  }
}
