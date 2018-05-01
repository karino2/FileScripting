package org.snapscript.core.link;

import java.util.List;

import org.snapscript.core.InternalStateException;
import org.snapscript.core.ResourceManager;
import org.snapscript.core.module.FilePathConverter;
import org.snapscript.core.module.Path;
import org.snapscript.core.module.PathConverter;

public class PackageLoader {
   
   private final PackageBundleLoader loader;
   private final PackageBundleMerger merger;
   private final PathConverter converter;
   
   public PackageLoader(PackageLinker linker, ResourceManager manager){
      this.converter = new FilePathConverter();
      this.loader = new PackageBundleLoader(linker, manager, converter);
      this.merger = new PackageBundleMerger();
   }

   public Package load(ImportType type, String... resources) throws Exception {
      PackageBundle bundle = loader.load(resources);
      List<Package> packages = bundle.getPackages();
      
      if(packages.isEmpty() && type.isRequired()) {
         StringBuilder message = new StringBuilder();
         
         for(String resource : resources) {
            Path path = converter.createPath(resource);
            int size = message.length();
            
            if(size > 0) {
               message.append(" or ");
            }
            message.append("'");
            message.append(path);
            message.append("'");
         }
         throw new InternalStateException("Could not load library " + message);
      }
      return merger.merge(bundle);
   }
}