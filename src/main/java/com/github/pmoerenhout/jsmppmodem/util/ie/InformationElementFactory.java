package com.github.pmoerenhout.jsmppmodem.util.ie;

import static com.github.pmoerenhout.jsmppmodem.util.ie.ConcatInformationElement.CONCAT_16BIT_REF;
import static com.github.pmoerenhout.jsmppmodem.util.ie.ConcatInformationElement.CONCAT_8BIT_REF;
import static com.github.pmoerenhout.jsmppmodem.util.ie.InformationElement.IEI_APPLICATIONPORT_16BIT;
import static com.github.pmoerenhout.jsmppmodem.util.ie.InformationElement.IEI_COMMAND_PACKET_HEADER;
import static com.github.pmoerenhout.jsmppmodem.util.ie.InformationElement.IEI_RESPONSE_PACKET_HEADER;

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

public class InformationElementFactory {
  // used  to determine what InformationElement to use based on bytes from a UDH
  // assumes the supplied bytes are correct
  public static InformationElement createInformationElement(int id, byte[] data) {
    final byte iei = (byte) (id & 0xff);
    switch (iei) {
      case CONCAT_8BIT_REF:
      case CONCAT_16BIT_REF:
        return new ConcatInformationElement(iei, data);
      case IEI_APPLICATIONPORT_16BIT:
        return new PortInformationElement(iei, data);
      case IEI_COMMAND_PACKET_HEADER:
        return new CommandPacketElement(iei, data);
      case IEI_RESPONSE_PACKET_HEADER:
        return new ResponsePacketElement(iei, data);
      default:
        return new InformationElement(iei, data);
    }
  }

  public static ConcatInformationElement generateConcatInfo(int mpRefNo, int partNo) {
    final ConcatInformationElement concatInfo = new ConcatInformationElement(ConcatInformationElement.getDefaultConcatType(), mpRefNo, 1, partNo);
    return concatInfo;
  }

  public static ConcatInformationElement generateConcatInfo(int identifier, int mpRefNo, int partNo) {
    final ConcatInformationElement concatInfo = new ConcatInformationElement(identifier, mpRefNo, 1, partNo);
    return concatInfo;
  }

  public static PortInformationElement generatePortInformation(final int destPort, final int srcPort) {
    final PortInformationElement portInformation = new PortInformationElement(IEI_APPLICATIONPORT_16BIT, destPort, srcPort);
    return portInformation;
  }

  public static CommandPacketElement generateCommandPacket(final byte[] data) {
    final CommandPacketElement commandPacket = new CommandPacketElement(IEI_RESPONSE_PACKET_HEADER, data);
    return commandPacket;
  }

  public static ResponsePacketElement generateResponsePacket(final byte[] data) {
    final ResponsePacketElement responsePacket = new ResponsePacketElement(IEI_RESPONSE_PACKET_HEADER, data);
    return responsePacket;
  }
}
