package org.snapscript.tree;

public interface OperationResolver {
   Operation resolve(String name) throws Exception;
}