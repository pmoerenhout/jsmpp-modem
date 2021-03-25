package com.github.pmoerenhout.jsmppmodem.smsc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "smpp")
public class SmppConfiguration {

  private int port;
  private boolean ssl;
  private String serviceType;
  private String charset;
  private Long bindTimeout;
  private Integer enquireLinkTimer;
  private Integer transactionTimer;

  public SmppConfiguration() {
    this.port = 2775;
    this.ssl = false;
    this.serviceType = "CMT";
    this.bindTimeout = 60000L;
    this.enquireLinkTimer = 0;
    this.transactionTimer = 60000;
  }

  public int getPort() {
    return port;
  }

  public void setPort(final int port) {
    this.port = port;
  }

  public boolean isSsl() {
    return ssl;
  }

  public void setSsl(final boolean ssl) {
    this.ssl = ssl;
  }

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(final String serviceType) {
    this.serviceType = serviceType;
  }

  public String getCharset() {
    return charset;
  }

  public void setCharset(final String charset) {
    this.charset = charset;
  }

  public Long getBindTimeout() {
    return bindTimeout;
  }

  public void setBindTimeout(final Long bindTimeout) {
    this.bindTimeout = bindTimeout;
  }

  public Integer getEnquireLinkTimer() {
    return enquireLinkTimer;
  }

  public void setEnquireLinkTimer(final int enquireLinkTimer) {
    this.enquireLinkTimer = enquireLinkTimer;
  }

  public Integer getTransactionTimer() {
    return transactionTimer;
  }

  public void setTransactionTimer(final int transactionTimer) {
    this.transactionTimer = transactionTimer;
  }

}
