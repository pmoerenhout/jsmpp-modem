package com.github.pmoerenhout.jsmppmodem.smsc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmppSmsTranscoding {

  // http://www.qtc.jp/3GPP/GSM/SMG_20/tdocs/P-96-607.pdf

  private static final Logger LOG = LoggerFactory.getLogger(SmppSmsTranscoding.class);

  public DataCodedMessage toSmpp(final byte[] tpUd, final byte tpDcs) {
    if (tpDcs == 0x00) {
      return new DataCodedMessage(SmsUtil.unpackGsm(tpUd), (byte) 0x00);
    }
    if ((tpDcs & (byte) 0xc0) == (byte) 0xc0) {
      LOG.info("DCS {} is General Data Coding indication", String.format("%02X", tpDcs));
      final boolean compressed = ((tpDcs & (byte) 0x20) == (byte) 0x20);
      LOG.info("DCS {} is compressed? {}", String.format("%02X", tpDcs), compressed);
      final boolean messageClass = ((tpDcs & (byte) 0x10) == (byte) 0x10);
      LOG.info("DCS {} has MessageClass? {}", String.format("%02X", tpDcs), messageClass);
      switch ((tpDcs & (byte) 0x0c)) {
        case 0x00:
          // default alphabet
          return new DataCodedMessage(SmsUtil.unpackGsm(tpUd), tpDcs);
        case 0x04:
          // 8 bit
          return new DataCodedMessage(SmsUtil.unpackGsm(tpUd), tpDcs);
        case 0x08:
          // UCS2
          return new DataCodedMessage(tpUd, tpDcs);
        case 0x0c:
          // reserved
          throw new IllegalArgumentException("DCS is reserved");
      }
    }
    return new DataCodedMessage(tpUd, tpDcs);
  }
}
