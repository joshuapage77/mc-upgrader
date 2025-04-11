package com.mordore.pojo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ModVersion implements Comparable<ModVersion> {
   private final List<String> gameVersions;
   private final String versionNumber;
   private final String url;
   private final OffsetDateTime published;

   public ModVersion(JsonNode version) {
      gameVersions = new ArrayList<>();
      JsonNode gameVersionsNode = version.path("game_versions");
      if (gameVersionsNode.isArray()) {
         for (JsonNode v : gameVersionsNode) {
            gameVersions.add(v.asText());
         }
      }

      versionNumber = version.get("version_number").asText();
      url = version.path("files").path(0).path("url").asText();
      published = OffsetDateTime.parse(version.path("date_published").asText());
   }
   @Override
   public int compareTo(ModVersion other) {
      return this.versionNumber.compareTo(other.versionNumber);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ModVersion that = (ModVersion) o;
      return versionNumber.equals(that.versionNumber);
   }

   @Override
   public int hashCode() {
      return versionNumber.hashCode();
   }
   public List<String> getGameVersions() { return gameVersions; }
   public String getVersionNumber() { return versionNumber; }
   public String getUrl() { return url; }
   public OffsetDateTime getPublished() { return published; }
}