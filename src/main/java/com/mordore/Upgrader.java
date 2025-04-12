package com.mordore;

import com.mordore.config.Config;
import com.mordore.mods.Mod;
import com.mordore.mods.ModrinthArtifactProvider;
import com.mordore.pojo.ModVersion;

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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.mordore.config.ModConfig;

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
         ch.qos.logback.classic.Logger root =
               (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
         root.setLevel(ch.qos.logback.classic.Level.DEBUG);

         log.debug("Verbose mode enabled");
      }

      if (opts.specificVersion != null) {
         opts.rangeStart = opts.specificVersion;
         opts.rangeEnd = opts.specificVersion;
      }

      Config config = Config.getInstance();

      for (Config.GameConfig game : config.getGames()) {
         if (opts.game != null && !game.name.equalsIgnoreCase(opts.game)) continue;
         log.info("Checking game: {} at path: {}", game.name, game.getPath());

         String fabricGameVersion = LauncherProfiles.findCurrentVersion(config.getMinecraft(), game.getPath());
         String minecraftGameVersion = extractMinecraftVersion(fabricGameVersion);

         String gameRangeStart = (opts.rangeStart != null) ? opts.rangeStart : minecraftGameVersion;

         log.info("Associated Install current version: {}, minecraft game version: {}", fabricGameVersion, minecraftGameVersion);
         List<String> searchVersions = ModrinthArtifactProvider.getReleaseMinecraftVersions(gameRangeStart, true, opts.rangeEnd, true);

         List<Mod> mods = Config.getModConfigs(game.getPath()).stream()
               .map(mod -> new Mod(mod, searchVersions))
               .toList();

         List<String> validVersions = resolveVersions(mods, searchVersions, !opts.requiredOnly);

         if (validVersions.isEmpty()) {
            log.error("Targed minecraft versions [{}, {}] not available for all{} mods", gameRangeStart, opts.rangeEnd == null ? "latest" : opts.rangeEnd, opts.requiredOnly ? " required" : "");
            log.error("Aborting");
            continue;
         }

         String selectedVersion = validVersions.getFirst();
         if (opts.specificVersion != null) {
            if (!validVersions.contains(opts.specificVersion)) {
               log.error("Requested version {} is not available for all{} mods", opts.specificVersion, opts.requiredOnly ? " required" : "");
               log.error("Aborting");
               continue;
            }
            selectedVersion = opts.specificVersion;
         }
         log.debug("Available Versions: {}", String.join(", ", validVersions));
         log.info("Selected Version: {}", selectedVersion);
         for (Mod mod : mods) {
            ModVersion version = mod.getModVersion(selectedVersion);
            log.info("\tMod: {}\tVersion: {}\tUrl: {}",
                  String.format("%-20s", mod.getModConfig().slug),
                  String.format("%-25s", (version != null) ? version.getVersionNumber() : "Version Unavailable"),
                  (version != null) ? version.getUrl() : "");
         }

         String versionChangeString = "Upgrade";
         if (selectedVersion.compareTo(minecraftGameVersion) < 0) {
            versionChangeString = "DOWNGRADE";
            log.warn("NOTE: Downgrades minecraft from {} to {}", minecraftGameVersion, selectedVersion);
         }

         if (opts.dryRun) {
            log.info("[Dry Run] Would {} to: {}", versionChangeString, selectedVersion);
            return;
         }
         System.out.print("Proceed with " + versionChangeString + "? [y/N]: ");
         Scanner scanner = new Scanner(System.in);
         String input = scanner.nextLine().trim().toLowerCase();
         if (!input.equals("y")) {
            System.out.println("Aborting");
            continue;
         }
         log.info("{}ing {} to {}", versionChangeString.substring(0, versionChangeString.length() - 1), game.name, selectedVersion);
         String newFabricInstallId = upgradeFabricVersion(config.getMinecraft(), selectedVersion);
         LauncherProfiles.updateVersion(config.getMinecraft(), game.getPath(), newFabricInstallId);

         updateMods(mods, selectedVersion, game.getPath());
         log.info("All changes complete");
      }
   }

   // logic - we only count required unless all are required. The number needed to consider a version depends on whether all mods need a version
   // or not (allRequired)
   private static List<String> resolveVersions(List<Mod> mods, List<String> searchVersions, boolean allRequired) {
      long requiredCount = mods.stream()
            .filter(mod -> allRequired || !mod.getModConfig().optional)
            .count();
      log.debug("Count of required mods for version support: {}", requiredCount);
      Map<String, Integer> versionCount = new HashMap<>();
      for (Mod mod : mods) {
         for (String version : mod.getMinecraftVersions()) {
            if (allRequired || !mod.getModConfig().optional) {
               Integer count = (versionCount.containsKey(version)) ? versionCount.get(version) : Integer.valueOf(0);
               count++;
               versionCount.put(version, count);
            }
         }
      }

      for (String version : versionCount.keySet()) {
         log.debug("  Mods supporting version [{}]: {}", version, versionCount.get(version));
      }
      List<String> validVersions = versionCount.entrySet().stream()
            .filter(e -> e.getValue() == requiredCount)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());;
      log.debug("Versions with required support: {}", String.join(", ", validVersions));
      validVersions.sort(Comparator.reverseOrder());
      return validVersions;
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

   public static void updateMods(List<Mod> mods, String selectedVersion, String gamePath) {
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

      for (Mod mod : mods) {
         ModVersion version = mod.getModVersion(selectedVersion);
         String modName = mod.getModConfig().slug;

         Path destDir;
         String extension;

         if ("shader".equals(mod.getModConfig().type)) {
            destDir = shaderDir;
            extension = ".zip";
         } else {
            destDir = modsDir;
            extension = ".jar";
         }

         try (Stream<Path> files = Files.list(destDir)) {
            files.filter(p -> p.getFileName().toString().startsWith(modName) && p.getFileName().toString().endsWith(extension))
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
            Path targetFile = destDir.resolve(modName + "-" + version.getVersionNumber() + extension);
            Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
         } catch (IOException e) {
            throw new RuntimeException("Failed to download mod: " + modName, e);
         }
      }
   }
}
