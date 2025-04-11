package com.mordore.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordore.Utils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Config {
   private static Config instance;
   private final String minecraft;
   private final List<GameConfig> games;

   private Config() throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      PropertiesConfig config = mapper.readValue(
            Files.readString(Paths.get("properties.json")),
            PropertiesConfig.class
      );
      this.games = config.games;
      this.minecraft = Utils.expandPath(config.minecraft);
   }

   public static Config getInstance() throws IOException {
      if (instance == null) {
         synchronized (Config.class) {
            if (instance == null) {
               instance = new Config();
            }
         }
      }
      return instance;
   }

   static public List<ModConfig> getModConfigs(String gamePath) throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(
            Files.readString(Paths.get(gamePath, "config", "mods.json")),
            new TypeReference<List<ModConfig>>() {
            }
      );
   }

   public List<GameConfig> getGames() {
      return games;
   }
   public String getMinecraft() { return minecraft; }

   public static class ModConfig {
      public String slug;
      public boolean optional;
      public String type;
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
