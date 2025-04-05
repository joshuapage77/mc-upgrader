import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ModCompatibilityResolver {

   static class ModConfig {
      public String slug;
      public boolean optional;
   }

   public static void main(String[] args) throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      List<ModConfig> mods = mapper.readValue(
         Files.readString(Paths.get("mods.json")),
         new TypeReference<List<ModConfig>>() {}
      );

      Map<String, Set<String>> modToVersions = new HashMap<>();

      for (ModConfig mod : mods) {
         Set<String> versions = fetchCompatibleVersions(mod.slug);
         modToVersions.put(mod.slug, versions);
      }

      Set<String> commonRequired = null;
      Set<String> commonAll = null;

      for (ModConfig mod : mods) {
         Set<String> versions = modToVersions.get(mod.slug);
         if (!mod.optional) {
            commonRequired = (commonRequired == null) ? new HashSet<>(versions) : intersection(commonRequired, versions);
         }
         commonAll = (commonAll == null) ? new HashSet<>(versions) : intersection(commonAll, versions);
      }

      System.out.println("Highest compatible Minecraft version (all mods): " + getLatestVersion(commonAll));
      System.out.println("Highest compatible Minecraft version (required only): " + getLatestVersion(commonRequired));
   }

   private static Set<String> fetchCompatibleVersions(String slug) throws IOException {
      String url = "https://api.modrinth.com/v2/project/" + slug + "/version";
      HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/json");

      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(conn.getInputStream());

      Set<String> mcVersions = new HashSet<>();
      for (JsonNode version : root) {
         JsonNode versions = version.get("game_versions");
         if (versions != null && versions.isArray()) {
            for (JsonNode v : versions) {
               mcVersions.add(v.asText());
            }
         }
      }
      return mcVersions;
   }

   private static Set<String> intersection(Set<String> a, Set<String> b) {
      return a.stream().filter(b::contains).collect(Collectors.toSet());
   }

   private static String getLatestVersion(Set<String> versions) {
      if (versions == null || versions.isEmpty()) return "None";
      return versions.stream()
         .sorted(ModCompatibilityResolver::compareMcVersions)
         .reduce((first, second) -> second) // last in sorted order
         .orElse("None");
   }

   private static int compareMcVersions(String v1, String v2) {
      String[] a = v1.split("\\.");
      String[] b = v2.split("\\.");
      int len = Math.max(a.length, b.length);
      for (int i = 0; i < len; i++) {
         int ai = i < a.length ? Integer.parseInt(a[i]) : 0;
         int bi = i < b.length ? Integer.parseInt(b[i]) : 0;
         int cmp = Integer.compare(ai, bi);
         if (cmp != 0) return cmp;
      }
      return 0;
   }
}
