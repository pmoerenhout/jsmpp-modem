package com.github.pmoerenhout.jsmppmodem.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

import javax.activation.DataHandler;

public class Util {

  private static final String STR_NULL = "NULL";
  private static final int INITIAL_SIZE = 256 * 1024;
  private static final int BUFFER_SIZE = 1024;
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME ;

  public static String bytesToHexString(final byte[] bytes) {
    if (bytes == null) {
      return STR_NULL;
    }
    final StringBuilder sb = new StringBuilder(bytes.length * 2);
    final Formatter formatter = new Formatter(sb);
    for (byte b : bytes) {
      formatter.format("%02X", b);
    }
    formatter.close();
    return sb.toString();
  }

  public static String bytesToHexString(final byte b) {
    final StringBuilder sb = new StringBuilder(2);
    final Formatter formatter = new Formatter(sb);
    formatter.format("%02X", b);
    formatter.close();
    return sb.toString();
  }

  public static String stringToHexString(String s, int i) {
    String ss = bytesToHexString(s.getBytes());
    for (int j = 0; j < i - s.length(); j++) {
      ss += "FF";
    }
    return ss;
  }

  public static byte[] hexToByteArray(final String s) {
    final String s2 = s.replaceAll(" ", "");
    final int length = s2.length() / 2;
    final byte[] b = new byte[length];
    for (int i = 0; i < length; i++) {
      b[i] = Integer.valueOf(s2.substring(i * 2, (i * 2) + 2), 16).byteValue();
    }
    return b;
  }

  public static byte hexToByte(final String s) {
    final String s2 = s.replaceAll(" ", "");
    final byte b = Integer.valueOf(s2.substring(0, 2), 16).byteValue();
    return b;
  }

  public static String reverse(String s) {
    final StringBuilder sb = new StringBuilder();
    if (s.length() % 2 == 1) {
      s += "F";
    }
    for (int i = 0; i < s.length(); i += 2) {
      sb.append(s.charAt(i + 1));
      sb.append(s.charAt(i));
    }
    return sb.toString();
  }

  public static String address(String s) {
    final StringBuilder sb = new StringBuilder();
    final int semiOctets = s.length();
    if (s.length() % 2 == 1) {
      s += "F";
    }
    for (int i = 0; i < s.length(); i += 2) {
      sb.append(s.charAt(i + 1));
      sb.append(s.charAt(i));
    }
    return String.format("%02X", semiOctets) + "91" + sb.toString();
  }

  public static byte[] address(final String s, final int ton, final int npi) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.write(1 + s.length() / 2);
    baos.write(0x80 | ((ton & 0x07) >> 4) | ((byte) (npi & (byte) 0x08)));
    for (int i = 0; i < s.length(); i += 2) {
      int j = s.charAt(i) & 0x0F;

      baos.write(j);
    }
    return baos.toByteArray();
  }

  public static String smscAddress(String s) {
    final StringBuilder sb = new StringBuilder();
    if (s == null || s.length() == 0) {
      return "00";
    }
    // int semiOctets = s.length();
    if (s.length() % 2 == 1) {
      s += "F";
    }
    for (int i = 0; i < s.length(); i += 2) {
      sb.append(s.charAt(i + 1));
      sb.append(s.charAt(i));
    }
    return String.format("%02X", 1 + (s.length() / 2)) + "91"
        + sb.toString();
  }

  public static int bcd2dec(int bcd) {
    int dec = 0;
    int mult;
    for (mult = 1; bcd != 0; bcd = bcd >> 4, mult *= 10) {
      dec += (bcd & 0x0f) * mult;
    }
    return dec;
  }

  public static byte[] dataHandlerToBytes(final DataHandler dh) throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream(INITIAL_SIZE);
    final InputStream in = dh.getInputStream();
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;
    while ((bytesRead = in.read(buffer)) >= 0) {
      bos.write(buffer, 0, bytesRead);
    }
    return bos.toByteArray();
  }

  public static Calendar dateToCalendar(final Date date) {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    return cal;
  }

  public static long counterToLong(final byte[] bytes) {
    long result = 0;
    if (bytes.length > 8) {
      throw new IllegalArgumentException("Cannot put more then 8 bytes in long");
    }
    for (int i = 0; i < bytes.length; i++) {
      result <<= 8;
      result |= (bytes[i] & 0xFF);
    }
    return result;
  }

  public static int status(final byte sw1, final byte sw2) {
    return (((sw1 << 8) & (byte) 0xff) | (sw2 & (byte) 0xff)) & 0xffff;
  }

  public static int status(final byte[] data) {
    if (data.length < 2) {
      return -1;
    }
    return status(data[data.length - 2], data[data.length - 1]);
  }

  public static String statusAsString(final int status) {
    return String.format("%04X", status);
  }

//  public static <T> Consumer<T> throwingConsumerWrapper(ThrowingConsumer<T, Exception> throwingConsumer) {
//    return i -> {
//      try {
//        throwingConsumer.accept(i);
//      } catch (Exception ex) {
//        throw new RuntimeException(ex);
//      }
//    };
//  }

  public static String formatZonedSystemDefault(final Instant instant){
    return DATE_TIME_FORMATTER.format(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
  }

  public static Integer defaultIfNull(final Integer integer, final Integer defaultInteger) {
    return integer == null ? defaultInteger : integer;
  }

  public static String onlyPrintable(final byte[] buf) {
    final int l = buf.length;
    final StringBuilder sb = new StringBuilder(l);
    for (int i = 0; i < l; i++) {
      final byte b = buf[i];
      if (b >= 32 && b < 127) {
        sb.append((char) b);
      } else {
        sb.append(String.format("<%02x>", b));
      }
    }
    return sb.toString();
  }

}
