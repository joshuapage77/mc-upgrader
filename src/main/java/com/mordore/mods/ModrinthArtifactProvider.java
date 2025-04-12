package com.mordore.mods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordore.pojo.ModVersion;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModrinthArtifactProvider extends ModArtifactProvider {

   public ModrinthArtifactProvider(String loader) {
      super(loader);
   }

   @Override
   public Map<String, ModVersion> getVersions(String slug, List<String> mcVersions) {
      return getVersions(slug, mcVersions, false);
   }

   @Override
   public Map<String, ModVersion> getVersions(String slug, List<String> mcVersions, boolean allowSnapshots) {
      try {
         String baseUrl = "https://api.modrinth.com/v2/project/" + slug + "/version";
         ObjectMapper mapper = new ObjectMapper();
         String gameVersionsJson = mapper.writeValueAsString(mcVersions);
         String loadersJson = mapper.writeValueAsString(List.of(loader));

         String gameVersionsQp = (mcVersions != null && !mcVersions.isEmpty())
               ? "&game_versions=" + URLEncoder.encode(gameVersionsJson, StandardCharsets.UTF_8)
               : "";

         String url = baseUrl + "?loaders=" + URLEncoder.encode(loadersJson, StandardCharsets.UTF_8) + gameVersionsQp;

         HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
         conn.setRequestMethod("GET");
         conn.setRequestProperty("Accept", "application/json");

         JsonNode root = mapper.readTree(conn.getInputStream());
         Map<String, ModVersion> latestPerMcVersion = new HashMap<>();

         for (JsonNode version : root) {
            String type = version.path("version_type").asText();
            if (!allowSnapshots && !"release".equalsIgnoreCase(type)) continue;
            if (!version.path("game_versions").isArray()) continue;

            ModVersion modVersion = new ModVersion(version);
            if (modVersion.getGameVersions().isEmpty()) continue;

            for (String ver : modVersion.getGameVersions()) {
               if (!latestPerMcVersion.containsKey(ver) || latestPerMcVersion.get(ver).compareTo(modVersion) < 0) {
                  latestPerMcVersion.put(ver, modVersion);
               }
            }
         }

         return latestPerMcVersion;

      } catch (IOException e) {
         throw new RuntimeException("Failed to fetch versions from Modrinth for slug: " + slug, e);
      }
   }

   /**
    * Returns versions release versions in the range provided
    * @param startVersion minecraft game versions
    * @param endVersion minecraft game version
    * @return game versions in range or empty list
    * @throws IOException
    */
   public static List<String> getReleaseMinecraftVersions(String startVersion, boolean startInclusive, String endVersion, boolean endInclusive) throws IOException {
      String url = "https://api.modrinth.com/v2/tag/game_version";
      HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/json");

      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(conn.getInputStream());

      List<String> releaseVersions = new ArrayList<>();

      for (JsonNode version : root) {
         String type = version.get("version_type").asText();

         if (!"release".equalsIgnoreCase(type)) continue;
         String id = version.get("version").asText();

         int compareStart = compareMcVersions(id, startVersion);
         int compareEnd = endVersion == null ? -1 : compareMcVersions(id, endVersion);
         if ((compareStart > 0 || (startInclusive && compareStart == 0)) && (compareEnd < 0 || (endInclusive && compareEnd == 0))) {
            releaseVersions.add(id);
         }
      }

      releaseVersions.sort((a, b) -> compareMcVersions(b, a));
      return releaseVersions;
   }

   protected static int compareMcVersions(String v1, String v2) {
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