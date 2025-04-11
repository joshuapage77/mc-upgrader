package com.mordore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordore.config.Config;
import com.mordore.config.Config.ModConfig;
import com.mordore.pojo.ModVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import com.mordore.pojo.VersionResult;
import java.util.*;
import java.util.stream.Collectors;

public class ModCompatibilityResolver {
   private static final Logger log = LoggerFactory.getLogger(ModCompatibilityResolver.class);

   public static void main(String[] args) throws IOException {
      String targetGame = (args.length > 0) ? args[0] : null;
      Config config = Config.getInstance();

      for (Config.GameConfig game : config.getGames()) {
         if (targetGame != null && !game.name.equalsIgnoreCase(targetGame)) continue;
         log.info("Checking game: {} at path: {}", game.name, game.getPath());
         List<ModConfig> mods = Config.getModConfigs(game.getPath());
         VersionResult versions = runVersionCheck(mods, game.getPath());
         log.info("Highest compatible Minecraft version (all mods): {}", versions.latestAll());
         log.info("Highest compatible Minecraft version (required only): {}", versions.latestRequired());
      }
   }

   public static VersionResult runVersionCheck(List<ModConfig> mods, String gamePath) throws IOException {
      return runVersionCheck(mods, gamePath, null);
   }
   public static VersionResult runVersionCheck(List<ModConfig> mods, String gamePath, List<String> gameVersions) throws IOException {
      log.debug("runVersionCheck Params - gamePath: [{}] gameVersions:\n{}", gamePath, Utils.prettyPrint(gameVersions));
      log.debug("Loaded mod configuration:\n{}", Utils.prettyPrint(mods));
      Map<String, Map<String, ModVersion>> modVersionMap = new HashMap<>(); // maps mod slug to map of game versions to ModVersions
      for (ModConfig mod : mods) {
         modVersionMap.put(mod.slug, fetchCompatibleVersions(mod.slug, gameVersions));
      }
      log.debug("Fetched compatible versions:\n{}", Utils.prettyPrint(modVersionMap));

      Set<String> commonRequired = null;
      Set<String> commonAll = null;

      for (ModConfig mod : mods) {
         Set<String> versions = modVersionMap.get(mod.slug).keySet();
         if (!mod.optional) commonRequired = (commonRequired == null) ? new HashSet<>(versions) : intersection(commonRequired, versions);
         commonAll = (commonAll == null) ? new HashSet<>(versions) : intersection(commonAll, versions);
      }

      String latestAll = getLatestVersion(commonAll);
      String latestRequired = getLatestVersion(commonRequired);

      //map each version to a map of slugs (mod name) to newest compatible mod version for the game version
      Map<String, Map<String, ModVersion>> gameVersionToModVersion = new HashMap<>();
      gameVersionToModVersion.put(latestAll, getModVersionsForGameVersion(mods, latestAll, modVersionMap));
      gameVersionToModVersion.put(latestRequired, getModVersionsForGameVersion(mods, latestRequired, modVersionMap));

      return new VersionResult(latestAll, latestRequired, gameVersionToModVersion);
   }

   private static Map<String, ModVersion> getModVersionsForGameVersion (List<ModConfig> mods, String gameVersion, Map<String, Map<String, ModVersion>> modVersionMap) {
      Map<String, ModVersion> modVersion = new HashMap<>();
      for (Config.ModConfig mod : mods) {
         modVersion.put(mod.slug, modVersionMap.get(mod.slug).get(gameVersion));
      }
      return modVersion;
   }

   private static Map<String, ModVersion> fetchCompatibleVersions(String slug, List<String> gameVersions) throws IOException {
      String baseUrl = "https://api.modrinth.com/v2/project/" + slug + "/version";
      ObjectMapper mapper = new ObjectMapper();
      String gameVersionsJson = mapper.writeValueAsString(gameVersions); // ["1.20.3", "1.19.4"]
      String loadersJson = mapper.writeValueAsString(List.of("fabric"));

      String gameVersionsQp = (gameVersions != null && !gameVersions.isEmpty()) ? "&game_versions=" + URLEncoder.encode(gameVersionsJson, StandardCharsets.UTF_8) : "";
      String url = baseUrl + "?loaders=" + URLEncoder.encode(loadersJson, StandardCharsets.UTF_8) + gameVersionsQp;

      HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/json");

      JsonNode root = mapper.readTree(conn.getInputStream());

      Map<String, ModVersion> mcVersions = new HashMap<>();
      for (JsonNode version : root) {
         if (!version.path("version_type").asText().equalsIgnoreCase("release")) continue;
         if (!version.path("game_versions").isArray()) continue;

         ModVersion modVersion = new ModVersion(version);
         if (modVersion.getGameVersions().isEmpty()) continue;

         for (String ver : modVersion.getGameVersions()) {
            if (mcVersions.containsKey(ver)) {
               if (mcVersions.get(ver).compareTo(modVersion) >= 0) continue;
            }
            mcVersions.put(ver, modVersion);
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
            .reduce((first, second) -> second)
            .orElse("None");
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

   // Returns list of release versions of minecraft game newer than provided `currentVersion`
   public static List<String> getNewerGameVersions(String currentVersion) throws IOException {
      String url = "https://api.modrinth.com/v2/tag/game_version";
      HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/json");

      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(conn.getInputStream());

      List<String> newerVersions = new ArrayList<>();

      for (JsonNode version : root) {
         String type = version.get("version_type").asText();

         if (!"release".equalsIgnoreCase(type)) continue;
         String id = version.get("version").asText();

         if (compareMcVersions(id, currentVersion) > 0) {
            newerVersions.add(id);
         }
      }

      newerVersions.sort((a, b) -> compareMcVersions(b, a));
      return newerVersions;
   }

}