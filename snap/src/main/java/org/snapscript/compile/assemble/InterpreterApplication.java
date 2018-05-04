package org.snapscript.compile.assemble;

import org.snapscript.core.Context;
import org.snapscript.core.Execution;
import org.snapscript.core.ScopeMerger;
import org.snapscript.core.error.ErrorHandler;
import org.snapscript.core.link.Package;
import org.snapscript.core.module.Path;
import org.snapscript.core.result.Result;
import org.snapscript.core.scope.EmptyModel;
import org.snapscript.core.scope.Model;
import org.snapscript.core.scope.Scope;

import static org.snapscript.core.error.Reason.THROW;

public class InterpreterApplication {
    private ApplicationCompiler compiler = null;
    private final ScopeMerger merger;
    private final Context context;
    private final Model model;
    private final String name;
    private final Path path;

    Scope scope;

    public InterpreterApplication(Context context, Path path, String name, Model model){
        this.merger = new ScopeMerger(context);
        this.model = model;
        this.context = context;
        this.path = path;
        this.name = name;

        scope = merger.merge(model, name, path);
    }

    public Object evaluate(String source) throws Exception {
        return context.getEvaluator().evaluate(scope, source);
    }

    public Result evaluateStatements(Package library) throws Exception{
        setupCompiler(library);
        Execution execution = compiler.compile(scope); // create all types

        return execution.execute(scope);
    }

    private void setupCompiler(Package library) {
        if(compiler == null) {
            compiler = new ApplicationCompiler(context, library);
        } else{
            compiler.setLibrary(library);
        }
    }

}
