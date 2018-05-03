package org.snapscript.core.function.index;

import java.util.List;

import org.snapscript.common.CopyOnWriteSparseArray;
import org.snapscript.common.SparseArray;
import org.snapscript.core.type.Type;
import org.snapscript.core.function.Function;
import org.snapscript.core.module.Module;
import org.snapscript.core.stack.ThreadStack;
import org.snapscript.core.type.TypeExtractor;

public class ModuleIndexer {
   
   private final SparseArray<FunctionIndex> indexes;
   private final FunctionPointerConverter converter;
   private final FunctionIndexBuilder builder;
   
   public ModuleIndexer(TypeExtractor extractor, ThreadStack stack) {
      this(extractor, stack, 10000);
   }
   
   public ModuleIndexer(TypeExtractor extractor, ThreadStack stack, int capacity) {
      this.indexes = new CopyOnWriteSparseArray<FunctionIndex>(capacity);
      this.builder = new FunctionIndexBuilder(extractor, stack);
      this.converter = new FunctionPointerConverter(stack);
   }

   public void purgeModuleIndex(Module module) {
      int index = module.getOrder();
      indexes.remove(index);
   }

   public FunctionPointer index(Module module, String name, Type... types) throws Exception { 
      int index = module.getOrder();
      FunctionIndex match = indexes.get(index);
      
      if(match == null) {
         List<Function> functions = module.getFunctions();
         FunctionIndex table = builder.create(module);
         
         for(Function function : functions){
            FunctionPointer pointer = converter.convert(function);
            table.index(pointer);
         }
         indexes.set(index, table);
         return table.resolve(name, types);
      }
      return match.resolve(name, types);
   }
   
   public FunctionPointer index(Module module, String name, Object... values) throws Exception { 
      int index = module.getOrder();
      FunctionIndex match = indexes.get(index);
      
      if(match == null) {
         List<Function> functions = module.getFunctions();
         FunctionIndex table = builder.create(module);
         
         for(Function function : functions){
            FunctionPointer pointer = converter.convert(function);
            table.index(pointer);
         }
         indexes.set(index, table);
         return table.resolve(name, values);
      }
      return match.resolve(name, values);
   }
}