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

import java.util.List;

import org.jsmpp.extra.SessionState;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.session.Session;
import org.jsmpp.session.SessionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pmoerenhout.jsmppmodem.ApplicationContextProvider;
import com.github.pmoerenhout.jsmppmodem.events.BoundReceiverEvent;

public class SessionStateListenerImpl implements SessionStateListener {

  private static final Logger LOG = LoggerFactory.getLogger(SessionStateListenerImpl.class);

  private List<SMPPServerSession> sessions;

  public SessionStateListenerImpl(final List<SMPPServerSession> sessions) {
    LOG.info("Sessions {}", sessions.getClass().getName());
    this.sessions = sessions;
  }

  public void onStateChange(SessionState newState, SessionState oldState, Session source) {
    LOG.info("Session {} changed from {} to {}", source.getSessionId(), oldState, newState);
    if (newState == SessionState.CLOSED) {
      final boolean isRemoved = sessions.remove(source);
      if (!isRemoved) {
        LOG.warn("The session {} could not be removed from the sessions list", source.getSessionId());
      }
    }
    if (newState == SessionState.BOUND_RX || newState == SessionState.BOUND_TRX) {
      ApplicationContextProvider.getApplicationContext().publishEvent(new BoundReceiverEvent(this));
    }
  }
}

