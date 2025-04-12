package com.mordore.mods;

import com.mordore.pojo.ModVersion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mordore.config.ModConfig;

public class Mod {
   private final ModConfig modCfg;
   private final List<String> mcVersions;
   private final ModArtifactProvider releaseProvider;
   private final ModArtifactProvider snapshotProvider;
   private Map<String, ModVersion> allByMcVersion = null;
   private Map<String, ModVersion> releaseByMcVersion;
   private Map<String, ModVersion> latestByMcVersion;

   public Mod(ModConfig modConfig, List<String> mcVersions) {
      modCfg = modConfig;
      this.mcVersions = mcVersions;
      ProviderType releaseArtifacts = ProviderType.fromValue(modCfg.releaseArtifacts);
      ProviderType snapshotArtifacts = ProviderType.fromValue(modCfg.snapshotArtifacts);

      releaseProvider = (releaseArtifacts == ProviderType.Modrinth) ? new ModrinthArtifactProvider(modCfg.loader) : new EnginehubArtifactProvider(modCfg.loader);
      snapshotProvider = (snapshotArtifacts == ProviderType.Modrinth) ? new ModrinthArtifactProvider(modCfg.loader) : new EnginehubArtifactProvider(modCfg.loader);

   }
   public List<String> getMinecraftVersions () {
      if (allByMcVersion == null) {
         if (modCfg.useSnapshot) {
            allByMcVersion = new HashMap<>(getLatestByMcVersion());
            allByMcVersion.putAll(getReleaseByMcVersion());
         } else allByMcVersion = getLatestByMcVersion();
      }
      return allByMcVersion.keySet().stream().toList();
   }
   public ModVersion getModVersion(String minecraftVersion) {
      return allByMcVersion.get(minecraftVersion);
   }
   private Map<String, ModVersion> getReleaseByMcVersion () {
      if (releaseByMcVersion == null) {
         releaseByMcVersion = releaseProvider.getVersions(modCfg.slug, mcVersions, false);
      }
      return releaseByMcVersion;
   }
   private Map<String, ModVersion> getLatestByMcVersion() {
      if (latestByMcVersion == null) {
         latestByMcVersion = snapshotProvider.getVersions(modCfg.slug, mcVersions, true);
      }
      return latestByMcVersion;
   }

   public List<String> getReleaseMinecraftVersions () {
      return getReleaseByMcVersion().keySet().stream().toList();
   }

   public List<String> getSnapshotMinecraftVersions () {
      return getLatestByMcVersion().keySet().stream().toList();
   }

   public ModConfig getModConfig() {
      return modCfg;
   }

}
