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

public class ResponsePacketElement extends InformationElement {

  ResponsePacketElement(final byte id, final byte[] data) {
    super(id, data);
    if (getIdentifier() != IEI_RESPONSE_PACKET_HEADER) {
      throw new RuntimeException("Invalid identifier " + getIdentifier() + " in data in: " + getClass().getSimpleName());
    }
    // iei
    // iel
    // ied
  }
}
