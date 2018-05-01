package org.snapscript.core.platform;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicReference;

import org.snapscript.core.ContextClassLoader;
import org.snapscript.core.convert.proxy.ProxyWrapper;
import org.snapscript.core.function.index.FunctionIndexer;
import org.snapscript.core.type.Any;

public class PlatformClassLoader {
   
   private final AtomicReference<Constructor> reference;
   private final PlatformNameBuilder builder;
   private final ClassLoader loader;
   
   public PlatformClassLoader() {
      this.reference = new AtomicReference<Constructor>();
      this.loader = new ContextClassLoader(Any.class);
      this.builder = new PlatformNameBuilder();
   }

   public Constructor loadConstructor(){
      Constructor constructor = reference.get();
      
      if(constructor == null) {
         try {
            PlatformType platform = PlatformType.resolveType();
            String type = builder.createFullName(platform);
            Class value = loader.loadClass(type);
            
            constructor = value.getDeclaredConstructor(FunctionIndexer.class, ProxyWrapper.class);
            reference.set(constructor);
         }catch(Exception e) {
            throw new IllegalStateException("Could not load constructor", e);
         }
      }
      return constructor;
   }
}