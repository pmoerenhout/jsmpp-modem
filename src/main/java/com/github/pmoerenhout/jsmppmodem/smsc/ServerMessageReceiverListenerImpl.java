/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.github.pmoerenhout.jsmppmodem.smsc;

import static org.jsmpp.SMPPConstant.STAT_ESME_RSYSERR;

import java.nio.charset.Charset;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.RandomUtils;
import org.jsmpp.bean.BroadcastSm;
import org.jsmpp.bean.CancelBroadcastSm;
import org.jsmpp.bean.CancelSm;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.QueryBroadcastSm;
import org.jsmpp.bean.QuerySm;
import org.jsmpp.bean.ReplaceSm;
import org.jsmpp.bean.SubmitMulti;
import org.jsmpp.bean.SubmitSm;
import org.jsmpp.bean.UnsuccessDelivery;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.BroadcastSmResult;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.QueryBroadcastSmResult;
import org.jsmpp.session.QuerySmResult;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.session.ServerMessageReceiverListener;
import org.jsmpp.session.Session;
import org.jsmpp.session.SubmitMultiResult;
import org.jsmpp.session.SubmitSmResult;
import org.jsmpp.util.MessageIDGenerator;
import org.jsmpp.util.MessageId;

import com.github.pmoerenhout.jsmppmodem.smsc.task.DeliveryReceiptTask;
import com.github.pmoerenhout.jsmppmodem.util.SmppUtil;
import com.github.pmoerenhout.jsmppmodem.util.Util;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerMessageReceiverListenerImpl implements ServerMessageReceiverListener {

  private static final byte OTA_SMS_DCS = (byte) 0xF6;
  private static final byte OTA_SMS_PID = (byte) 0x7F;

  private MessageIDGenerator messageIDGenerator;
  private Charset charset;
  private long submitSmReplyDelay;

  public ServerMessageReceiverListenerImpl(final Charset charset, final long submitSmReplyDelay) {
    // The SMSC default alphabet
    this.charset = charset;
    this.submitSmReplyDelay = submitSmReplyDelay;
    // messageIDGenerator = new RandomMessageIDGenerator();
    messageIDGenerator = new UuidMessageIDGenerator();

    log.info("SMSC default charset is {}", charset.name());
  }

  @Override
  public SubmitSmResult onAcceptSubmitSm(SubmitSm submitSm, SMPPServerSession source) throws ProcessRequestException {
    log.info("received submit_sm on session {}", source.getSessionId());
    log.info(" SubmitSm command id     : {}", submitSm.getCommandId());
    log.info(" SubmitSm command length : {}", submitSm.getCommandLength());
    log.info(" SubmitSm command status : {}", submitSm.getCommandStatus());
    log.info(" SubmitSm DCS            : {}", Util.bytesToHexString(submitSm.getDataCoding()));
    log.info(" SubmitSm PID            : {}", Util.bytesToHexString(submitSm.getProtocolId()));
    log.info(" SubmitSm Priority       : {}", Util.bytesToHexString(submitSm.getPriorityFlag()));
    log.info(" SubmitSm ESM            : {}", Util.bytesToHexString(submitSm.getEsmClass()));
    log.info(" SubmitSm Service Type   : {}", submitSm.getServiceType());
    log.info(" SubmitSm Validity Period: {}", submitSm.getValidityPeriod());
    log.info(" SubmitSm Scheduled Deliv: {}", submitSm.getScheduleDeliveryTime());
    log.info(" SubmitSm Registered Deli: {}", submitSm.getRegisteredDelivery());
    log.info(" SubmitSm Source         : {} {}/{}", submitSm.getSourceAddr(), submitSm.getSourceAddrTon(), submitSm.getSourceAddrNpi());
    log.info(" SubmitSm Destination    : {} {}/{}", submitSm.getDestAddress(), submitSm.getDestAddrTon(), submitSm.getDestAddrNpi());
    log.info(" SubmitSm shortMessage   : {}", Util.bytesToHexString(submitSm.getShortMessage()));
    final OptionalParameter.OctetString messagePayload = submitSm.getOptionalParameter(OptionalParameter.Message_payload.class);
    if (messagePayload != null) {
      log.info(" SubmitSm Message Payload: {}", Util.bytesToHexString(messagePayload.getValue()));
    }
    try {
      final byte[] message = SmppUtil.getShortMessageOrPayload(submitSm.getShortMessage(), messagePayload);
      if (SmppUtil.isBinary(submitSm.getDataCoding())) {
        log.info(" SubmitSm binary     : {}", Util.bytesToHexString(message));
      } else {
        final String decodedMessage = SmppUtil.decode(submitSm.getDataCoding(), submitSm.getEsmClass(), message, charset);
        log.info(" SubmitSm decoded    : {}", decodedMessage);
      }
      SmppUtil.logOptionalParameters(submitSm.getOptionalParameters(), "submit_sm");

      final MessageId messageId = messageIDGenerator.newMessageId();

      // Schedule a delivery receipt to be send to the originator of the message
      // final long delay = RandomUtils.nextLong(0, 10000);
      final long delay = submitSmReplyDelay >= 0 ? submitSmReplyDelay : RandomUtils.nextLong(0, -1L * submitSmReplyDelay);

      if (SmppUtil.isBinaryMessageClass2(submitSm.getDataCoding()) && submitSm.getProtocolId() == OTA_SMS_PID) {

      } else {
        log.debug("Start the delivery receipt for the short message (text) with an delay of {}ms", delay);
        Executors.newSingleThreadExecutor().submit(new DeliveryReceiptTask(source, submitSm, messageId, charset, delay));
      }
      log.info("Return the messageId {}", messageId);
      SubmitSmResult submitSmResult = new SubmitSmResult(messageId, new OptionalParameter[]{});
      return submitSmResult;
    } catch (InvalidMessagePayloadException e) {
      throw new ProcessRequestException(e.getMessage(), 6000);
    }
  }

  @Override
  public DataSmResult onAcceptDataSm(final DataSm dataSm, final Session source) throws ProcessRequestException {
    log.info("Received data_sm");
    throw new ProcessRequestException("The data_sm is not implemented", STAT_ESME_RSYSERR);
  }

  @Override
  public SubmitMultiResult onAcceptSubmitMulti(final SubmitMulti submitMulti, final SMPPServerSession source) throws ProcessRequestException {
    log.info("Received submit_multi");
    final MessageId messageId = messageIDGenerator.newMessageId();
    return new SubmitMultiResult(messageId.getValue(), new UnsuccessDelivery[]{}, new OptionalParameter[]{});
  }

  @Override
  public QuerySmResult onAcceptQuerySm(QuerySm querySm, SMPPServerSession source) throws ProcessRequestException {
    log.info("Received query_sm");
    throw new ProcessRequestException("The replace_sm is not implemented", STAT_ESME_RSYSERR);
  }

  @Override
  public void onAcceptReplaceSm(ReplaceSm replaceSm, SMPPServerSession source) throws ProcessRequestException {
    log.info("Received replace_sm");
    throw new ProcessRequestException("The replace_sm is not implemented", STAT_ESME_RSYSERR);
  }

  @Override
  public void onAcceptCancelSm(CancelSm cancelSm, SMPPServerSession source)
      throws ProcessRequestException {
    log.info("Received cancel_sm");
    throw new ProcessRequestException("The cancel_sm is not implemented", STAT_ESME_RSYSERR);
  }

  @Override
  public QueryBroadcastSmResult onAcceptQueryBroadcastSm(QueryBroadcastSm queryBroadcastSm, SMPPServerSession source)
      throws ProcessRequestException {
    log.info("Received query_broadcast_sm");
    throw new ProcessRequestException("The query_broadcast_sm is not implemented", STAT_ESME_RSYSERR);
  }

  @Override
  public void onAcceptCancelBroadcastSm(CancelBroadcastSm cancelBroadcastSm, SMPPServerSession source)
      throws ProcessRequestException {
    log.info("Received cancel_broadcast_sm");
    throw new ProcessRequestException("The cancel_broadcast_sm is not implemented", STAT_ESME_RSYSERR);
  }

  @Override
  public BroadcastSmResult onAcceptBroadcastSm(BroadcastSm broadcastSm, SMPPServerSession source)
      throws ProcessRequestException {
    log.info("Received broadcast_sm");
    throw new ProcessRequestException("The broadcast_sm is not implemented", STAT_ESME_RSYSERR);
  }

  private byte[] getUserData(final boolean udhi, final byte[] data) {
    if (udhi) {
      final int udhLength = data[0] + 1;
      final byte[] ud = new byte[data.length - udhLength];
      System.arraycopy(data, udhLength, ud, 0, data.length - udhLength);
      return ud;
    }
    return data;
  }

}
