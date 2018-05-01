package org.snapscript.common.io;

public class StatementException extends RuntimeException {
   
   public StatementException(String message) {
      super(message);
   }
   
   public StatementException(String message, Throwable cause) {
      super(message, cause);
   }
}