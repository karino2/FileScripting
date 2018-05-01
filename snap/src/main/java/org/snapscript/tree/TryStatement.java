package org.snapscript.tree;

import static org.snapscript.core.error.Reason.THROW;

import org.snapscript.core.Compilation;
import org.snapscript.core.Context;
import org.snapscript.core.Execution;
import org.snapscript.core.Statement;
import org.snapscript.core.constraint.Constraint;
import org.snapscript.core.error.ErrorHandler;
import org.snapscript.core.module.Module;
import org.snapscript.core.module.Path;
import org.snapscript.core.result.Result;
import org.snapscript.core.scope.Scope;
import org.snapscript.core.yield.Resume;
import org.snapscript.core.yield.Yield;

public class TryStatement implements Compilation {
   
   private final CatchBlockList list;
   private final Statement statement;
   private final Statement finish;
   
   public TryStatement(Statement statement, Statement finish) {
      this(statement, null, finish);
   }
   
   public TryStatement(Statement statement, CatchBlockList list) {
      this(statement, list, null);
   }
   
   public TryStatement(Statement statement, CatchBlockList list, Statement finish) {
      this.statement = statement;
      this.finish = finish;  
      this.list = list;
   } 

   @Override
   public Object compile(Module module, Path path, int line) throws Exception {
      Context context = module.getContext();
      ErrorHandler handler = context.getHandler();
      
      return new CompileResult(handler, statement, list, finish);
   }

   private static class CompileResult extends Statement {
   
      private final ErrorHandler handler;
      private final CatchBlockList list;
      private final Statement statement;
      private final Statement finish;
      
      public CompileResult(ErrorHandler handler, Statement statement, CatchBlockList list, Statement finish) {
         this.statement = statement;
         this.handler = handler;
         this.finish = finish;  
         this.list = list;
      }    
      
      @Override
      public boolean define(Scope scope) throws Exception {  
         statement.define(scope);
         
         if(list != null) {
            list.define(scope);
         }
         if(finish != null) {
            finish.define(scope);
         }
         return true;
      }
      
      @Override
      public Execution compile(Scope scope, Constraint returns) throws Exception {
         Execution body = statement.compile(scope, returns);
         Execution after = null;
         
         if(list != null) {
            list.compile(scope);
         }
         if(finish != null) {
            after = finish.compile(scope, returns); // should not return
         }         
         return new CompileExecution(handler, body, list, after);
      }
   }
   
   private static class CompileExecution extends SuspendStatement<Resume>  {
      
      private final StatementResume statement;
      private final ErrorHandler handler;
      private final Execution finish;
      private final CatchBlockList list;
      
      public CompileExecution(ErrorHandler handler, Execution statement, CatchBlockList list, Execution finish) {
         this.statement = new StatementResume(statement);
         this.handler = handler;
         this.finish = finish;  
         this.list = list;
      }    
   
      @Override
      public Result execute(Scope scope) throws Exception {
         return resume(scope, statement);
      }
      
      @Override
      public Result resume(Scope scope, Resume statement) throws Exception {     
         Result result = handle(scope, statement);
         
         if(result.isYield()) {
            return suspend(scope, result, this, null);
         }
         return process(scope, result);
      }

      @Override
      public Resume suspend(Result result, Resume resume, Resume value) throws Exception {
         Yield yield = result.getValue();
         Resume child = yield.getResume();
         
         return new TryResume(child, resume);
      }

      private Result process(Scope scope, Result result) throws Exception {
         try {
            if(list != null) {
               if(result.isThrow()) {
                  result = list.execute(scope, result);            
               }   
            }
            if(result.isThrow()) {
               Object value = result.getValue();
               
               handler.handleInternalError(THROW, scope, value);
            }
         } finally {
            if(finish != null) {
               finish.execute(scope); // what about an exception here
            }
         }
         return result;
      }
      
      private Result handle(Scope scope, Resume statement) throws Exception {
         try {
            return statement.resume(scope, null);
         } catch(Throwable cause) {
            return Result.getThrow(cause);
         }
      }
   }
}