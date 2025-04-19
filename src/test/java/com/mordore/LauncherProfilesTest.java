package com.mordore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LauncherProfilesTest {

   @TempDir
   Path tempDir;

   Path launcherProfilesPath;

   @BeforeEach
   public void setUp() throws IOException {
      launcherProfilesPath = tempDir.resolve("launcher_profiles.json");
      String content = """
         {
           "profiles": {
             "profile-1": {
               "gameDir": "games/testgame",
               "lastVersionId": "version-1"
             },
             "profile-2": {
               "gameDir": "games/testgame2",
               "lastVersionId": "version-2"
             }
           }
         }
         """;
      Files.writeString(launcherProfilesPath, content);
   }

   @Test
   void resolvesAbsolutePathUnchanged(@TempDir Path tempDir) {
      Path absolutePath = tempDir.resolve("games/testgame").toAbsolutePath().normalize();
      Path result = LauncherProfilesTestHarness.resolveGameDir(absolutePath.toString(), tempDir);
      assertEquals(absolutePath, result);
   }

   @Test
   void resolvesRelativePathAgainstMinecraftDir(@TempDir Path tempDir) {
      Path expected = tempDir.resolve("games/testgame").toAbsolutePath().normalize();
      Path result = LauncherProfilesTestHarness.resolveGameDir("games/testgame", tempDir);
      assertEquals(expected, result);
   }

   @Test
   public void findsVersionForAbsolutePath() {
      Path gameDir = tempDir.resolve("games/testgame");
      String version = LauncherProfiles.findCurrentVersion(tempDir, gameDir);
      assertEquals("version-1", version);
   }

//   @Test
//   public void findsVersionForRelativePath() {
//      String version = LauncherProfiles.findCurrentVersion(tempDir.toString(), "games/testgame");
//      assertEquals("version-1", version);
//   }

   @Test
   public void returnsNullForMissingProfile() {
      Path missingDir = tempDir.resolve("nonexistent");
      String version = LauncherProfiles.findCurrentVersion(tempDir, missingDir);
      assertNull(version);
   }

   @Test
   public void updatesVersionIfMatch() throws IOException {
      Path gameDir = tempDir.resolve("games/testgame");
      boolean updated = LauncherProfiles.updateVersion(tempDir, gameDir, "new-version");
      assertTrue(updated);
      String updatedJson = Files.readString(launcherProfilesPath);
      assertTrue(updatedJson.contains("new-version"));
   }

   @Test
   public void doesNotUpdateIfNoMatch() throws IOException {
      Path gameDir = tempDir.resolve("games/unknown");
      boolean updated = LauncherProfiles.updateVersion(tempDir, gameDir, "new-version");
      assertFalse(updated);
      String json = Files.readString(launcherProfilesPath);
      assertFalse(json.contains("new-version"));
   }

   // Internal test harness to expose private method
   static class LauncherProfilesTestHarness extends LauncherProfiles {
      public static Path resolveGameDir(String gameDirPath, Path minecraftDir) {
         return LauncherProfiles.resolveGameDir(gameDirPath, minecraftDir);
      }
   }
}
