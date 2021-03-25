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

import com.github.pmoerenhout.jsmppmodem.ApplicationContextProvider;
import com.github.pmoerenhout.jsmppmodem.events.BoundReceiverEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SessionStateListenerImpl implements SessionStateListener {

  private List<SMPPServerSession> sessions;

  public SessionStateListenerImpl(final List<SMPPServerSession> sessions) {
    this.sessions = sessions;
  }

  public void addServerSession(final SMPPServerSession session) {
    log.info("Add server session {}", session);
    this.sessions.add(session);
  }


  public void onStateChange(final SessionState newState, final SessionState oldState, final Session session) {
    log.info("Session {} changed from {} to {}", session.getSessionId(), oldState, newState);
    final SMPPServerSession serverSession = (SMPPServerSession) session;
    if (!sessions.contains(session)) {
      log.info("Session added");
      sessions.add(serverSession);
    }
    if (newState.isBound()) {
      serverSession.setEnquireLinkTimer(15000);
    }
    if (newState == SessionState.CLOSED) {
      log.info("Remove session {}", serverSession.getSessionId());
      final boolean isRemoved = sessions.remove(serverSession);
      if (!isRemoved) {
        log.warn("The session {} could not be removed from the sessions list", serverSession.getSessionId());
      }
    }
    if (newState == SessionState.BOUND_RX || newState == SessionState.BOUND_TRX) {
      log.info("Send BoundReceiverEvent for session {}", serverSession.getSessionId());
      ApplicationContextProvider.getApplicationContext().publishEvent(new BoundReceiverEvent(this, serverSession));
    } else {
      log.info("NOT Send BoundReceiverEvent");
    }
  }
}

