package org.snapscript.common;

public class CompleteProgress<T extends Enum> implements Progress<T> {
   
   @Override
   public boolean done(T phase) {
      return false;
   }

   @Override
   public boolean wait(T phase) {
      return true;
   }

   @Override
   public boolean wait(T phase, long duration) {
      return true;
   }
}