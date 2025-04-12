package com.mordore.mods;

import com.mordore.pojo.ModVersion;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mordore.Utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ModrinthArtifactProviderTest {
   @Disabled("Integration Test")
   @Test
   public void testFetchVersions() {
      ModrinthArtifactProvider provider = new ModrinthArtifactProvider("fabric");
      Map<String, ModVersion> versions = provider.getVersions("worldedit-cui", List.of("1.21.4", "1.20.4", "1.20.3"), true);
      assertNotNull(versions);
      for (String mcVersion : versions.keySet()) {
         ModVersion version = versions.get(mcVersion);
         System.out.printf("Version: %s%nURL: %s%nPublished: %s%nMinecraft: %s%n%n",
               version.getVersionNumber(),
               version.getUrl(),
               version.getPublished(),
               Utils.prettyPrint(version.getGameVersions()));
      }
   }
}
