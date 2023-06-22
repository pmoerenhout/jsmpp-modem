package com.github.pmoerenhout.jsmppmodem.smsc;

import java.nio.charset.Charset;
import java.util.BitSet;

import net.freeutils.charset.gsm.SCGSMCharset;
import net.freeutils.charset.gsm.SCPackedGSMCharset;

public class SmsUtil {

  private static final Charset GSM_PACKED_CHARSET = new SCPackedGSMCharset();
  private static final Charset GSM_CHARSET = new SCGSMCharset();

  private SmsUtil() {
  }

  public static byte[] unpackGsm(final byte[] bytes) {
    return new String(bytes, GSM_PACKED_CHARSET).getBytes(GSM_CHARSET);
  }

  public static String packedGsmToString(final byte[] bytes) {
    return new String(bytes, GSM_PACKED_CHARSET);
  }

  public static int fillBits(final int udhSize) {
    final int reminder = (udhSize % 7);
    if (reminder != 0) {
      return 7 - reminder;
    }
    return 0;
  }

  public static byte[] removeFillBits(final byte[] data, final int fillBits) {
    final BitSet dataBitSet = BitSet.valueOf(data);
    final BitSet bs = new BitSet(data.length * 8 - fillBits);
    for (int i = 0; i < (data.length * 8 - fillBits); i++) {
      bs.set(i, dataBitSet.get(i + fillBits));
    }
    return bs.toByteArray();
  }
}
