package com.mordore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import com.mordore.pojo.InstallSettings;

public class Utils {
   private static final ObjectMapper mapper = new ObjectMapper();
   static {
      mapper.registerModule(new JavaTimeModule());
   }

   public static String expandPath(String path) {
      if (path.startsWith("~")) {
         return path.replaceFirst("~", System.getProperty("user.home"));
      }
      return path;
   }

   public static String prettyPrint(Object obj) {
      try {
         return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
      } catch (Exception e) {
         return "Failed to serialize object: " + e.getMessage();
      }
   }
   public static Path findMinecraftDirectory() {
      Path devPath = Paths.get("sandbox").toAbsolutePath().normalize();
      if (Files.isDirectory(devPath)) {
         devPath = devPath.resolve("minecraft");
         return devPath;
      }
      String userHome = System.getProperty("user.home");

      if (isWindows()) {
         String appData = System.getenv("APPDATA");
         if (appData != null) {
            Path mcPath = Paths.get(appData, ".minecraft");
            if (Files.isDirectory(mcPath)) return mcPath;
         }
      } else if (isMac()) {
         Path mcPath = Paths.get(userHome, "Library", "Application Support", "minecraft");
         if (Files.isDirectory(mcPath)) return mcPath;
      } else {
         Path mcPath = Paths.get(userHome, ".minecraft");
         if (Files.isDirectory(mcPath)) return mcPath;
      }

      return null;
   }

   public static Path findJavaExecutable(String minecraftPath) throws IOException {
      return findJavaExecutable(Path.of(minecraftPath));
   }
   public static Path findJavaExecutable(Path minecraftPath) throws IOException {
      if (isWindows()) {
         String localAppData = System.getenv("LOCALAPPDATA");
         // On my windows machine, minecraftPath embedded java was installed to:
         // %LOCALAPPDATA%\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\runtime\java-runtime-delta\windows-x64\java-runtime-delta\bin\javaw.exe
         List<Path> javaPaths = findFolders(Path.of(localAppData), "Microsoft.", null, false).stream()
               .flatMap(microsoftPath -> {
                  try {
                     return findFolders(microsoftPath, "java-runtime-delta", null, true).stream();
                  } catch (IOException e) {
                     return Stream.empty();
                  }
               })
               .map(runtimePath -> {
                  try {
                     return findFile(runtimePath, List.of("java.exe"));
                  } catch (IOException e) {
                     return null;
                  }
               })
               .filter(Objects::nonNull)
               .toList();
         if (!javaPaths.isEmpty()) return javaPaths.getFirst().toAbsolutePath();
      } else {
         Path runtimeDelta = findFolder(minecraftPath, "java-runtime-delta", null, true);
         Path java = findFile(runtimeDelta, List.of("java"));
         if (java != null) return java.toAbsolutePath();
      }

      // Fallback: JAVA_HOME
      String javaHome = System.getenv("JAVA_HOME");
      if (javaHome != null) {
         Path javaPath = Paths.get(javaHome, "bin", isWindows() ? "java.exe" : "java");
         if (Files.isRegularFile(javaPath)) return javaPath.toAbsolutePath();
      }

      // Fallback: system PATH
      Process p = new ProcessBuilder(isWindows() ? "where" : "which", "java").start();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
         String result = reader.readLine();
         if (result != null && !result.isEmpty()) return Path.of(result.trim()).toAbsolutePath();
      }

