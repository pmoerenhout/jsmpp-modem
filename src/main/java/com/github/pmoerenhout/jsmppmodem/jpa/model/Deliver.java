package com.github.pmoerenhout.jsmppmodem.jpa.model;

import java.io.Serializable;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "deliver")
public class Deliver implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "timestamp")
  private Instant timestamp;

  @Column(name = "connectionid")
  private String connectionId;

  @Column(name = "pdu")
  private byte[] pdu;

//  @Column(name = "messageid")
//  private String messageId;
//
//  @Column(name = "servicetype", updatable = false)
//  private String serviceType;
//
//  @Column(name = "originatingaddress", updatable = false)
//  private String originatingAddress;
//
//  @Column(name = "originatingaddresston", updatable = false)
//  private byte originatingAddressTon;
//
//  @Column(name = "originatingaddressnpi", updatable = false)
//  private byte originatingAddressNpi;
//
//  @Column(name = "destination", updatable = false)
//  private String destination;
//
//  @Column(name = "destinationton", updatable = false)
//  private byte destinationTon;
//
//  @Column(name = "destinationnpi", updatable = false)
//  private byte destinationNpi;
//
//  @Column(name = "esmclass", updatable = false)
//  private byte esmClass;
//
//  @Column(name = "dcs", updatable = false)
//  private byte dataCodingScheme;
//
//  @Column(name = "pid", updatable = false)
//  private byte protocolIdentifier;
//
//  @Column(name = "priority", updatable = false)
//  private byte priorityFlag;
//
//  @Column(name = "scheduled", updatable = false)
//  private String scheduled;
//
//  @Column(name = "validityperiod", updatable = false)
//  private String validityPeriod;
//
//  @Column(name = "replaceifpresent", updatable = false)
//  private byte replaceIfPresentFlag;
//
//  @Column(name = "userdata", updatable = false, columnDefinition = "blob")
//  private byte[] userData;

  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final Instant timestamp) {
    this.timestamp = timestamp;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(final String connectionId) {
    this.connectionId = connectionId;
  }

  public byte[] getPdu() {
    return pdu;
  }

  public void setPdu(final byte[] pdu) {
    this.pdu = pdu;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("Deliver{");
    sb.append("id=").append(id);
    sb.append(", timestamp=").append(timestamp);
    sb.append(", connectionId='").append(connectionId).append('\'');
    sb.append(", pdu=");
    if (pdu == null) {
      sb.append("null");
    } else {
      sb.append('[');
      for (int i = 0; i < pdu.length; ++i) {
        sb.append(i == 0 ? "" : ", ").append(pdu[i]);
      }
      sb.append(']');
    }
    sb.append('}');
    return sb.toString();
  }
}
