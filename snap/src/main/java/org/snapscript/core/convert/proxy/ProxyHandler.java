package org.snapscript.core.convert.proxy;

import java.lang.reflect.InvocationHandler;

public interface ProxyHandler extends InvocationHandler {
   Object extract();
}