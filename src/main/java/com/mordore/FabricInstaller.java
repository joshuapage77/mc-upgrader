package com.mordore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import com.mordore.config.Config;

public class FabricInstaller {
   private final static Config config = Config.getInstance();
   public static void installFabric(String installerJar, Path mcDir, String mcVersion, String loaderVersion) throws IOException, InterruptedException {
      String javaStr = config.getJava().toString();
      List<String> command = List.of(
            javaStr, "-jar", installerJar,
            "client",
            "-dir", mcDir.toString(),
            "-mcversion", mcVersion,
            "-loader", loaderVersion
      );

      ProcessBuilder pb = new ProcessBuilder(command);
      pb.inheritIO(); // redirects output to your console
      Process process = pb.start();
      int exitCode = process.waitFor();

      if (exitCode != 0) {
         throw new RuntimeException("Fabric installer failed with exit code " + exitCode);
      }
   }
}