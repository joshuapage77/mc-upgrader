package com.mordore.config;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class InstallerOptions {
   @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message")
   public boolean help;

   @Option(names = {"-v", "--verbose"}, description = "Verbose output (debug log level)")
   public boolean verbose;

}