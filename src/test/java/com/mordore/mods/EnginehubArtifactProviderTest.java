package com.mordore.mods;

import com.mordore.Utils;
import com.mordore.pojo.ModVersion;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;

public class EnginehubArtifactProviderTest {
   @Disabled("Integration Test")
   @Test
   public void testFetchVersions() {
      EnginehubArtifactProvider provider = new EnginehubArtifactProvider("fabric");
      Map<String, ModVersion> versionMap = provider.getVersions("worldedit-cui", List.of("1.21.4", "1.21", "1.21.5"), true);
      assertNotNull(versionMap);
      for (String mcVersion : versionMap.keySet()) {
         ModVersion version = versionMap.get(mcVersion);
         System.out.printf("Version: %s%nURL: %s%nPublished: %s%nMinecraft: %s%n%n",
               version.getVersionNumber(),
               version.getUrl(),
               version.getPublished(),
               Utils.prettyPrint(version.getGameVersions()));
      }
   }
}
