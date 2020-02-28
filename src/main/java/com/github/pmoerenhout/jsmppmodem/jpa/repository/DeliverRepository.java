package com.github.pmoerenhout.jsmppmodem.jpa.repository;

import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.github.pmoerenhout.jsmppmodem.jpa.model.Deliver;

@Repository
public interface DeliverRepository extends JpaRepository<Deliver, String> {

  // List<Deliver> findByOriginatingAddress(String originatingAddress);

  // @QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE))
  @Query(value = "select d from Deliver d")
  Stream<Deliver> streamAll();

  //Stream<Usage> readAll();
}
