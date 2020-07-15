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
    this.sessions = sessions;
  }

  public void addServerSession(final SMPPServerSession session) {
    LOG.info("Add server session {}", session);
    this.sessions.add(session);
  }

  public void onStateChange(final SessionState newState, final SessionState oldState, final Session session) {
    LOG.info("Session {} changed from {} to {}", session.getSessionId(), oldState, newState);
    SMPPServerSession serverSession = (SMPPServerSession) session;
    if (!sessions.contains(session)) {
      sessions.add(serverSession);
    }
    if (newState.isBound()) {
      serverSession.setEnquireLinkTimer(15000);
    }
    if (newState == SessionState.CLOSED) {
      final boolean isRemoved = sessions.remove(session);
      if (!isRemoved) {
        LOG.warn("The session {} could not be removed from the sessions list", session.getSessionId());
      }
    }
    if (newState == SessionState.BOUND_RX || newState == SessionState.BOUND_TRX) {
      LOG.info("Send BoundReceiverEvent");
      ApplicationContextProvider.getApplicationContext().publishEvent(new BoundReceiverEvent(this, serverSession));
    } else {
      LOG.info("NOT Send BoundReceiverEvent");
    }
  }
}

