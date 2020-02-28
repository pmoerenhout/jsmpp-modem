package com.github.pmoerenhout.jsmppmodem;

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
      LOG.error("Error starting the modem service ){})", e.getClass().getName());
      LOG.error("Error starting the modem service ){})", e.getClass().getName());
      LOG.error("Error starting the modem service ){})", e.getClass().getName());
      LOG.error("Error starting the modem service ){})", e.getClass().getName());
    }
    smppService.start();

    final Modem modem = modemService.getFirstModem();
    LOG.info("Is first modem {} initialized? {}", modem.getId(), modem.isInitialized());

    if (modem.isInitialized()) {
      modemService.showAllMessages(modem);

      modemService.sendAllMessagesViaSmpp(modem);
    }

    // modemService.deleteAllMessage(modem);
    // modemService.send(modem, "31635778003", 1);
    // modemService.send(modem, "31614240689", 1);
    // modemService.send(modem, "31635930247", 100);
    // modemService.send("31687263195", 10);

    for (int i = 0; i < 5000; i++) {
      Thread.sleep(20000);
//      smppService.stop();
//      Thread.sleep(5000);
//      smppService.start();
      //modemService.showAllMessages(modem);
    }
    smppService.stop();
    modemService.close();
  }

}
