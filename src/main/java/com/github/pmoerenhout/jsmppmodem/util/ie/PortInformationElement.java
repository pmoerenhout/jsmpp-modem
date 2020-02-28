package com.github.pmoerenhout.jsmppmodem.util.ie;

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

public class PortInformationElement extends InformationElement {

  private static final int LENGTH = 4;

  PortInformationElement(byte id, byte[] data) {
    super(id, data);
    if (getIdentifier() != IEI_APPLICATIONPORT_16BIT) {
      throw new RuntimeException("Invalid identifier " + getIdentifier() + " in data in: " + getClass().getSimpleName());
    }
    // iei
    // iel
    // dest(2 bytes)
    // src (2 bytes)
    if (data.length != LENGTH) {
      throw new RuntimeException("Invalid data length in: " + getClass().getSimpleName());
    }
  }

  PortInformationElement(int identifier, int destPort, int srcPort) {
    super((byte) (identifier & (byte)0xff));
    final byte[] data;
    switch (identifier) {
      case IEI_APPLICATIONPORT_16BIT:
        data = new byte[LENGTH];
        data[0] = (byte) ((destPort & 0xff00) >>> 8);
        data[1] = (byte) (destPort & 0xff);
        data[2] = (byte) ((srcPort & 0xff00) >>> 8);
        data[3] = (byte) (srcPort & 0xff);
        break;
      default:
        throw new RuntimeException("Invalid identifier for " + getClass().getSimpleName());
    }
    setData(data);
  }

  public int getDestPort() {
    // first 2 bytes of data
    byte[] data = getData();
    return (((data[0] & (byte) 0xff) << 8) | (data[1] & (byte) 0xff));
  }

  public int getSrcPort() {
    // next 2 bytes of data
    byte[] data = getData();
    return (((data[2] & (byte) 0xff) << 8) | (data[3] & (byte) 0xff));
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(super.toString());
    sb.append("[dst port: ");
    sb.append(getDestPort());
    sb.append(", src port: ");
    sb.append(getSrcPort());
    sb.append("]");
    return sb.toString();
  }
}
