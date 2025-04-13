package com.mordore.install;

import com.mordore.LauncherProfiles;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;

// To test locally with sandbox, set the working directory to sandbox/installer
public class InstallerMain {

   public static void main(String[] args) throws IOException {
      if (args.length < 1) {
         System.err.println("Usage: java InstallerMain <gameFolder>");
         System.exit(1);
      }

      String gameFolder = args[0];
      Path mcPath = locateMinecraft();
      if (mcPath == null || !Files.isDirectory(mcPath)) {
         System.err.println("Error: Could not locate Minecraft directory.");
         System.exit(1);
      }

      Path installerDir = Paths.get("").toAbsolutePath();
      Path gamesSrc = installerDir.resolve("games");
      Path gameSrc = gamesSrc.resolve(gameFolder);
      if (!Files.exists(gameSrc) || !Files.isDirectory(gameSrc)) {
         System.err.println("Error: Source folder " + gameSrc + " not found.");
         System.exit(1);
      }

      Path gamesDest = mcPath.resolve("games");
      Path gameDest = gamesDest.resolve(gameFolder);

      try {
         System.out.println("Copying " + gamesSrc + " → " + gamesDest);
         copyRecursive(gamesSrc, gamesDest);

         Path optionsSrc = mcPath.resolve("options.txt");
         Path optionsDest = gameDest.resolve("options.txt");
         if (Files.exists(optionsSrc)) {
            Files.copy(optionsSrc, optionsDest, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied options.txt");
         } else {
            System.out.println("Warning: options.txt not found, skipping.");
         }

         Path configSrc = mcPath.resolve("config");
         Path configDest = gameDest.resolve("config");
         if (Files.exists(configSrc)) {
            copyRecursive(configSrc, configDest);
            System.out.println("Copied config files.");
         } else {
            System.out.println("Warning: config directory not found, skipping.");
         }

         System.out.println("Install complete.");

      } catch (IOException e) {
         System.err.println("Installation failed: " + e.getMessage());
         e.printStackTrace();
         System.exit(1);
      }

      LauncherProfiles.backup(mcPath);
      LauncherProfiles.addInstallation(mcPath.toString(), gameDest.toString(), "fabric-loader-0.16.13-1.21.5");

   }

   private static Path locateMinecraft() {
      Path devPath = Paths.get("..", "minecraft").toAbsolutePath().normalize();
      if (Files.isDirectory(devPath)) {
         System.out.println("Using local sandbox Minecraft directory: " + devPath);
         return devPath;
      }

      String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
      String home = System.getProperty("user.home");

      if (os.contains("win")) {
         String appData = System.getenv("APPDATA");
         if (appData != null) {
            return Paths.get(appData, ".minecraft");
         }
      } else if (os.contains("mac")) {
         return Paths.get(home, "Library", "Application Support", "minecraft");
      } else if (os.contains("nix") || os.contains("nux")) {
         return Paths.get(home, ".minecraft");
      }

      return null;
   }


   private static void copyRecursive(Path src, Path dest) throws IOException {
      boolean isUnix = System.getProperty("os.name").toLowerCase(Locale.ROOT).matches(".*(nix|nux|mac).*");

      if (!Files.exists(dest)) {
         Files.createDirectories(dest);
      }

      Files.walk(src).forEach(sourcePath -> {
         try {
            Path targetPath = dest.resolve(src.relativize(sourcePath));
            if (Files.isDirectory(sourcePath)) {
               Files.createDirectories(targetPath);
            } else {
               Files.createDirectories(targetPath.getParent()); // ensure parent exists
               Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

               if (isUnix && targetPath.toString().endsWith(".sh")) {
                  targetPath.toFile().setExecutable(true, false);
               }
            }
         } catch (IOException e) {
            throw new UncheckedIOException(e);
         }
      });
   }
}
