package org.snapscript.tree;

import static org.snapscript.core.error.Reason.CAST;
import static org.snapscript.core.result.Result.RETURN;

import org.snapscript.core.Compilation;
import org.snapscript.core.Context;
import org.snapscript.core.Evaluation;
import org.snapscript.core.Execution;
import org.snapscript.core.Statement;
import org.snapscript.core.constraint.Constraint;
import org.snapscript.core.convert.ConstraintConverter;
import org.snapscript.core.convert.ConstraintMatcher;
import org.snapscript.core.convert.Score;
import org.snapscript.core.error.ErrorHandler;
import org.snapscript.core.module.Module;
import org.snapscript.core.module.Path;
import org.snapscript.core.result.Result;
import org.snapscript.core.scope.Scope;
import org.snapscript.core.trace.Trace;
import org.snapscript.core.trace.TraceInterceptor;
import org.snapscript.core.trace.TraceStatement;
import org.snapscript.core.type.Type;
import org.snapscript.core.variable.Value;
import org.snapscript.parse.StringToken;

public class ReturnStatement implements Compilation {
   
   private final Evaluation evaluation;
   
   public ReturnStatement(StringToken token){
      this(null, token);
   }
   
   public ReturnStatement(Evaluation evaluation){
      this(evaluation, null);
   }
   
   public ReturnStatement(Evaluation evaluation, StringToken token){
      this.evaluation = evaluation;
   }
   
   @Override
   public Statement compile(Module module, Path path, int line) throws Exception {
      Context context = module.getContext();
      ErrorHandler handler = context.getHandler();
      TraceInterceptor interceptor = context.getInterceptor();
      Trace trace = Trace.getNormal(module, path, line);
      Statement statement = create(module, path, line);
      
      return new TraceStatement(interceptor, handler, statement, trace);
   }   
   
   private Statement create(Module module, Path path, int line) throws Exception {
      Context context = module.getContext();
      ErrorHandler handler = context.getHandler();
      ConstraintMatcher matcher = context.getMatcher();
      
      return new CompileResult(matcher, handler, evaluation);
   }
   
   private static class CompileResult extends Statement {
   
      private final ConstraintMatcher matcher;
      private final ErrorHandler handler;
      private final Evaluation evaluation;

      public CompileResult(ConstraintMatcher matcher, ErrorHandler handler, Evaluation evaluation){
         this.evaluation = evaluation;
         this.matcher = matcher;
         this.handler = handler;
      }
      
      @Override
      public boolean define(Scope scope) throws Exception {
         if(evaluation != null) {
            evaluation.define(scope);
         }
         return true;
      }
      
      @Override
      public Execution compile(Scope scope, Constraint returns) throws Exception {
         if(evaluation != null) {
            Constraint constraint = evaluation.compile(scope, null);
            
            if(returns != null) {
               Type require = returns.getType(scope);
               Type actual = constraint.getType(scope);
               
               if(require != null) { // definite constraint
                  ConstraintConverter converter = matcher.match(require);
                  Score score = converter.score(actual);
                  
                  if(score.isInvalid()) {
                     handler.handleCompileError(CAST, scope, require, actual);
                  }                  
               }
            }                       
         }
         return new CompileExecution(evaluation);
      }
   }
   
   private static class CompileExecution extends Execution {
      
      private final Evaluation evaluation;

      public CompileExecution(Evaluation evaluation){
         this.evaluation = evaluation;
      }

      @Override
      public Result execute(Scope scope) throws Exception {
         if(evaluation != null) {
            Value value = evaluation.evaluate(scope, null);
            Object object = value.getValue();
            
            return Result.getReturn(object);
         }
         return RETURN;
      }
   }
}