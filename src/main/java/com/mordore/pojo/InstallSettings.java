package com.mordore.pojo;

import com.mordore.config.Config;

import java.nio.file.Path;

public class InstallSettings {
   private Path minecraftPath;
   private Path javaPath;
   private Path gamesPath;
   private String gameName;

   public InstallSettings() {
      this.minecraftPath = Path.of("unknown");
      this.javaPath = Path.of("unknown");
      this.gamesPath = Path.of("unknown");
      this.gameName = "no name";
   }

   public Path getMinecraftPath() { return minecraftPath; }
   public void setMinecraftPath(Path minecraftPath) { this.minecraftPath = minecraftPath; }

   public Path getJavaPath() { return javaPath; }
   public void setJavaPath(Path javaPath) { this.javaPath = javaPath; }

   public Path getGamesPath() { return gamesPath; }
   public void setGamesPath(Path gamesPath) { this.gamesPath = gamesPath; }

   public String getGameName() { return gameName; }
   public void setGameName(String gameName) { this.gameName = gameName; }

   public String getGameFolder() {
      String cleaned = gameName.replaceAll("[^A-Za-z0-9_]+", " ").trim();
      if (cleaned.isEmpty()) return "_";
      String[] words = cleaned.split("\\s+");
      StringBuilder sb = new StringBuilder();
      sb.append(words[0].toLowerCase());
      for (int i = 1; i < words.length; i++) {
         sb.append(Character.toUpperCase(words[i].charAt(0)))
               .append(words[i].substring(1).toLowerCase());
      }
      if (Character.isDigit(sb.charAt(0))) sb.insert(0, '_');
      return sb.toString();
   }

   public Path getGameFolderPath() { return gamesPath.resolve(getGameFolder()); }

   @Override
   public String toString() {
      return "InstallSettings {}" + getJsonString();
   }

   public String getJsonString() {
      return "{\n" +
            "  \"version\": \"" + Config.getInstance().getVersion() + "\",\n" +
            "  \"minecraftPath\": \"" + getMinecraftPath() + "\",\n" +
            "  \"javaPath\": \"" + getJavaPath() + "\",\n" +
            "  \"gamesPath\": \"" + getGamesPath() + "\",\n" +
            "  \"gameName\": \"" + getGameName() + "\",\n" +
            "  \"gameFolder\": \"" + getGameFolder() + "\"\n" +
            "  \"gameFolderPath\": \"" + getGameFolderPath() + "\"\n" +
            "}";
   }

}
