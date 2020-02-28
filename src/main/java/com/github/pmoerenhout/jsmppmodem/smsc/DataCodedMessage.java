package com.github.pmoerenhout.jsmppmodem.smsc;

public class DataCodedMessage {

  private byte[] message;
  private byte codingScheme;

  public DataCodedMessage(final byte[] message, final byte codingScheme) {
    this.message = message;
    this.codingScheme = codingScheme;
  }

  public byte[] getMessage() {
    return message;
  }


  public byte getCodingScheme() {
    return codingScheme;
  }

}
