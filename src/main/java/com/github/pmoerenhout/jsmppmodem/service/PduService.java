package com.github.pmoerenhout.jsmppmodem.service;

import com.github.pmoerenhout.jsmppmodem.util.Util;
import com.github.pmoerenhout.pduutils.MsIsdn;
import com.github.pmoerenhout.pduutils.gsm0340.Pdu;
import com.github.pmoerenhout.pduutils.gsm0340.PduFactory;
import com.github.pmoerenhout.pduutils.gsm0340.PduGenerator;
import com.github.pmoerenhout.pduutils.gsm0340.PduParser;
import com.github.pmoerenhout.pduutils.gsm0340.SmsDeliveryPdu;
import com.github.pmoerenhout.pduutils.gsm0340.SmsSubmitPdu;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PduService {

  public static byte[] createSmsSubmitPdu(final String address, final String text) {
    final PduGenerator pduGenerator = new PduGenerator();
    final SmsSubmitPdu pdu = PduFactory.newSmsSubmitPdu();
    pdu.setMessageReference(0);
//    pdu.setSmscAddress("316540998300");
//    pdu.setSmscAddressType(17);
    if (address.length() <= 5) {
      pdu.setAddress(new MsIsdn(address, MsIsdn.Type.NATIONAL));
      //pdu.setAddressType(0x00); // KPN
      pdu.setAddressType(0x21);
    } else {
      pdu.setAddress(new MsIsdn(address));
      pdu.setAddressType(0x11);
    }
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
    log.info("HEX: {}", hex);
    final Pdu pdu = pduParser.parsePdu(hex);
    log.info("Message: PDU:{} ({})", hex, pdu.getClass().getSimpleName());
    if (pdu instanceof SmsDeliveryPdu) {
      log.info("DELIVERY: SMSC:{} ADDRESS:{} DCS:{} PID:{} TEXT:'{}'", pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(), pdu.getProtocolIdentifier(),
          pdu.getDecodedText());
    } else {
      log.info(" SMSC:{} ADDRESS:{} DCS:{} PID:{} TEXT:'{}'", pdu.getSmscAddress(), pdu.getAddress(), pdu.getDataCodingScheme(), pdu.getProtocolIdentifier());
    }
    log.info("SMSC            : 0x{} '{}'", String.format("%02X", pdu.getSmscAddressType()), pdu.getSmscAddress());
    log.info("TP-MTI          : 0x{}", String.format("%02X", pdu.getTpMti()));
    log.info("TP-UDHI         : {}", pdu.hasTpUdhi());
    log.info("TP-DCS          : 0x{}", String.format("%02X", pdu.getDataCodingScheme()));
    log.info("TP-PID          : 0x{}", String.format("%02X", pdu.getProtocolIdentifier()));
    log.info("Orig Address    : 0x{} '{}'", String.format("%02X", pdu.getAddressType()), pdu.getAddress());
    log.info("UD              : {}", Util.bytesToHexString(pdu.getUDData()));
    log.info("UDH             : {}", Util.bytesToHexString(pdu.getUDHData()));
    if (pdu.getUDData() != null) {
      log.info("UD (without UDH): {}", Util.bytesToHexString(pdu.getUserDataAsBytes()));
      log.info("TotalUDHLength  : {}", pdu.getTotalUDHLength());
    }
    if (pdu instanceof SmsDeliveryPdu) {
      final SmsDeliveryPdu smsDeliveryPdu = (SmsDeliveryPdu) pdu;
      log.info("TP-MSS          : {}", smsDeliveryPdu.hasTpMms());
      log.info("TP-SRI          : {}", smsDeliveryPdu.hasTpSri());
      log.info("TP-RP           : {}", smsDeliveryPdu.hasTpRp());
      log.info("TP-SCTS         : {}", smsDeliveryPdu.getServiceCentreTimestamp());
    }
    return pdu;
  }
}
