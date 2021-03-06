package org.snapscript.core.scope.instance;

import java.util.Iterator;
import java.util.Set;

import org.snapscript.common.Cache;
import org.snapscript.common.CompoundIterator;
import org.snapscript.common.HashCache;
import org.snapscript.core.InternalStateException;
import org.snapscript.core.scope.State;
import org.snapscript.core.variable.Value;

public class InstanceState implements State {
   
   private final Cache<String, Value> values;
   private final Instance instance;

   public InstanceState(Instance instance) {
      this.values = new HashCache<String, Value>();
      this.instance = instance;
   }

   @Override
   public Iterator<String> iterator() {
      Set<String> keys = values.keySet();
      Iterator<String> inner = keys.iterator();
      
      if(instance != null) {
         State state = instance.getState();
         Iterator<String> outer = state.iterator();
         
         return new CompoundIterator<String>(inner, outer);
      }
      return inner;
   }

   @Override
   public Value get(String name) {
      Value value = values.fetch(name);
      
      if(value == null) {
         State state = instance.getState();
         
         if(state == null) {
            throw new InternalStateException("Scope for '" + name + "' does not exist");
         }
         value = state.get(name);
      }
      return value;
   }
   
   @Override
   public void add(String name, Value value) {
      Value variable = values.fetch(name);

      if(variable != null) {
         throw new InternalStateException("Variable '" + name + "' already exists");
      }
      values.cache(name, value); 
   }
   
   @Override
   public String toString() {
      return String.valueOf(values);
   }
}