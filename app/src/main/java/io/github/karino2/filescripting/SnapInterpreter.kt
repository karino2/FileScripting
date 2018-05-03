package io.github.karino2.filescripting

import org.snapscript.common.store.ClassPathStore
import org.snapscript.compile.StoreContext
import org.snapscript.compile.StringCompiler
import org.snapscript.core.Reserved.DEFAULT_PACKAGE
import org.snapscript.core.ScopeMerger
import org.snapscript.core.constraint.Constraint
import org.snapscript.core.constraint.Constraint.NONE
import org.snapscript.core.convert.FunctionComparator
import org.snapscript.core.function.*
import org.snapscript.core.function.resolve.FunctionCall
import org.snapscript.core.module.FilePathConverter
import org.snapscript.core.module.Path
import org.snapscript.core.scope.MapModel
import org.snapscript.core.type.Type

/*
 */
class SnapInterpreter() {
    val variableMap by lazy {
        val map = mutableMapOf<String, Any?>()
        map.put("GLOBALS", map)
        map
    }

    fun getVar(name:String) = variableMap.get(name)
    fun putVar(name:String, obj: Any?) = variableMap.put(name, obj)

    val model by lazy {
        MapModel(variableMap)
    }

    val evaluator by lazy {
        context.evaluator
    }

    val context by lazy {
        val store = ClassPathStore()
         StoreContext(store)
    }

    fun getVariable(name: String) =variableMap.get(name)
    fun putVariable(name:String, someValue: Any?) = variableMap.put(name, someValue)


    fun eval(script: String): Any?{
        try {
            // try first as expression
            return evaluator.evaluate<Any?>(model, script)
        }catch(e: Exception) {
            val converter = FilePathConverter()
            context.linker.purge(converter.createPath(DEFAULT_PACKAGE));
            context.resolver.purgeModuleIndex(defaultModule)

            val compiler = StringCompiler(context)
            val exec = compiler.compile(script)
            exec.execute(model)


            return null
        }

    }

    /*
          Score scoreExtendX = extendX.getSignature().getConverter().score(new byte[10], 1, 1);

                FunctionComparator comparator = new FunctionComparator(matcher);
      Module module = new ContextModule(context, null, path, "yy", "", 1);
      ClosureFunctionFinder finder = new ClosureFunctionFinder(comparator, extractor, loader);
      Parameter parameter = new Parameter("n", Constraint.STRING, false);
      Signature signature = new FunctionSignature(Arrays.asList(parameter), module, null, false);
      Type type = new InvocationFunction(signature, null, null, null, "xx").getHandle();
      Function function = finder.findFunctional(type);


      Map<String, Object> map = new HashMap<String, Object>();
      Context context = new TestContext();
      Model model = new EmptyModel();
      Path path = new Path(Reserved.DEFAULT_RESOURCE);
      Module module = new ContextModule(context, null, path, Reserved.DEFAULT_PACKAGE, "");
      Scope scope = new ModelScope(model, module);

      context.getResolver().resolveInstance(scope, map, "put", "x", 11).call();

      Class real2 = MockComparator.class;
      Type type2 = loader.loadType(real2);

     */

    val defaultModule by lazy { context.registry.getModule(DEFAULT_PACKAGE) }

    val merger by lazy { ScopeMerger(context)  }

    fun resolveFunction(name: String,  args: Array<Any?>) : FunctionCall? {
        val module = defaultModule

        /*
        val parameters = mutableListOf<Parameter>()

        val builder = ParameterBuilder()
        for((i, argTypeClass) in argTypes.withIndex()) {
            val constraints = Constraint.getConstraint(context.extractor.getType(argTypeClass))

            val parameter = when(argTypeClass) {
                is java.lang.Object -> builder.create(NONE, i)
                else -> builder.create(constraints, i)
            }
            parameters.add(parameter);
        }

        val signature = FunctionSignature(parameters, module, null, true)
        */

        /*
              FunctionCall local = binder.resolveModule(scope, module, name, arguments);

      if(local == null) {
         FunctionCall closure = binder.resolveScope(scope, name, arguments); // function variable

         if(closure != null) {
            return closure;
         }
      }
      return local;

         */

        // merger.merge(model, module);

        return context.resolver.resolveModule(merger.merge(model, DEFAULT_PACKAGE), module, name, *args)

        /*
                        FunctionComparator comparator = new FunctionComparator(matcher);
      Module module = new ContextModule(context, null, path, "yy", "", 1);
      ClosureFunctionFinder finder = new ClosureFunctionFinder(comparator, extractor, loader);
      Parameter parameter = new Parameter("n", Constraint.STRING, false);
      Signature signature = new FunctionSignature(Arrays.asList(parameter), module, null, false);
      Type type = new InvocationFunction(signature, null, null, null, "xx").getHandle();
      Function function = finder.findFunctional(type);

         */

        /*
        val comparator = FunctionComparator(context.matcher)
        val finder = ClosureFunctionFinder(comparator, context.extractor, context.loader)
        val closureType = InvocationFunction(signature, null,null, null, name).handle

        // InvocationFunction(signature, null,null, null, name).invocation.


        return finder.findFunctional(closureType)
        */



        /*
        // ParameterBuilder
        Parameter("n")
        module.functions
        FunctionSignature
        context.resolver.resolveFunction(module.scope, null, name, argTypes)
        */
    }

}