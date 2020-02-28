package com.github.pmoerenhout.jsmppmodem.util.ie;

import com.github.pmoerenhout.jsmppmodem.util.Util;

// PduUtils Library - A Java library for generating GSM 3040 Protocol Data Units (PDUs)
//
// Copyright (C) 2008, Ateneo Java Wireless Competency Center/Blueblade Technologies, Philippines.
// PduUtils is distributed under the terms of the Apache License version 2.0
//
// Licensed under the Apache License, Version 2.0 (the "License");
// You may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

public class InformationElement {

  public static final byte IEI_CONCATENATED_8BIT = (byte) 0x00;
  public static final byte IEI_SPECIAL_SMS_MESSAGE_INDICATION = (byte) 0x01;
  public static final byte IEI_APPLICATIONPORT_8BIT = (byte) 0x04;
  public static final byte IEI_APPLICATIONPORT_16BIT = (byte) 0x05;
  public static final byte IEI_SERVICE_CENTER_CONTROL = (byte) 0x06;
  public static final byte IEI_UDH_SOURCE_INDICATOR = (byte) 0x07;
  public static final byte IEI_CONCATENATED_16BIT = (byte) 0x08;
  public static final byte IEI_WIRELESS_CONTROL_MESSAGE_PROTOCOL = (byte) 0x09;
  public static final byte IEI_TEXT_FORMATTING = (byte) 0x0a;
  public static final byte IEI_PREDEFINED_SOUND = (byte) 0x0b;
  public static final byte IEI_USERDEFINED_SOUND = (byte) 0x0c;
  public static final byte IEI_PREDEFINED_ANIMATION = (byte) 0x0d;
  public static final byte IEI_LARGE_ANIMATION = (byte) 0x0e;
  public static final byte IEI_SMALL_ANIMATION = (byte) 0x0f;
  public static final byte IEI_LARGE_PICTURE = (byte) 0x10;
  public static final byte IEI_SMALL_PICTURE = (byte) 0x11;
  public static final byte IEI_VARIABLESIZE_PICTURE = (byte) 0x12;
  public static final byte IEI_USER_PROMPT_INDICATOR = (byte) 0x13;
  public static final byte IEI_EXTENDED_OBJECT = (byte) 0x14;
  public static final byte IEI_REUSED_EXTENDED_OBJECT = (byte) 0x15;
  public static final byte IEI_COMPRESSION_CONTROL = (byte) 0x16;

  // http://books.google.nl/books?id=rYeHSlp0CMsC&pg=PA97&lpg=PA97&dq=ie+iedl+information+element+sms&source=bl&ots=c7K1C7cICA&sig=ffUVz2PMiYslTl1F3ypxtG8qVHU&hl=nl&sa=X&ei=ryjdUfzUBIWZPYKJgJAC&ved=0CDsQ6AEwAQ#v=onepage&q=ie%20iedl%20information%20element%20sms&f=false
  public static final byte IEI_EMAIL_HEADER = (byte) 0x20;
  public static final byte IEI_HYPERLINK_FORMAT = (byte) 0x21;
  public static final byte IEI_ALTERNATE_REPLY = (byte) 0x22;
  public static final byte IEI_ENHANCED_VOICEMAIL_NOTIFICATION = (byte) 0x23;

  // http://www.etsi.org/deliver/etsi_ts/131100_131199/131115/08.00.01_60/ts_131115v080001p.pdf
  public static final byte IEI_COMMAND_PACKET_HEADER = (byte) 0x70;
  public static final byte IEI_RESPONSE_PACKET_HEADER = (byte) 0x71;

  protected byte identifier;
  protected byte[] data;

  // iei
  // iel (implicit length of data)
  // ied (raw ie data)

  public InformationElement(final byte id) {
    this.identifier = id;
  }

  public InformationElement(final byte id, final byte[] data) {
    this.identifier = id;
    this.data = data;
  }

  InformationElement() {
  }

  public int getIdentifier() {
    return (this.identifier);
  }

  public int getLength() {
    return this.data.length;
  }

  public byte[] getData() {
    return this.data;
  }

  // for outgoing messages
  void setData(final byte[] data) {
    this.data = data;
  }


  public byte[] toBytes() {
    final byte[] ie = new byte[2 + data.length];
    ie[0] = identifier;
    ie[1] = (byte) (data.length & 0xff);
    System.arraycopy(data, 0, ie, 2, data.length);
    return ie;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName() + "[");
    sb.append(Util.bytesToHexString(this.identifier));
    sb.append(", ");
    sb.append(this.data.length);
    sb.append(", ");
    sb.append(Util.bytesToHexString(this.data));
    sb.append("]");
    return sb.toString();
  }
}
