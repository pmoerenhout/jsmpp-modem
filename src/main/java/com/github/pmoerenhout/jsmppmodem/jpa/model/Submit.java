package com.github.pmoerenhout.jsmppmodem.jpa.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "submit")
public class Submit implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "timestamp")
  private Instant timestamp;

  @Column(name = "connectionid")
  private String connectionId;

  @Column(name = "servicetype", updatable = false)
  private String serviceType;

  @Column(name = "source", updatable = false)
  private String source;

  @Column(name = "sourceton", updatable = false)
  private byte sourceTon;

  @Column(name = "sourcenpi", updatable = false)
  private byte sourceNpi;

  @Column(name = "destination", updatable = false)
  private String destination;

  @Column(name = "destinationton", updatable = false)
  private byte destinationTon;

  @Column(name = "destinationnpi", updatable = false)
  private byte destinationNpi;

  @Column(name = "esmclass", updatable = false)
  private byte esmClass;

  @Column(name = "dcs", updatable = false)
  private byte dataCodingScheme;

  @Column(name = "pid", updatable = false)
  private byte protocolIdentifier;

  @Column(name = "priority", updatable = false)
  private byte priorityFlag;

  @Column(name = "scheduled", updatable = false)
  private String scheduled;

  @Column(name = "validityperiod", updatable = false)
  private String validityPeriod;

  @Column(name = "replaceifpresent", updatable = false)
  private byte replaceIfPresentFlag;

  @Column(name = "shortmessage", updatable = false, columnDefinition = "blob")
  private byte[] shortMessage;

  @Column(name = "messageid")
  private String messageId;

  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(final String connectionId) {
    this.connectionId = connectionId;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final Instant timestamp) {
    this.timestamp = timestamp;
  }

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(final String serviceType) {
    this.serviceType = serviceType;
  }

  public String getSource() {
    return source;
  }

  public void setSource(final String source) {
    this.source = source;
  }

  public byte getSourceTon() {
    return sourceTon;
  }

  public void setSourceTon(final byte sourceTon) {
    this.sourceTon = sourceTon;
  }

  public byte getSourceNpi() {
    return sourceNpi;
  }

  public void setSourceNpi(final byte sourceNpi) {
    this.sourceNpi = sourceNpi;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(final String destination) {
    this.destination = destination;
  }

  public byte getDestinationTon() {
    return destinationTon;
  }

  public void setDestinationTon(final byte destinationTon) {
    this.destinationTon = destinationTon;
  }

  public byte getDestinationNpi() {
    return destinationNpi;
  }

  public void setDestinationNpi(final byte destinationNpi) {
    this.destinationNpi = destinationNpi;
  }

  public byte getEsmClass() {
    return esmClass;
  }

  public void setEsmClass(final byte esmClass) {
    this.esmClass = esmClass;
  }

  public byte getDataCodingScheme() {
    return dataCodingScheme;
  }

  public void setDataCodingScheme(final byte dataCodingScheme) {
    this.dataCodingScheme = dataCodingScheme;
  }

  public byte getProtocolIdentifier() {
    return protocolIdentifier;
  }

  public void setProtocolIdentifier(final byte protocolIdentifier) {
    this.protocolIdentifier = protocolIdentifier;
  }

  public byte getPriorityFlag() {
    return priorityFlag;
  }

  public void setPriorityFlag(final byte priorityFlag) {
    this.priorityFlag = priorityFlag;
  }

  public String getScheduled() {
    return scheduled;
  }

  public void setScheduled(final String scheduled) {
    this.scheduled = scheduled;
  }

  public String getValidityPeriod() {
    return validityPeriod;
  }

  public void setValidityPeriod(final String validityPeriod) {
    this.validityPeriod = validityPeriod;
  }

  public byte getReplaceIfPresentFlag() {
    return replaceIfPresentFlag;
  }

  public void setReplaceIfPresentFlag(final byte replaceIfPresentFlag) {
    this.replaceIfPresentFlag = replaceIfPresentFlag;
  }

  public byte[] getShortMessage() {
    return shortMessage;
  }

  public void setShortMessage(final byte[] shortMessage) {
    this.shortMessage = shortMessage;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(final String messageId) {
    this.messageId = messageId;
  }


  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Submit{");
    sb.append("id=").append(id);
    sb.append(", timestamp=").append(timestamp);
    sb.append(", connectionId='").append(connectionId).append('\'');
    sb.append(", serviceType='").append(serviceType).append('\'');
    sb.append(", source='").append(source).append('\'');
    sb.append(", sourceTon=").append(sourceTon);
    sb.append(", sourceNpi=").append(sourceNpi);
    sb.append(", destination='").append(destination).append('\'');
    sb.append(", destinationTon=").append(destinationTon);
    sb.append(", destinationNpi=").append(destinationNpi);
    sb.append(", esmClass=").append(esmClass);
    sb.append(", dataCodingScheme=").append(dataCodingScheme);
    sb.append(", protocolIdentifier=").append(protocolIdentifier);
    sb.append(", priorityFlag=").append(priorityFlag);
    sb.append(", scheduled='").append(scheduled).append('\'');
    sb.append(", validityPeriod='").append(validityPeriod).append('\'');
    sb.append(", replaceIfPresentFlag=").append(replaceIfPresentFlag);
    sb.append(", shortMessage=").append(Arrays.toString(shortMessage));
    sb.append(", messageId='").append(messageId).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
