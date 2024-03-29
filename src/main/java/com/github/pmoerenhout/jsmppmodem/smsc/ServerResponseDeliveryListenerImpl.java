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

import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.session.ServerResponseDeliveryListener;
import org.jsmpp.session.SubmitMultiResult;
import org.jsmpp.session.SubmitSmResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerResponseDeliveryListenerImpl implements ServerResponseDeliveryListener {

  public void onSubmitSmRespSent(SubmitSmResult submitSmResult, SMPPServerSession source) {
    log.info("submit_sm_resp_sent with message id {} on session {}", submitSmResult.getMessageId(), source.getSessionId());
  }

  @Override
  public void onSubmitSmRespError(SubmitSmResult submitSmResult, Exception cause, SMPPServerSession source) {
    log.info("submit_sm_resp_error with message id {} on session {}: {}", submitSmResult.getMessageId(), source.getSessionId(), cause.getMessage());
  }

  @Override
  public void onSubmitMultiRespSent(SubmitMultiResult submitMultiResult, SMPPServerSession source) {
    log.info("submit_multi_resp_sent");
  }

  @Override
  public void onSubmitMultiRespError(SubmitMultiResult submitMultiResult, Exception cause, SMPPServerSession source) {
    log.info("submit_multi_resp_error");
  }

}
