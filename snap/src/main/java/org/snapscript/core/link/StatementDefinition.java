package org.snapscript.core.link;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

import org.snapscript.core.Context;
import org.snapscript.core.NoStatement;
import org.snapscript.core.Statement;
import org.snapscript.core.module.Module;
import org.snapscript.core.module.ModuleRegistry;
import org.snapscript.core.module.Path;
import org.snapscript.core.scope.Scope;

public class StatementDefinition implements PackageDefinition {

   private final AtomicReference<Statement> reference;
   private final Statement statement;
   private final Statement empty;
   private final String name;
   private final Path path;
   
   public StatementDefinition(Statement statement, Path path, String name) {
      this.reference = new AtomicReference<Statement>();
      this.empty = new NoStatement();
      this.statement = statement;
      this.name = name;
      this.path = path;
   }

   @Override
   public Statement define(Scope scope, Path from) throws Exception {
      if(!path.equals(from)) { // don't import yourself
         Statement value = reference.get();
         
         if(value == null) {
            Executable executable = new Executable(scope);
            FutureTask<Statement> task = new FutureTask<Statement>(executable);
            FutureStatement result = new FutureStatement(task, path);
            
            if(reference.compareAndSet(null, result)) {
               task.run();
               return result;
            }
         }
         return reference.get(); // return future package
      }
      return empty;
   }
   
   private class Executable implements Callable<Statement> {
      
      private final Scope scope;
      
      public Executable(Scope scope) {
         this.scope = scope;
      }

      @Override
      public Statement call() throws Exception {
         try {
            Module module = scope.getModule();
            Context context = module.getContext();
            ModuleRegistry registry = context.getRegistry();
            Module library = registry.addModule(name);
            Scope inner = library.getScope();
            
            statement.define(inner);
         } catch(Exception cause) {
            return new ExceptionStatement("Error occured compiling '" + path + "'", cause);
         }
         return statement;
      }
   }
}