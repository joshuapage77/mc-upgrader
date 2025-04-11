// Test for getNewerVersions
package com.mordore;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ModCompatibilityResolverTest {

   // Example placeholder test for getNewerVersions - will hit API
   @Test
   public void testGetNewerGameVersions_liveCall() throws Exception {
      List<String> newer = ModCompatibilityResolver.getNewerGameVersions("1.19");
      assertNotNull(newer);
      assertTrue(newer.stream().allMatch(v -> ModCompatibilityResolverWrapper.getVersionComparator().compare(v, "1.19") > 0));
      ObjectMapper mapper = new ObjectMapper();
      System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(newer));
   }

   private static class ModCompatibilityResolverWrapper extends ModCompatibilityResolver {
      public static Comparator<String> getVersionComparator() {
         return ModCompatibilityResolver::compareMcVersions;
      }
   }

}
