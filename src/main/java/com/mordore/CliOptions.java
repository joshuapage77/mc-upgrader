package com.mordore;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class CliOptions {
   @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message")
   public boolean help;

   @Option(names = {"-n", "--non-destructive"}, description = "Non-destructive (dry run)")
   public boolean dryRun;

   @Option(names = {"-v", "--verbose"}, description = "Verbose output (debug log level)")
   public boolean verbose;

   @Option(names = {"-r", "--required-only"}, description = "Use required-only mods to determine upgrade version")
   public boolean requiredOnly;

   @Option(names = {"-s", "--specific-version"}, description = "Force upgrade/downgrade to specific Minecraft version. Overrides range")
   public String specificVersion;

   @Option(names = {"--range-start"}, description = "Force upgrade/downgrade to specific Minecraft version range")
   public String rangeStart;

   @Option(names = {"--range-end"}, description = "Force upgrade/downgrade to specific Minecraft version range")
   public String rangeEnd;

   @Parameters(arity = "0..1", description = "Optional game name to upgrade (default: all)")
   public String game;
}