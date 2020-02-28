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
@Table(name = "report")
public class Report implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "connectionid")
  private String connectionId;

  @Column(name = "timestamp")
  private Instant timestamp;

  @Column(name = "source")
  private String source;

  @Column(name = "sourceton")
  private byte sourceTon;

  @Column(name = "sourcenpi")
  private byte sourceNpi;

  @Column(name = "destination")
  private String destination;

  @Column(name = "destinationton")
  private byte destinationTon;

  @Column(name = "destinationnpi", updatable = false)
  private byte destinationNpi;

  @Column(name = "servicetype", updatable = false)
  private String serviceType;

  @Column(name = "messageid", updatable = false)
  private String messageId;

  @Column(name = "submitted", updatable = false)
  private byte submitted;

  @Column(name = "delivered", updatable = false)
  private byte delivered;

  @Column(name = "submitdate", updatable = false)
  private Instant submitDate;

  @Column(name = "donedate", updatable = false)
  private Instant doneDate;

  @Column(name = "state", updatable = false)
  private byte state;

  @Column(name = "error", updatable = false)
  private String error;

  @Column(name = "text", updatable = false)
  private byte[] text;

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

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(final String serviceType) {
    this.serviceType = serviceType;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(final String messageId) {
    this.messageId = messageId;
  }

  public byte getSubmitted() {
    return submitted;
  }

  public void setSubmitted(final byte submitted) {
    this.submitted = submitted;
  }

  public byte getDelivered() {
    return delivered;
  }

  public void setDelivered(final byte delivered) {
    this.delivered = delivered;
  }

  public Instant getSubmitDate() {
    return submitDate;
  }

  public void setSubmitDate(final Instant submitDate) {
    this.submitDate = submitDate;
  }

  public Instant getDoneDate() {
    return doneDate;
  }

  public void setDoneDate(final Instant doneDate) {
    this.doneDate = doneDate;
  }

  public byte getState() {
    return state;
  }

  public void setState(final byte state) {
    this.state = state;
  }

  public String getError() {
    return error;
  }

  public void setError(final String error) {
    this.error = error;
  }

  public byte[] getText() {
    return text;
  }

  public void setText(final byte[] text) {
    this.text = text;
  }

//  public Submit getSm() {
//    return sm;
//  }
//
//  public void setSm(final Submit sm) {
//    this.sm = sm;
//  }

  @Override
  public String toString() {
    return "Report{" +
        "id=" + id +
        ", timestamp=" + timestamp +
        ", source='" + source + '\'' +
        ", sourceTon=" + sourceTon +
        ", sourceNpi=" + sourceNpi +
        ", destination='" + destination + '\'' +
        ", destinationTon=" + destinationTon +
        ", destinationNpi=" + destinationNpi +
        ", serviceType='" + serviceType + '\'' +
        ", messageId='" + messageId + '\'' +
        ", submitted=" + submitted +
        ", delivered=" + delivered +
        ", submitDate=" + submitDate +
        ", doneDate=" + doneDate +
        ", state=" + state +
        ", error='" + error + '\'' +
        ", text=" + Arrays.toString(text) +
        '}';
  }
}
