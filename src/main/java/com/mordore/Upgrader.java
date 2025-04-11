package com.mordore;

import com.mordore.config.Config;
import com.mordore.pojo.ModVersion;
import com.mordore.pojo.VersionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import picocli.CommandLine;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Upgrader {
   private static final Logger log = LoggerFactory.getLogger(Upgrader.class);

   public static void main(String[] args) throws IOException, InterruptedException {
      CliOptions opts = new CliOptions();
      CommandLine cmd = new CommandLine(opts);

      try {
         cmd.parseArgs(args);
      } catch (Exception e) {
         System.out.println(e.getMessage());
         opts.help = true;
      }

      if (opts.help) {
         cmd.usage(System.out);
         return;
      }

      if (opts.verbose) {
         System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
         log.debug("Verbose mode enabled");
      }

      Config config = Config.getInstance();

      for (Config.GameConfig game : config.getGames()) {
         if (opts.game != null && !game.name.equalsIgnoreCase(opts.game)) continue;
         log.info("Checking game: {} at path: {}", game.name, game.getPath());

         String fabricGameVersion = LauncherProfiles.findCurrentVersion(config.getMinecraft(), game.getPath());
         String minecraftGameVersion = extractMinecraftVersion(fabricGameVersion);

         log.info("Associated Install current version: {}, minecraft game version: {}", fabricGameVersion, minecraftGameVersion);
         List<String> searchVersions;
         if (opts.specificVersion != null && !opts.specificVersion.isEmpty()) {
            searchVersions = List.of(opts.specificVersion);
         } else {
            searchVersions = ModCompatibilityResolver.getNewerGameVersions(minecraftGameVersion);
         }
         List<Config.ModConfig> mods = Config.getModConfigs(game.getPath());
         VersionResult versionResult = ModCompatibilityResolver.runVersionCheck(mods, game.getPath(), searchVersions);
         log.debug("versionResult - required: {}  all {}", versionResult.latestRequired(), versionResult.latestAll());
         String chosenVersion = opts.requiredOnly ? versionResult.latestRequired() : versionResult.latestAll();
         log.info("Chosen mincraft version: {}", chosenVersion);
         String versionChangeString = "Upgrade";
         if (chosenVersion.compareTo(minecraftGameVersion) < 0) {
            versionChangeString = "DOWNGRADE";
            log.warn("NOTE: Downgrades minecraft from {} to {}", minecraftGameVersion, chosenVersion);
         }
         Map<String, ModVersion> modVersions = versionResult.versionToMod().get(chosenVersion);

         for (String slug : modVersions.keySet()) {
            ModVersion version = modVersions.get(slug);
            log.info("\tMod: {}\tVersion: {}\tUrl: {}",
                  String.format("%-15s", slug),
                  String.format("%-25s", (version != null) ? version.getVersionNumber() : "Version Unavailable"),
                  (version != null) ? version.getUrl() : "");
         }

         if (opts.dryRun) {
            log.info("[Dry Run] Would {} to: {}", versionChangeString, chosenVersion);
            return;
         }
         System.out.print("Proceed with " + versionChangeString + "? [y/N]: ");
         Scanner scanner = new Scanner(System.in);
         String input = scanner.nextLine().trim().toLowerCase();
         if (!input.equals("y")) {
            System.out.println("Aborting");
            continue;
         }
         log.info("{}ing {} to {}", versionChangeString.substring(0, versionChangeString.length() - 1), game.name, chosenVersion);
         String newFabricInstallId = upgradeFabricVersion(config.getMinecraft(), chosenVersion);
         LauncherProfiles.updateVersion(config.getMinecraft(), game.getPath(), newFabricInstallId);

         updateMods(mods, modVersions, game.getPath());
         log.info("All changes complete");
      }
   }
   public static String extractMinecraftVersion(String fabricVersionId) {
      int lastDash = fabricVersionId.lastIndexOf('-');
      if (lastDash == -1 || lastDash == fabricVersionId.length() - 1) {
         throw new IllegalArgumentException("Invalid fabric version format: " + fabricVersionId);
      }
      return fabricVersionId.substring(lastDash + 1);
   }

   public static String upgradeFabricVersion(String minecraftDir, String mcVersion) {
      try {
         String loaderVersion = fetchLatestVersionFromMaven("fabric-loader");
         String installerVersion = fetchLatestVersionFromMaven("fabric-installer");

         log.info("Installing Fabric version - Loader: {} Installer: {}", loaderVersion, installerVersion);

         String versionFolderName = String.format("fabric-loader-%s-%s", loaderVersion, mcVersion);
         Path versionPath = Paths.get(minecraftDir, "versions", versionFolderName);

         if (Files.exists(versionPath)) {
            log.info("Fabric version {} already installed. Skipping installation.", versionFolderName);
         } else {
            String installerJarUrl = String.format(
                  "https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar",
                  installerVersion, installerVersion
            );

            Path installerJar = Files.createTempFile("fabric-installer", ".jar");
            try (InputStream in = URI.create(installerJarUrl).toURL().openStream()) {
               Files.copy(in, installerJar, StandardCopyOption.REPLACE_EXISTING);
            }
            // Backup launcher_profiles - it gets updated by fabric installer
            Path backupPath = LauncherProfiles.backup();
            FabricInstaller.installFabric(installerJar.toString(), minecraftDir, mcVersion, loaderVersion);
            LauncherProfiles.restore(backupPath);
            Files.deleteIfExists(installerJar);
         }
         return versionFolderName;
      } catch (Exception e) {
         throw new RuntimeException("Failed to install Fabric game", e);
      }
   }

   private static String fetchLatestVersionFromMaven(String artifact) throws IOException {
      String metadataUrl = "https://maven.fabricmc.net/net/fabricmc/" + artifact + "/maven-metadata.xml";
      try (InputStream input = URI.create(metadataUrl).toURL().openStream()) {
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         DocumentBuilder builder = factory.newDocumentBuilder();
         Document doc;
         try {
            doc = builder.parse(input);
         } catch (Exception e) {
            throw new IOException("Failed to parse maven-metadata.xml for " + artifact, e);
         }

         NodeList versioning = doc.getElementsByTagName("versioning");
         if (versioning.getLength() == 0) throw new IOException("Missing <versioning> in metadata");

         Element versioningElement = (Element) versioning.item(0);
         return versioningElement.getElementsByTagName("release").item(0).getTextContent();
      } catch (Exception e) {
         throw new IOException("Error fetching latest version for " + artifact, e);
      }
   }

   public static void updateMods(List<Config.ModConfig> mods, Map<String, ModVersion> modVersions, String gamePath) {
      Path modsDir = Paths.get(gamePath, "mods");
      if (!Files.exists(modsDir)) {
         try {
            Files.createDirectories(modsDir);
         } catch (IOException e) {
            throw new RuntimeException("Failed to create mods directory at " + modsDir, e);
         }
      }

      Path shaderDir = Paths.get(gamePath, "shaderpacks");
      if (!Files.exists(shaderDir)) {
         try {
            Files.createDirectories(shaderDir);
         } catch (IOException e) {
            throw new RuntimeException("Failed to create shaderpacks directory at " + shaderDir, e);
         }
      }

      Map<String, Config.ModConfig> modMap = mods.stream()
            .collect(Collectors.toMap(mod -> mod.slug, mod -> mod));

      for (String slug : modVersions.keySet()) {
         ModVersion version = modVersions.get(slug);

         Path destDir;
         String extension;

         if ("shader".equals(modMap.get(slug).type)) {
            destDir = shaderDir;
            extension = ".zip";
         } else {
            destDir = modsDir;
            extension = ".jar";
         }

         try (Stream<Path> files = Files.list(destDir)) {
            files.filter(p -> p.getFileName().toString().startsWith(slug) && p.getFileName().toString().endsWith(extension))
                  .forEach(p -> {
                     try {
                        Files.deleteIfExists(p);
                     } catch (IOException e) {
                        log.warn("Failed to delete old file: {}", p, e);
                     }
                  });
         } catch (IOException e) {
            log.warn("Failed to list directory: {}", destDir, e);
         }
         if (version == null) continue;

         // Download new mod jar
         try (InputStream in = URI.create(version.getUrl()).toURL().openStream()) {
            Path targetFile = destDir.resolve(slug + "-" + version.getVersionNumber() + extension);
            Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
         } catch (IOException e) {
            throw new RuntimeException("Failed to download mod: " + slug, e);
         }
      }
   }
}
