package org.snapscript.tree.script;

import static org.snapscript.core.result.Result.NORMAL;

import org.snapscript.core.Execution;
import org.snapscript.core.InternalStateException;
import org.snapscript.core.Statement;
import org.snapscript.core.constraint.Constraint;
import org.snapscript.core.result.Result;
import org.snapscript.core.scope.Scope;

public class Script extends Statement {
   
   private final Statement[] statements;
   private final Statement[] executable;
   
   public Script(Statement... statements) {
      this.executable = new Statement[statements.length];
      this.statements = statements;
   }
   
   @Override
   public void create(Scope scope) throws Exception {
      for(Statement statement : statements) {
         statement.create(scope);
      }
   }
   
   @Override
   public boolean define(Scope scope) throws Exception {      
      for(int i = 0; i < statements.length; i++){
         Statement statement = statements[i];
         
         if(statement.define(scope)){
            executable[i] = statement;
            statements[i] = null;
         }
      }
      return true;
   }
   
   @Override
   public Execution compile(Scope scope, Constraint returns) throws Exception {
      Execution[] executions = new Execution[statements.length];
      
      for(int i = 0; i < executable.length; i++) {
         Statement statement = executable[i];
         
         if(statement != null) {
            executions[i]  = statement.compile(scope, null);
         }
      }
      for(int i = 0; i < statements.length; i++) {
         Statement statement = statements[i];
         
         if(statement != null) {
            executions[i]  = statement.compile(scope, null);
         }
      }
      return new ScriptExecution(executions);
   }
   
   private static class ScriptExecution extends Execution {
      
      private final Execution[] executions;
      
      public ScriptExecution(Execution... executions) {
         this.executions = executions;
      }
   
      @Override
      public Result execute(Scope scope) throws Exception {
         Result last = NORMAL;
         
         for(Execution statement : executions) {
            Result result = statement.execute(scope);
            
            if(!result.isNormal()){
               throw new InternalStateException("Illegal statement");
            }
            last = result;
         }
         return last;
      }
   }
}