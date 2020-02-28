package com.github.pmoerenhout.jsmppmodem.smsc;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class SmsUtilTest {

  @Test
  public void test_unpack() {
    // text is Test@0
    final byte[] bytes = new byte[]{ (byte) 0xd4, (byte) 0xf2,(byte) 0x9c,(byte) 0x0e,(byte) 0x80,(byte) 0x01,} ;
    assertArrayEquals(new byte[]{ (byte) 0x54, (byte) 0x65, (byte) 0x73, (byte) 0x74, (byte) 0x00, (byte) 0x30 }, SmsUtil.unpackGsm(bytes));
  }
}