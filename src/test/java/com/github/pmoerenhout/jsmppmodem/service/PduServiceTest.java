package com.github.pmoerenhout.jsmppmodem.service;

import static org.junit.Assert.assertEquals;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.ajwcc.pduutils.gsm0340.SmsDeliveryPdu;
import org.junit.Test;

import com.github.pmoerenhout.jsmppmodem.util.Util;
import net.freeutils.charset.gsm.CCPackedGSMCharset;

public class PduServiceTest {

  @Test
  public void test_decode__deliver_pdu() {
    final byte[] bytes = Util.hexToByteArray("0791448720003023240DD0E474D81C0EBB010000111011315214000BE474D81C0EBB5DE3771B");
    final SmsDeliveryPdu pdu = (SmsDeliveryPdu) PduService.decode(bytes);
    assertEquals("diafaan", pdu.getAddress());
    assertEquals(0x81, pdu.getAddressType());
    //assertEquals(new Date(111,0,11,14,25,41), pdu.getTimestamp());
    assertEquals(ZonedDateTime.of(2011, 1, 11, 13, 25, 41, 0, ZoneOffset.ofTotalSeconds(0)), pdu.getServiceCentreTimestamp());
    assertEquals("diafaan.com", new String(pdu.getUserDataAsBytes(), new CCPackedGSMCharset()));
  }
}