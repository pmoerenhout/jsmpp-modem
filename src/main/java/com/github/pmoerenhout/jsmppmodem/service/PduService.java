package com.github.pmoerenhout.jsmppmodem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pmoerenhout.jsmppmodem.util.Util;
import com.github.pmoerenhout.pduutils.MsIsdn;
import com.github.pmoerenhout.pduutils.gsm0340.Pdu;
import com.github.pmoerenhout.pduutils.gsm0340.PduFactory;
import com.github.pmoerenhout.pduutils.gsm0340.PduGenerator;
import com.github.pmoerenhout.pduutils.gsm0340.PduParser;
import com.github.pmoerenhout.pduutils.gsm0340.SmsDeliveryPdu;
import com.github.pmoerenhout.pduutils.gsm0340.SmsSubmitPdu;

public class PduService {

  private static final Logger LOG = LoggerFactory.getLogger(PduService.class);

  public static byte[] createSmsSubmitPdu(final String address, final String text) {
    final PduGenerator pduGenerator = new PduGenerator();
    final SmsSubmitPdu pdu = PduFactory.newSmsSubmitPdu();
    pdu.setMessageReference(0);
//    pdu.setSmscAddress("316540998300");
//    pdu.setSmscAddressType(17);
    pdu.setAddress(new MsIsdn(address));
    pdu.setAddressType(0x11);
    pdu.setDecodedText(text);
    pdu.setDataCodingScheme(0);
    pdu.setProtocolIdentifier(0);
    pdu.setValidityPeriod(1);
    return Util.hexToByteArray(pduGenerator.generatePduString(pdu));
  }

  public static byte[] createSmsSubmitBinaryPdu(final String address, final byte[] data) {
    final PduGenerator pduGenerator = new PduGenerator();
    final SmsSubmitPdu pdu = PduFactory.newSmsSubmitPdu();
    pdu.setMessageReference(0);
    pdu.setAddress(new MsIsdn(address));
    pdu.setAddressType(0x11);
    pdu.setDataCodingScheme(4);
    pdu.setProtocolIdentifier(0);
    pdu.setValidityPeriod(1);
    pdu.setTpUdhi(0);
    pdu.setUDLength(data.length);
    pdu.setUDData(data);
    pdu.setDataBytes(data);
    return Util.hexToByteArray(pduGenerator.generatePduString(pdu));
  }

  public static Pdu decode(final byte[] bytes) {
    final PduParser pduParser = new PduParser();
    final String hex = Util.bytesToHexString(bytes);
    LOG.info("HEX: {}", hex);
    final Pdu pdu = pduParser.parsePdu(hex);
    LOG.info("Message: PDU:{} ({})", hex, pdu.getClass().getSimpleName());
    LOG.info(" SMSC:{} ADDRESS:{} DCS:{} PID:{} TEXT:'{}'", pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(), pdu.getProtocolIdentifier(),
        pdu.getDecodedText());
    LOG.info("SMSC            : 0x{} '{}'", String.format("%02X", pdu.getSmscAddressType()), pdu.getSmscAddress());
    LOG.info("TP-MTI          : '{}'", String.format("%02X", pdu.getTpMti()));
    LOG.info("TP-UDHI         : {}", pdu.hasTpUdhi());
    LOG.info("TP-DCS          : '{}'", String.format("%02X", pdu.getDataCodingScheme()));
    LOG.info("TP-PID          : '{}'", String.format("%02X", pdu.getProtocolIdentifier()));
    LOG.info("Orig Address    : 0x{} '{}'", String.format("%02X",pdu.getAddressType()), pdu.getAddress());
    LOG.info("UD              : '{}'", Util.bytesToHexString(pdu.getUDData()));
    LOG.info("UDH             : '{}'", Util.bytesToHexString(pdu.getUDHData()));
    LOG.info("UD (without UDH): '{}'", Util.bytesToHexString(pdu.getUserDataAsBytes()));
    LOG.info("TotalUDHLength  : {}", pdu.getTotalUDHLength());
    if (pdu instanceof SmsDeliveryPdu) {
      final SmsDeliveryPdu smsDeliveryPdu = (SmsDeliveryPdu) pdu;
      LOG.info("TP-MSS          : '{}'", smsDeliveryPdu.hasTpMms());
      LOG.info("TP-SRI          : '{}'", smsDeliveryPdu.hasTpSri());
      LOG.info("TP-RP           : '{}'", smsDeliveryPdu.hasTpRp());
      LOG.info("TP-SCTS         : '{}'", smsDeliveryPdu.getServiceCentreTimestamp());
    }
    return pdu;
  }
}
