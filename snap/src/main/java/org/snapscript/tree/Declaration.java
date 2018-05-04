package org.snapscript.tree;

import java.util.concurrent.atomic.AtomicInteger;

import org.snapscript.core.Evaluation;
import org.snapscript.core.InternalStateException;
import org.snapscript.core.constraint.Constraint;
import org.snapscript.core.module.ModuleScope;
import org.snapscript.core.scope.ModelScope;
import org.snapscript.core.scope.Scope;
import org.snapscript.core.scope.index.Index;
import org.snapscript.core.scope.index.Local;
import org.snapscript.core.scope.index.StackIndex;
import org.snapscript.core.scope.index.Table;
import org.snapscript.core.variable.Value;
import org.snapscript.tree.literal.TextLiteral;

public class Declaration {

   private final DeclarationAllocator allocator;
   private final NameReference reference;
   private final AtomicInteger offset;
   private final Evaluation value;
   
   public Declaration(TextLiteral identifier) {
      this(identifier, null, null);
   }
   
   public Declaration(TextLiteral identifier, Constraint constraint) {      
      this(identifier, constraint, null);
   }
   
   public Declaration(TextLiteral identifier, Evaluation value) {
      this(identifier, null, value);
   }
   
   public Declaration(TextLiteral identifier, Constraint constraint, Evaluation value) {
      this.allocator = new DeclarationAllocator(constraint, value);
      this.reference = new NameReference(identifier);
      this.offset = new AtomicInteger(-1);
      this.value = value;
   }   

   public int define(Scope scope, int modifiers) throws Exception {
      String name = reference.getName(scope);
      
      if(value != null){
         value.define(scope); // must compile value first
      }
      int depth = getStackIndex(scope, name);
      
      offset.set(depth);
      return depth;
   }

   /*
    allow re-definition of variable for global scope.
    this is often the case for REPL, but I don't want to make big change.
    So just check scope whether it's ModuleScope or not, and if ModuleScope, I regard it as REPL scope.
    This has side effect for not checking module scope variable re-definition by accident,
    but module scope variable definition itself is not so common.
     */

   private int getStackIndex(Scope scope, String name) {
      boolean isGlobal = scope instanceof ModuleScope;
      Index index =  scope.getIndex();
      if(isGlobal && (index instanceof StackIndex)) {
         return ((StackIndex)index).newOrReplaceIndex(name);
      } else {
         return index.index(name);
      }
   }


   public Value compile(Scope scope, int modifiers) throws Exception {
      String name = reference.getName(scope);
      Local local = allocator.compile(scope, name, modifiers);
      Table table = scope.getTable();
      int depth = offset.get();
      
      try { 
         table.add(depth, local);
      }catch(Exception e) {
         throw new InternalStateException("Declaration of variable '" + name +"' failed", e);
      }  
      return local;
   }
   
   public Value declare(Scope scope, int modifiers) throws Exception {
      String name = reference.getName(scope);
      Local local = allocator.allocate(scope, name, modifiers);
      Table table = scope.getTable();
      int depth = offset.get();
      
      try { 
         table.add(depth, local);
      }catch(Exception e) {
         throw new InternalStateException("Declaration of variable '" + name +"' failed", e);
      }  
      return local;
   }
}