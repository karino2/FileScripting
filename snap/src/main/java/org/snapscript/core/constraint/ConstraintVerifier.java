package org.snapscript.core.constraint;

import java.util.List;

import org.snapscript.core.InternalStateException;
import org.snapscript.core.convert.InstanceOfChecker;
import org.snapscript.core.scope.Scope;
import org.snapscript.core.type.Type;

public class ConstraintVerifier {
   
   private final InstanceOfChecker checker;
   
   public ConstraintVerifier() {
      this.checker = new InstanceOfChecker();
   }

   public void verify(Scope scope, Constraint constraint) {
      Type type = constraint.getType(scope);
      
      if(type != null) {
         List<Constraint> generics = type.getConstraints();
         List<Constraint> constraints = constraint.getGenerics(scope);
         int constraintCount = constraints.size();
         int genericCount = generics.size();
         
         if(constraintCount != 0) {
            if(constraintCount != genericCount) {
               throw new InternalStateException("Generic type '" + type + "' requires " + genericCount + " bounds");
            }
            for(int i = 0; i < constraintCount; i++) {
               Constraint genericBound = generics.get(i);
               Constraint constraintBound = constraints.get(i);
               Type genericType = genericBound.getType(scope);
               Type constraintType = constraintBound.getType(scope);
               
               if(genericType != null && constraintType == null) {
                  throw new InternalStateException("Generic parameter '" + constraintType + "' does not match '" + genericType + "'");
               }
               if(!checker.isInstanceOf(scope, constraintType, genericType)) {
                  throw new InternalStateException("Generic parameter '" + constraintType + "' does not match '" + genericType + "'");
               }
            }
         }
      }
   }
}
