package com.mordore.mods;

import com.mordore.pojo.ModVersion;

import java.util.List;
import java.util.Map;

public abstract class ModArtifactProvider {
   protected final String loader;

   protected ModArtifactProvider(String loader) {
      this.loader = loader;
   }

   public abstract Map<String, ModVersion> getVersions(String slug, List<String> mcVersion);
   public abstract Map<String, ModVersion> getVersions(String slug, List<String> mcVersion, boolean allowSnapshots);
}