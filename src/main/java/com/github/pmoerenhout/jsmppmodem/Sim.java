package com.github.pmoerenhout.jsmppmodem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Sim {

  private String iccid;
  private String imsi;
  private String subscriberNumber;
  private String pin1;
  private String puk1;

}
