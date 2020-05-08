package com.github.pmoerenhout.jsmppmodem;

import java.util.ArrayList;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.github.pmoerenhout.jsmppmodem.smsc.SmppService;

@Component
public class Bla implements CommandLineRunner {

  final static Logger LOG = LoggerFactory.getLogger(Bla.class);

  @Autowired
  private ModemService modemService;

  @Autowired
  private SmppService smppService;

  public void run(final String... args) throws Exception {
    LOG.info("Run...");
    try {
      modemService.init();
    } catch (Exception e){
      LOG.error("Error starting the modem service ){})", e.getClass().getName());
    }
    smppService.start();

    final Modem modem = modemService.getFirstModem();
    LOG.info("Is first modem {} initialized? {}", modem.getId(), modem.isInitialized());

    final boolean initialized = modemService.waitForInitialisation(60000);
    if (!initialized){
      LOG.warn("Initialisation failed");
      return;
    }

    // modemService.showAllMessages(modem);
    // modemService.sendAllMessagesViaSmpp(modem);

    LOG.info("STORE");
    modemService.storeAllMessages(modem);
    LOG.info("DONE");

    // modemService.deleteAllMessage(modem);
    // modemService.send(modem, "31635778003", 1);
    // modemService.send(modem, "31614240689", 1);
    // modemService.send(modem, "31645152740", 1);
    Thread.sleep(1000);
     // modemService.send(modem, "3197014268566", 1);
    // modemService.send(modem, "31635930247", 100);
    // modemService.send("31687263195", 10);
    // modemService.send(modem, "31682346962", 1);
    modemService.sendBinary(modem, Collections.singletonList("31682346962"));

    final ArrayList<String> destinations = new ArrayList<>();
    destinations.add("31682346962");

    destinations.add("447904552724");
    destinations.add("447475496158");
    destinations.add("447743427001");
    destinations.add("447552987911");
    destinations.add("31620293340");
    destinations.add("31614209963");
    destinations.add("31626630781");
    destinations.add("31627548867");
    destinations.add("491772422542");
    destinations.add("4915126703686");
    destinations.add("4915205291784");
    destinations.add("393926163994");
    destinations.add("393336957100");
    destinations.add("393403867982");
    destinations.add("393274282152");
    destinations.add("46700949287");
    destinations.add("46763252390");
    destinations.add("46721866354");
    destinations.add("46702069873");
    destinations.add("4531579521");
    destinations.add("4551356583");
    destinations.add("4581906432");
    destinations.add("4526447288");
    destinations.add("33755294040");
    destinations.add("33769799845");
    destinations.add("33688522771");
    destinations.add("33611389588");
    destinations.add("436601466401");
    destinations.add("4366499461330");
    destinations.add("436767869013");
    destinations.add("4915145805523");
    destinations.add("972522673119");
    destinations.add("48508108362");
    destinations.add("48790594510");
    destinations.add("48667185658");
    destinations.add("48692000845");
    destinations.add("32488155283");
    destinations.add("32493175556");
    destinations.add("32474208645");

    // modemService.sendBinary(modem, destinations);

    for (int i = 0; i < 5000; i++) {
      Thread.sleep(60000);
//      smppService.stop();
//      Thread.sleep(5000);
//      smppService.start();
      //modemService.showAllMessages(modem);
    }
    smppService.stop();
    modemService.close();
  }

}
