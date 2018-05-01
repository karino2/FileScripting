package org.snapscript.tree;

import org.snapscript.parse.StringToken;

public class WildQualifier implements Qualifier {

   private final StringToken[] tokens;
   private final int count;

   public WildQualifier(StringToken... tokens) {
      this.count = tokens.length;
      this.tokens = tokens;
   }
   
   @Override
   public String[] getSegments() {
      String[] segments = new String[count];

      for (int i = 0; i < count; i++) {
         segments[i] = tokens[i].getValue();
      }
      return segments;
   }

   @Override
   public String getQualifier() {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < count; i++) {
         String value = tokens[i].getValue();

         if (i > 0) {
            builder.append(".");
         }
         builder.append(value);
      }
      return builder.toString();
   }

   @Override
   public String getLocation() {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < count; i++) {
         String value = tokens[i].getValue();
         char first = value.charAt(0);

         if(first >='A' && first <='Z') {
            return builder.toString();
         }
         if (i > 0) {
            builder.append(".");
         }
         builder.append(value);
      }
      return builder.toString();
   }

   @Override
   public String getTarget() {
      StringBuilder builder = new StringBuilder();

      for (int i = 1; i < count; i++) {
         String value = tokens[i].getValue();
         char first = value.charAt(0);

         if(first >='A' && first <='Z') {
            builder.append(value);
            
            while(++i < count) {
               value = tokens[i].getValue();
               first = value.charAt(0);
               
               if(first <'A' || first >'Z') {
                  return builder.toString();
               }
               builder.append("$");
               builder.append(value);
            }
            return builder.toString();
         }
      }
      return null;
   }
   
   @Override
   public String getName() {
      return null;
   }
}