package com.mordore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mordore.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;

public class LauncherProfiles {
   private static final Logger log = LoggerFactory.getLogger(LauncherProfiles.class);

   public static String findCurrentVersion(String minecraftDir, String gameDir) {
      ObjectNode profile = findProfileByGameDir(minecraftDir, gameDir);
      if (profile != null && profile.has("lastVersionId")) {
         return profile.get("lastVersionId").asText();
      }
      return null;
   }

   public static boolean updateVersion(String minecraftDir, String gameDir, String newVersionId) {
      Path launcherProfilesPath = getLauncherProfilesPath(minecraftDir);
      if (!Files.exists(launcherProfilesPath)) {
         log.warn("launcher_profiles.json not found at {}", launcherProfilesPath);
         return false;
      }

      try {
         String json = Files.readString(launcherProfilesPath);
         ObjectMapper mapper = new ObjectMapper();
         JsonNode root = mapper.readTree(json);
         JsonNode profiles = root.get("profiles");
         if (profiles == null || !profiles.isObject()) {
            log.warn("No profiles found in launcher_profiles.json");
            return false;
         }

         Path normalizedGameDir = Paths.get(gameDir).toAbsolutePath().normalize();
         boolean updated = false;

         Iterator<Map.Entry<String, JsonNode>> fields = profiles.fields();
         while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            ObjectNode profile = (ObjectNode) entry.getValue();
            JsonNode dirNode = profile.get("gameDir");
            if (dirNode != null) {
               Path profileDir = resolveGameDir(dirNode.asText(), minecraftDir);
               if (normalizedGameDir.equals(profileDir)) {
                  profile.put("lastVersionId", newVersionId);
                  updated = true;
                  break;
               }
            }
         }

         if (updated) {
            Files.writeString(launcherProfilesPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
            return true;
         } else {
            log.warn("No matching gameDir found in launcher_profiles.json");
         }

      } catch (IOException e) {
         log.error("Failed to update launcher_profiles.json", e);
      }

      return false;
   }

   private static Path getLauncherProfilesPath(String minecraftDir) {
      return Paths.get(minecraftDir, "launcher_profiles.json");
   }

   private static ObjectNode findProfileByGameDir(String minecraftDir, String gameDir) {
      Path launcherProfilesPath = Paths.get(minecraftDir, "launcher_profiles.json");
      if (!Files.exists(launcherProfilesPath)) {
         log.warn("launcher_profiles.json not found at {}", launcherProfilesPath);
         return null;
      }

      Path normalizedGameDir = Paths.get(gameDir).toAbsolutePath().normalize();

      try {
         String json = Files.readString(launcherProfilesPath);
         ObjectMapper mapper = new ObjectMapper();
         JsonNode root = mapper.readTree(json);
         JsonNode profiles = root.get("profiles");
         if (profiles == null || !profiles.isObject()) {
            log.warn("No profiles found in launcher_profiles.json");
            return null;
         }

         Iterator<Map.Entry<String, JsonNode>> fields = profiles.fields();
         while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode profile = entry.getValue();
            JsonNode dirNode = profile.get("gameDir");
            if (dirNode != null) {
               Path profileDir = resolveGameDir(dirNode.asText(), minecraftDir);
               if (normalizedGameDir.equals(profileDir)) {
                  return (ObjectNode) profile;
               }
            }
         }

         log.warn("No matching gameDir found in launcher_profiles.json");
      } catch (IOException e) {
         log.error("Failed to read or parse launcher_profiles.json", e);
      }

      return null;
   }

   protected static Path resolveGameDir(String gameDirPath, String minecraftDir) {
      Path path = Paths.get(gameDirPath);
      if (!path.isAbsolute()) {
         path = Paths.get(minecraftDir).resolve(path);
      }
      return path.toAbsolutePath().normalize();
   }

   public static Path backup() throws IOException {
      Path filePath = getLauncherProfilesPath(Config.getInstance().getMinecraft());
      if (!Files.exists(filePath)) {
         throw new IllegalArgumentException("File does not exist: " + filePath);
      }

      String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
      String backupName = filePath.getFileName().toString() + "." + timestamp;
      Path backupPath = filePath.getParent().resolve(backupName);

      return Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
   }

   public static void restore(Path backupPath) throws IOException {
      Path launcherProfiles = getLauncherProfilesPath(Config.getInstance().getMinecraft());
      Files.copy(backupPath, launcherProfiles, StandardCopyOption.REPLACE_EXISTING);
   }

   private static String expandPath(String path) {
      if (path.startsWith("~")) {
         return path.replaceFirst("~", System.getProperty("user.home"));
      }
      return path;
   }
}
