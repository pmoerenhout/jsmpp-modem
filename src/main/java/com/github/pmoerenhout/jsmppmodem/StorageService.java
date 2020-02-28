package com.github.pmoerenhout.jsmppmodem;

import java.time.Instant;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.github.pmoerenhout.jsmppmodem.events.ReceivedPduEvent;
import com.github.pmoerenhout.jsmppmodem.jpa.model.Deliver;
import com.github.pmoerenhout.jsmppmodem.jpa.repository.DeliverRepository;

@Service
public class StorageService {

  private static final Logger LOG = LoggerFactory.getLogger(StorageService.class);

  @Autowired
  private DeliverRepository deliverRepository;

  @Autowired
  private ApplicationEventPublisher applicationEventPublisher;

  @EventListener
  public void handleReceivedPduEvent(final ReceivedPduEvent event) throws Exception {
    LOG.info("handleReceivedPduEvent: {}", event.getPdu());
    final Deliver saved = deliverRepository.save(getDeliver(event.getTimestamp(), event.getPdu()));
    LOG.debug("The message was saved to database with id {}", saved.getId());
  }

  public void save(final long timestamp, final byte[] pduBytes) {
    final Deliver saved = deliverRepository.save(getDeliver(timestamp, pduBytes));
    LOG.debug("The message was saved to database with id {}", saved.getId());
  }

  public Stream<Deliver> streamAll() {
    return deliverRepository.streamAll();
  }

  private Deliver getDeliver(final long timestamp, final byte[] pduBytes) {
    final Deliver deliver = new Deliver();
    deliver.setTimestamp(Instant.ofEpochMilli(timestamp));
    deliver.setPdu(pduBytes);
    return deliver;
  }
}
