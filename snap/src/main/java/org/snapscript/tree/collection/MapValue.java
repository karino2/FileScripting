package org.snapscript.tree.collection;

import java.util.Map;

import org.snapscript.core.convert.proxy.ProxyWrapper;
import org.snapscript.core.variable.Value;

public class MapValue extends Value {
   
   private final ProxyWrapper wrapper;
   private final Object key;
   private final Map map;
   
   public MapValue(ProxyWrapper wrapper, Map map, Object key) {
      this.wrapper = wrapper;
      this.key = key;
      this.map = map;
   }
   
   @Override
   public Class getType() {
      return Object.class;
   }
   
   @Override
   public Object getValue(){
      Object value = map.get(key);
      
      if(value != null) {
         return wrapper.fromProxy(value);
      }
      return value;
   }
   
   @Override
   public void setValue(Object value){
      Object proxy = wrapper.toProxy(value);
      
      if(value != null) {
         map.put(key, proxy);
      } else {
         map.remove(key);
      }
   }       
   
   @Override
   public String toString() {
      return String.valueOf(key);
   }
}