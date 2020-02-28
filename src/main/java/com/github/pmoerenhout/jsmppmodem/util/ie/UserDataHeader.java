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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

public class UserDataHeader {

  private List<InformationElement> informationElements = new ArrayList<>();

  public UserDataHeader(final byte[] data) throws UserDataHeaderException {
    final int length = (data[0] & 0xff);
    if (length > 138) {
      throw new UserDataHeaderException("The information elements has more then 138 bytes");
    }
    if (data.length < length) {
      throw new UserDataHeaderException("Not enough data in user data header, expecting " + length + " bytes, got " + data.length);
    }
    int offset = 1;
    while (offset < length) {
      final byte iei = data[offset];
      if (offset + 1 >= data.length) {
        throw new UserDataHeaderException("Not enough data left for information element, expecting " + length + " bytes, got " + data.length);
      }
      final int iel = data[offset + 1] & 0xff;
      final InformationElement ie = InformationElementFactory.createInformationElement(iei, ArrayUtils.subarray(data, offset + 2, offset + 2 + iel));
      informationElements.add(ie);
      offset += (2 + iel);
    }
  }

  public List<InformationElement> getInformationElements() {
    return informationElements;
  }

  public Optional<InformationElement> getInformationElement(final byte identifier) {
    return informationElements.stream().filter(s -> s.identifier == identifier)
        .reduce((u, v) -> {
          throw new IllegalArgumentException("More than one information elements with identifier " + String.format("0x%02x", identifier) + " found");
        });
  }

  public List<InformationElement> getInformationElements(final byte identifier) {
    return informationElements.stream().filter(s -> s.identifier == identifier).collect(Collectors.toList());
  }
}
