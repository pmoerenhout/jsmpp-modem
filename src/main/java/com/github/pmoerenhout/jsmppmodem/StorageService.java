package com.github.pmoerenhout.jsmppmodem;

import java.time.Instant;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.github.pmoerenhout.jsmppmodem.events.ReceivedSmsDeliveryPduEvent;
import com.github.pmoerenhout.jsmppmodem.jpa.model.Deliver;
import com.github.pmoerenhout.jsmppmodem.jpa.repository.DeliverRepository;
import com.github.pmoerenhout.jsmppmodem.util.Util;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StorageService {

  @Autowired
  private DeliverRepository deliverRepository;

  @EventListener
  public void handleReceivedSmsDeliveryPdu(final ReceivedSmsDeliveryPduEvent event) throws Exception {
    log.debug("Received SMS-DELIVERY PDU: {}", Util.bytesToHexString(event.getPdu()));
    final Deliver saved = deliverRepository.save(getDeliver(event.getTimestamp(), event.getConnectionId(), event.getPdu()));
    log.debug("The sms delivery message was saved to database with id {}", saved.getId());
  }

  public void save(final long timestamp, final String connectionId, final byte[] pduBytes) {
    final Deliver saved = deliverRepository.save(getDeliver(timestamp, connectionId, pduBytes));
    log.debug("The message was saved to database with id {}", saved.getId());
  }

  public Stream<Deliver> streamAll() {
    return deliverRepository.streamAll();
  }

  private Deliver getDeliver(final long timestamp, final String connectionId, final byte[] pduBytes) {
    final Deliver deliver = new Deliver();
    deliver.setTimestamp(Instant.ofEpochMilli(timestamp));
    deliver.setConnectionId(connectionId);
    deliver.setPdu(pduBytes);
    return deliver;
  }
}
