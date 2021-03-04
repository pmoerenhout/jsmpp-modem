package com.github.pmoerenhout.jsmppmodem.smsc;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CharsetService {

  public static void showAll() {
    final SortedMap availableCharsets = Charset.availableCharsets();
    final Iterator i = availableCharsets.keySet().iterator();
    while (i.hasNext()) {
      final String name = (String) i.next();
      final Charset e = (Charset) availableCharsets.get(name);
      final String displayName = e.displayName();
      final Set s = e.aliases();
      final Iterator j = s.iterator();
      final StringBuilder sb = new StringBuilder(1024);
      while (j.hasNext()) {
        sb.append((String) j.next());
        sb.append(", ");
      }
      log.debug("{}, {}, {} {}", name, displayName, sb.toString().trim(), (e.canEncode() ? "can encode" : ", CANNOT ENCODE"));
    }
  }

  public static Charset getCharset(final String charsetName) {
    // First try Java charsets (and loaded META-INF/services/java.nio.charset.spi.CharsetProvider)
    if (Charset.isSupported(charsetName)) {
      return Charset.forName(charsetName);
    }
    // Try Freenet charsets (CCGSM, CCPGSM, etc)
    final Charset charset1 = new net.freeutils.charset.CharsetProvider().charsetForName(charsetName);
    if (charset1 != null) {
      return charset1;
    }
    // I tried... but no luck
    return null;
  }
}
