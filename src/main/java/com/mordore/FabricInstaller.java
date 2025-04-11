package com.mordore;

import java.io.IOException;
import java.util.List;

public class FabricInstaller {
   public static void installFabric(String installerJar, String mcDir, String mcVersion, String loaderVersion) throws IOException, InterruptedException {
      List<String> command = List.of(
            "java", "-jar", installerJar,
            "client",
            "-dir", mcDir,
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