package com.github.pmoerenhout.jsmppmodem.smsc;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharsetService {

  private static final Logger LOG = LoggerFactory.getLogger(CharsetService.class);

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
      LOG.debug("{}, {}, {} {}", name, displayName, sb.toString().trim(), (e.canEncode() ? "can encode" : ", CANNOT ENCODE"));
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