      return null;
   }

   public static Path findFile(Path searchPath, List<String> targetNames) throws IOException {
      if (searchPath == null || targetNames == null || targetNames.isEmpty()) return null;

      try (Stream<Path> paths = Files.walk(searchPath.resolve("runtime"))) {
         return paths
               .filter(Files::isRegularFile)
               .filter(p -> targetNames.contains(p.getFileName().toString()))
               .findFirst()
               .map(Path::toAbsolutePath)
               .orElse(null);
      }
   }

   public static Path findFolder(Path searchPath, String filterInclude, String filterExclude, boolean recursive) throws IOException {
      if (searchPath == null || !Files.isDirectory(searchPath)) return null;

      Stream<Path> paths = recursive ? Files.walk(searchPath) : Files.list(searchPath);
      try (paths) {
         return paths
               .filter(Files::isDirectory)
               .filter(p -> filterInclude == null || p.getFileName().toString().startsWith(filterInclude))
               .filter(p -> filterExclude == null || !p.getFileName().toString().startsWith(filterExclude))
               .findFirst()
               .orElse(null);
      }
   }

   public static List<Path> findFolders(Path searchPath, String filterInclude, String filterExclude, boolean recursive) throws IOException {
      if (searchPath == null || !Files.isDirectory(searchPath)) return List.of();

      Stream<Path> paths = recursive ? Files.walk(searchPath) : Files.list(searchPath);
      try (paths) {
         return paths
               .filter(Files::isDirectory)
               .filter(p -> filterInclude == null || p.getFileName().toString().startsWith(filterInclude))
               .filter(p -> filterExclude == null || !p.getFileName().toString().startsWith(filterExclude))
               .toList();
      }
   }

   public static boolean isWindows() {
      return System.getProperty("os.name").toLowerCase().contains("win");
   }

   public static boolean isMac() {
      return System.getProperty("os.name").toLowerCase().contains("mac");
   }

   public static boolean pathContainsSegment(Path path, String segment) {
      for (int i = 0; i < path.getNameCount(); i++) {
         if (path.getName(i).toString().equalsIgnoreCase(segment)) {
            return true;
         }
      }
      return false;
   }

   public static void createRegistryKey(String keyPath, String name, String value) throws IOException, InterruptedException {
      List<String> command = List.of(
            "reg", "add",
            "HKCU\\" + keyPath,
            "/v", name,
            "/t", "REG_SZ",
            "/d", value,
            "/f"
      );
      new ProcessBuilder(command)
            .inheritIO()
            .start()
            .waitFor();
   }

   public static void persistInstallSettings(InstallSettings settings) {
      String os = System.getProperty("os.name").toLowerCase();

      if (os.contains("win")) {
         try {
            createRegistryKey("Software\\MyApp", "InstallSettings", settings.getJsonString());
         } catch (Exception e) {
            throw new RuntimeException("Failed to write to Windows registry", e);
         }
      } else {
         try {
            Path configDir;

            if (os.contains("mac")) {
               configDir = Path.of(System.getProperty("user.home"), "Library", "Application Support", "MyApp");
            } else {
               // Linux and everything else
               String xdg = System.getenv("XDG_CONFIG_HOME");
               if (xdg != null && !xdg.isBlank()) {
                  configDir = Path.of(xdg, "myapp");
               } else {
                  configDir = Path.of(System.getProperty("user.home"), ".config", "myapp");
               }
            }

            Files.createDirectories(configDir);
            Path outFile = configDir.resolve("install.json");
            Files.writeString(outFile, settings.getJsonString(), StandardCharsets.UTF_8);
         } catch (IOException e) {
            throw new RuntimeException("Failed to persist install settings", e);
         }
      }
   }
   private static String getBase64ForIcon(Path iconPath) {
      try {
         byte[] fileBytes = Files.readAllBytes(iconPath);
         String base64 = Base64.getEncoder().encodeToString(fileBytes);
         return "data:image/png;base64," + base64;
      } catch (Exception e) {
         throw new RuntimeException("Failed to encode icon to base64", e);
      }
   }

   public static String getBase64ForIcon(String resourceName) throws IOException {
      try (InputStream is = Utils.class.getClassLoader().getResourceAsStream(resourceName)) {
         if (is == null) {
            if ("icon.png".equals(resourceName)) throw new FileNotFoundException("Resource not found: " + resourceName);
            return getBase64ForIcon(resourceName);
         }
         byte[] bytes = is.readAllBytes();
         return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
      }
   }

   public static URL getSafeIcon(String resourceName) {
      try {
         URL stream = Utils.class.getClassLoader().getResource(resourceName);
         if (stream == null) {
            System.out.println("Fuck you!");
            stream = Utils.class.getClassLoader().getResource("Wumpus.png");
            if (stream == null) throw new RuntimeException("Neither primary nor fallback icon found");
         }
         return stream;
      } catch (Exception e) {
         throw new RuntimeException("Failed to load icon: " + resourceName, e);
      }
   }

   public static void deleteDirectory(Path directory) {
      try {
         if (Files.notExists(directory)) return;
         Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
               Files.delete(file);
               return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
               Files.delete(dir);
               return FileVisitResult.CONTINUE;
            }
         });
      } catch (IOException e) {
         throw new RuntimeException("Failed to delete directory: " + directory, e);
      }
   }


}
