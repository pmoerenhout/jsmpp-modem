package com.github.pmoerenhout.jsmppmodem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.github.pmoerenhout.atcommander.api.SerialException;
import com.github.pmoerenhout.atcommander.basic.exceptions.ResponseException;
import com.github.pmoerenhout.atcommander.basic.exceptions.TimeoutException;
import com.github.pmoerenhout.atcommander.module._3gpp.EtsiModem;
import com.github.pmoerenhout.atcommander.module._3gpp.RegistrationState;
import com.github.pmoerenhout.atcommander.module._3gpp.commands.NetworkRegistrationResponse;
import com.github.pmoerenhout.atcommander.module._3gpp.commands.OperatorSelectionResponse;
import com.github.pmoerenhout.atcommander.module._3gpp.types.IndexPduMessage;
import com.github.pmoerenhout.atcommander.module._3gpp.types.SignalQuality;
import com.github.pmoerenhout.atcommander.module.v250.enums.AccessTechnology;
import com.github.pmoerenhout.jsmppmodem.smsc.SmppService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Bla implements ApplicationRunner {

  @Autowired
  private ModemService modemService;

  @Autowired
  private SmppService smppService;

  @Override
  public void run(final ApplicationArguments args) {
    log.info("Run with arguments {}", args);
    try {
      modemService.init();

      smppService.start();

      final Modem modem = modemService.getFirstModem();
      log.info("Is first modem {} initialized? {}", modem.getId(), modem.isInitialized());

      final boolean initialized = modemService.waitForInitialisation(60000);
      if (!initialized) {
        log.warn("Initialisation failed");
        return;
      }

      // modemService.showAllMessages(modem);
      // modemService.sendAllMessagesViaSmpp(modem);

      log.info("STORE");
      modemService.storeAllMessages(modem);
      log.info("DONE");

      String imsi = modem.get3gppModem().getInternationalMobileSubscriberIdentity();

      modemService.deleteAllMessage(modem);
      // modemService.send(modem, "31635778003", 1);
      // modemService.send(modem, "31614240689", 1);
      // modemService.send(modem, "31645152740", 1);
      Thread.sleep(5000);
      // modemService.send(modem, "3197014268566", 1);
      // modemService.send(modem, "31635930247", 100);
      // modemService.send("31687263195", 10);
      // modemService.send(modem, "31682346962", 1);

      if (true && "204080151466084".equals(imsi)) {
        // modemService.sendText(modem, "31638031041", "Marie-Louise, dit is een SMS van de PIM applicatie!");
        // modemService.sendText(modem, "31614240689", "Pim, dit is een SMS van de PIM applicatie!");
        modemService.sendText(modem, "1266", "SALDO");

//        Thread.sleep(5000);
//        sendUssd(modem.get3gppModem(), "*100#");

        Thread.sleep(15000);
        sendUssd(modem.get3gppModem(), "*149#");

        Thread.sleep(15000);
        sendUssd(modem.get3gppModem(), "*101#");

        Thread.sleep(15000);
        sendUssd(modem.get3gppModem(), "*107#");
//        IntStream.rangeClosed(100,123).forEach(
//            IntConsumerWithThrowable.castIntConsumerWithThrowable(i -> {
//          Thread.sleep(1000);
//          sendUssd(modem.get3gppModem(), "*" + i + "#");
//        }));
//        Thread.sleep(5000);
//        sendUssd(modem.get3gppModem(), "*107#");
//        Thread.sleep(5000);
//        modem.get3gppModem().setUssd(1, "*111#");
      }

      if ("222013410016127".equals(imsi)) {
        // smodemService.sendBinary(modem, Collections.singletonList("3197014268566"));
        modemService.sendBinary(modem, Collections.singletonList("31682346962"));
      }

      // T-Mobile Prepaid
      if ("204163404833090".equals(imsi)) {
        Thread.sleep(10000);
        modemService.sendText(modem, "1144", "SALDO");
        Thread.sleep(10000);
        modemService.sendText(modem, "31614240689", String.format("Pim, dit is een SMS van de PIM applicatie! IMSI is %s", imsi));
      }

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

      while (!Thread.currentThread().isInterrupted()) {
        Thread.sleep(60000);
        try {
          final EtsiModem etsiModem = modem.get3gppModem();
          imsi = etsiModem.getInternationalMobileSubscriberIdentity();
          log.info("IMSI: {}", imsi);
          final NetworkRegistrationResponse networkRegistration = etsiModem.getNetworkRegistration();
          final RegistrationState registrationState = networkRegistration.getRegistrationState();
          if (registrationState == RegistrationState.REGISTERED_HOME_NETWORK || registrationState == RegistrationState.REGISTERED_ROAMING) {
            if (networkRegistration.getLac().isPresent() && networkRegistration.getCellId().isPresent()) {
              log.info("Network registration: {} (LAC:{} CID:{})",
                  networkRegistration.getRegistrationState(),
                  networkRegistration.getLac().get(), networkRegistration.getCellId().get());
            } else {
              log.info("Network registration: {}", networkRegistration.getRegistrationState());
            }
          } else {
            log.warn("Network registration: {}", networkRegistration.getRegistrationState());
          }
          final SignalQuality signalQuality = etsiModem.getSignalQuality();
          log.info("Signal quality: RSSI:{} BER:{}", signalQuality.getRssi(), signalQuality.getBer());

          final OperatorSelectionResponse operatorSelection = etsiModem.getOperatorSelection();
          if (operatorSelection != null) {
            if (operatorSelection.getAct() != null) {
              log.info("Operator: '{}' Access Technology: {} ({})", operatorSelection.getOper(),
                  AccessTechnology.fromInt(operatorSelection.getAct()), operatorSelection.getAct());
            } else {
              log.info("Operator: '{}' Access Technology: {}", operatorSelection.getOper(), operatorSelection.getAct());
            }
          }

        } catch (final TimeoutException e) {
          log.info("Fetching the network registration failed: {}", e.getMessage());
        }
//      smppService.stop();
//      Thread.sleep(5000);
//      smppService.start();
        //modemService.showAllMessages(modem);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      log.error("Error running the modem service", e);
    }
    smppService.stop();
    modemService.close();
  }

  private void sendUssd(final EtsiModem modem, final String ussdString) throws
      ResponseException, TimeoutException, SerialException {
      log.info("Send USSD {}", ussdString);
      modem.setUssd(1, ussdString);
  }

  private void trySendUssd(final EtsiModem modem, final String ussdString)   {
    try {
      modem.setUssd(1, ussdString);
    } catch (ResponseException|  TimeoutException| SerialException e){
      log.error("Error executing USSD " + ussdString, e);
    }
  }

  @Scheduled(initialDelay = 60000, fixedRate = 60000)
  public void scheduleCleanup() {
    log.info("scheduleCleanup");
    final List<Modem> modems = modemService.getModems();
    for (Modem modem : modems) {
      try {
        final MessageService messageService = modem.getMessageService();
        final List<IndexPduMessage> messages = messageService.getAllMessages();
        log.info("Modem {} has {} messages", modem.getId(), messages.size());
        if (messages.size() > 200) {
          log.info("Delete all messages from modem {}", modem.getId());
          messageService.deleteAllMessages();
        }
      } catch (Exception e) {
        log.error("Error fetching messages", e);
      }
    }
//    modems.stream().map(Modem::getMessageService).forEach(ms ->{
//      log.info("Modem has {} messages", ms.getAllMessages().size());
//    });
  }

}
