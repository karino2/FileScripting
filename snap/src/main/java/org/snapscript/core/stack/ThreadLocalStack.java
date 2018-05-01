package org.snapscript.core.stack;

public class ThreadLocalStack extends ThreadLocal<StackTrace> {

   @Override
   public StackTrace initialValue() {
      return new StackTrace();
   }
}