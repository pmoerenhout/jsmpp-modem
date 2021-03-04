package com.github.pmoerenhout.jsmppmodem.smsc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.github.pmoerenhout.jsmppmodem.util.Util;
import net.freeutils.charset.gsm.CCPackedGSMCharset;

class SmsUtilTest {

  @Test
  public void test_unpack() {
    // text is Test@0
    final byte[] bytes = new byte[]{ (byte) 0xd4, (byte) 0xf2, (byte) 0x9c, (byte) 0x0e, (byte) 0x80, (byte) 0x01, };
    assertArrayEquals(new byte[]{ (byte) 0x54, (byte) 0x65, (byte) 0x73, (byte) 0x74, (byte) 0x00, (byte) 0x30 }, SmsUtil.unpackGsm(bytes));
  }

  @Test
  public void test_packed1() {
    // text is .com. Groet, KPN
    final String expected = "Beste klant. Uw tegoed bedraagt 0,00 euro en is houdbaar tot 11 nov 2020.\n" +
        "Uw extra registratie of prepaidkorting tegoed bedraagt 7,34 ";
    final int udhl = 6;
    final byte[] bytes = Util.hexToByteArray(
        "84E539BD0C5AB3C36EBA0B54BD83E8E5F3BB4C0689CB6479387CA683602C180C54AECBDFA0B21B949E83D0EF3A591C0ECB41F4371D148B81DC6F3B480693C15C8AEA1D54C6D3E56190BC7C4ECFE9F2303D5D06BDCD20B8BC0C0FA7C9EBB79C9E769F41F4F2F95D2683C465B23C1C3ED34137D68C0602");
    final byte[] alignedBytes = SmsUtil.removeFillBits(bytes, SmsUtil.fillBits(udhl));
    final byte[] ud = Util.hexToByteArray(
        StringUtils.repeat("0", udhl * 2) + Util.bytesToHexString(bytes));
    System.out.println(Util.bytesToHexString(alignedBytes));
    System.out.println(new String(alignedBytes, new CCPackedGSMCharset()));
    final String paddedString = StringUtils.substring(new String(ud, new CCPackedGSMCharset()), getLengthForUdhLength(udhl));
    assertEquals(expected, paddedString);
    assertEquals(expected, new String(alignedBytes, new CCPackedGSMCharset()));
  }


  @Test
  public void test_packed2() {
    final byte[] bytes = Util.hexToByteArray(
        "CA75F9DBA5A8DE41737A584EA797CFEF3219242E93E5E1F0990E82B162A0725DFE7629AA77D012EA043DDDE232BC2C5FD3414F373BED2E83E0F277FB4D4F9741E2BA9B5C6683EC65B93DCCA683DE70102C0752D7DD20194C067329ACEFB71CD42E97E5A0B4DBFC96B7C3F47459076AA7D56ED71AEE06");
    final byte[] bytes2 = Util.hexToByteArray(
        "75F9DBA5A8DE41737A584EA797CFEF3219242E93E5E1F0990E82B162A0725DFE7629AA77D012EA043DDDE232BC2C5FD3414F373BED2E83E0F277FB4D4F9741E2BA9B5C6683EC65B93DCCA683DE70102C0752D7DD20194C067329ACEFB71CD42E97E5A0B4DBFC96B7C3F47459076AA7D56ED71AEE06");
    assertEquals("uro.\n" +
        "Uw starttegoed bedraagt 0,1 euro.\n" +
        "Uw KPN Onbeperkt Online promotie bundel vervalt op 09 jun 2020.\n" +
        "Voor meer informatie: mijn.kpn", new String(bytes2, new CCPackedGSMCharset()));
  }

  @Test
  public void test_packed3() {
    final String expected = ".com. Groet, KPN";
    final byte[] bytes = Util.hexToByteArray("5CE377DB053ACADF653A0BB4843A01");

    final int udhl = 6;
    final byte[] alignedBytes = SmsUtil.removeFillBits(bytes, SmsUtil.fillBits(udhl));
    final byte[] ud = Util.hexToByteArray(
        StringUtils.repeat("0", udhl * 2) + Util.bytesToHexString(bytes));
    System.out.println(Util.bytesToHexString(alignedBytes));
    System.out.println(new String(alignedBytes, new CCPackedGSMCharset()));
    final String paddedString = StringUtils.substring(new String(ud, new CCPackedGSMCharset()), getLengthForUdhLength(udhl));
    assertEquals(expected, paddedString);
    assertEquals(expected, new String(alignedBytes, new CCPackedGSMCharset()));
  }

  @Test
  public void test_packed_12345678() {
    assertEquals("12345678", new String(Util.hexToByteArray("31D98C56B3DD70"), new CCPackedGSMCharset()));
    assertEquals("31D98C56B3DD70", Util.bytesToHexString("12345678".getBytes(new CCPackedGSMCharset())));
  }

  @Test
  public void test_packed_hellohello() {
    assertEquals("hellohello", new String(Util.hexToByteArray("E8329BFD4697D9EC37"), new CCPackedGSMCharset()));
    assertEquals("E8329BFD4697D9EC37", Util.bytesToHexString("hellohello".getBytes(new CCPackedGSMCharset())));
  }

  @Test
  public void test_pack() {
    // text is .com. Groet, KPN
    final String text = ".com. Groet, KPN";
    assertEquals("AEF1BBED021DE5EF329D055A429D", Util.bytesToHexString(text.getBytes(new CCPackedGSMCharset())));

    final String filledText = "@@@@@@@.com. Groet, KPN";
    assertEquals("0000000000005CE377DB053ACADF653A0BB4843A01", Util.bytesToHexString(filledText.getBytes(new CCPackedGSMCharset())));
  }

  @Test
  public void test_length_for_udh_length() {
    for (int i = 0; i < 32; i++) {
      System.out.println(i + "\t=>\t" + getLengthForUdhLength(i));
    }
  }

  private int getLengthForUdhLength(final int i) {
    int chars = (i * 8) / 7;
    final int reminder = ((i * 8) % 7);
    if (reminder != 0) {
      chars++;
    }
    return chars;
  }
}