package com.mordore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

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
      if (minecraftPath != null) {
         try (Stream<Path> paths = Files.walk(minecraftPath.resolve("runtime"))) {
            Optional<Path> java = paths
                  .filter(Files::isRegularFile)
                  .filter(p -> p.getFileName().toString().equals("java") || p.getFileName().toString().equals("java.exe"))
                  .findFirst();
            if (java.isPresent()) return java.get().toAbsolutePath();
         }
      }

      // Fallback: JAVA_HOME
      String javaHome = System.getenv("JAVA_HOME");
      if (javaHome != null) {
         Path javaPath = Paths.get(javaHome, "bin", isWindows() ? "java.exe" : "java");
         if (Files.isRegularFile(javaPath)) return javaPath;
      }

      // Fallback: system PATH
      Process p = new ProcessBuilder(isWindows() ? "where" : "which", "java").start();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
         String result = reader.readLine();
         if (result != null && !result.isEmpty()) return Path.of(result.trim());
      }


      return null;
   }

   private static boolean isWindows() {
      return System.getProperty("os.name").toLowerCase().contains("win");
   }

   private static boolean isMac() {
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

}
