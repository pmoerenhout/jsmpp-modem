package com.github.pmoerenhout.jsmppmodem.smsc;

import net.freeutils.charset.gsm.CCGSMCharset;
import net.freeutils.charset.gsm.CCPackedGSMCharset;

public class SmsUtil {

  private static final CCPackedGSMCharset GSM_PACKED_CHARSET = new CCPackedGSMCharset();
  private static final CCGSMCharset GSM_CHARSET = new CCGSMCharset();

  private SmsUtil() {
  }

  public static byte[] unpackGsm(final byte[] bytes) {
    return new String(bytes, GSM_PACKED_CHARSET).getBytes(GSM_CHARSET);
  }

  public static String packedGsmToString(final byte[] bytes) {
    return new String(bytes, GSM_PACKED_CHARSET);
  }
}
