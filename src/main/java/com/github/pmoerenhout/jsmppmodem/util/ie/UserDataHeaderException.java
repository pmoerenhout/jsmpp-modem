package com.github.pmoerenhout.jsmppmodem.util.ie;

public class UserDataHeaderException extends Exception {

  private static final long serialVersionUID = -351667036146851672L;

  public UserDataHeaderException() {
  }

  public UserDataHeaderException(final String message) {
    super(message);
  }

  public UserDataHeaderException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public UserDataHeaderException(final Throwable cause) {
    super(cause);
  }

  public UserDataHeaderException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}