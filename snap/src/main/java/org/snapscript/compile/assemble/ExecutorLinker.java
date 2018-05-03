package org.snapscript.compile.assemble;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import org.snapscript.core.Context;
import org.snapscript.core.link.ExceptionPackage;
import org.snapscript.core.link.FuturePackage;
import org.snapscript.core.link.Package;
import org.snapscript.core.link.PackageLinker;
import org.snapscript.core.module.Path;

public class ExecutorLinker implements PackageLinker {
   
   private final ConcurrentMap<Path, Package> registry;
   private final PackageLinker linker;
   private final Executor executor;
   
   public ExecutorLinker(Context context) {
      this(context, null);
   }
   
   public ExecutorLinker(Context context, Executor executor) {
      this.registry = new ConcurrentHashMap<Path, Package>();
      this.linker = new ApplicationLinker(context, executor);
      this.executor = executor;
   }

   @Override
   public Package link(Path path, String source) throws Exception {
      if(executor != null) {
         Executable executable = new Executable(path, source);
         FutureTask<Package> task = new FutureTask<Package>(executable);
         FuturePackage result = new FuturePackage(task, path);
         
         if(registry.putIfAbsent(path, result) == null) {
            executor.execute(task); 
            return result;
         }
         return registry.get(path);
      }
      return linker.link(path, source);
   }

   @Override
   public Package link(Path path, String source, String grammar) throws Exception {
      if(executor != null) {
         Executable executable = new Executable(path, source, grammar);
         FutureTask<Package> task = new FutureTask<Package>(executable);
         FuturePackage result = new FuturePackage(task, path);
         
         if(registry.putIfAbsent(path, result) == null) {
            executor.execute(task); 
            return result;
         }
         return registry.get(path);
      }
      return linker.link(path, source, grammar);
   }

   @Override
   public void purge(Path path) {
      registry.remove(path);
      linker.purge(path);
   }

   private class Executable implements Callable<Package> {      
      
      private final String grammar;
      private final String source;  
      private final Path path;
      
      public Executable(Path path, String source) {
         this(path, source, null);
      }
      
      public Executable(Path path, String source, String grammar) {
         this.grammar = grammar;
         this.source = source;
         this.path = path;
      }

      @Override
      public Package call() {
         try {               
            if(grammar != null) {
               return linker.link(path, source, grammar);
            }
            return linker.link(path, source);
         } catch(Exception cause) {
            return new ExceptionPackage("Could not link '" + path +"'", cause);
         } 
      }            
   }
}