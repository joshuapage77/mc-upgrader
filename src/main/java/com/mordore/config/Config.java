package com.mordore.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordore.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Config {
   private String minecraft;
   private final List<GameConfig> games;
   private Path java;
   private static final Logger log = LoggerFactory.getLogger(Config.class);

   private Config() {
      ObjectMapper mapper = new ObjectMapper();
      PropertiesConfig config = null;
      try {
         config = mapper.readValue(
               Files.readString(Paths.get("properties.json")),
               PropertiesConfig.class
         );
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
      this.games = config.games;
      this.minecraft = Utils.expandPath(config.minecraft);
      try {
         this.java = Utils.findJavaExecutable(this.minecraft);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public void setMinecraft (String mcPath) {
      this.minecraft = mcPath;
   }

   public List<GameConfig> getGames() {
      return games;
   }
   public String getMinecraft() { return minecraft; }

   public Path getJava() {
      return java;
   }

   public void setJava(Path java) {
      this.java = java;
   }

   private static final class InstanceHolder {
      private static final Config instance = new Config();
   }

   public static Config getInstance() {
      return InstanceHolder.instance;
   }

   static public List<ModConfig> getModConfigs(String gamePath) throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(
            Files.readString(Paths.get(gamePath, "config", "mods.json")),
            new TypeReference<List<ModConfig>>() {
            }
      );
   }

   public static class GameConfig {
      public String name;
      private String path;
      public void setPath(String path) {
         this.path = Utils.expandPath(path);
      }
      public String getPath() { return path; }
   }

   static class PropertiesConfig {
      public String minecraft;
      public List<GameConfig> games;
   }
}
