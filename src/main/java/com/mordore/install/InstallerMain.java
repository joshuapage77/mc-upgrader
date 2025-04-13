package com.mordore.install;

import com.mordore.LauncherProfiles;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Locale;

public class InstallerMain {

   private static JTextArea logArea;
   private static JFrame frame;
   private static JLabel descriptionLabel;
   private static JLabel minecraftPathLabel;
   private static JButton installButton;
   private static JButton cancelButton;
   private static JPanel buttonPanel;

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> createAndShowGUI(args));
   }

   private static void createAndShowGUI(String[] args) {
      frame = new JFrame("Tyberian Installer");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setSize(800, 500);

      ImageIcon icon = new ImageIcon(InstallerMain.class.getResource("/icon.png"));
      frame.setIconImage(icon.getImage());
      Toolkit.getDefaultToolkit().setDynamicLayout(true);
      Image altIcon = Toolkit.getDefaultToolkit().getImage(InstallerMain.class.getResource("/icon.png"));
      if (altIcon != null) frame.setIconImage(altIcon);

      descriptionLabel = new JLabel("Description: Installing Domain of Pages game folder and update utility");
      minecraftPathLabel = new JLabel("Minecraft Folder: Unknown");

      JPanel labelsPanel = new JPanel();
      labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));
      labelsPanel.add(descriptionLabel);
      labelsPanel.add(Box.createVerticalStrut(5));
      labelsPanel.add(minecraftPathLabel);

      ImageIcon rawIcon = new ImageIcon(InstallerMain.class.getResource("/icon.png"));
      Image scaledImage = rawIcon.getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH);
      JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.add(labelsPanel, BorderLayout.CENTER);
      topPanel.add(iconLabel, BorderLayout.EAST);
      topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

      logArea = new JTextArea();
      logArea.setEditable(false);
      logArea.setBackground(Color.LIGHT_GRAY);
      logArea.setForeground(UIManager.getColor("Label.foreground"));
      JScrollPane scrollPane = new JScrollPane(logArea);
      scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

      installButton = new JButton("Install");
      cancelButton = new JButton("Cancel");

      installButton.addActionListener(e -> {
         installButton.setEnabled(false);
         cancelButton.setEnabled(false);
         new Thread(() -> runInstall(args)).start();
      });

      cancelButton.addActionListener(e -> System.exit(0));

      buttonPanel = new JPanel();
      buttonPanel.add(installButton);
      buttonPanel.add(cancelButton);

      JPanel contentPanel = new JPanel(new BorderLayout());
      contentPanel.add(topPanel, BorderLayout.NORTH);
      contentPanel.add(scrollPane, BorderLayout.CENTER);
      contentPanel.add(buttonPanel, BorderLayout.SOUTH);

      frame.setContentPane(contentPanel);
      frame.setLocationRelativeTo(null);
      Path mcPath = locateMinecraft();
      if (mcPath != null && Files.isDirectory(mcPath)) {
         updateMinecraftPathLabel(mcPath.toString());
      }
      frame.setVisible(true);
   }

   private static void showExitButton() {
      SwingUtilities.invokeLater(() -> {
         buttonPanel.removeAll();
         JButton exitButton = new JButton("Exit");
         exitButton.addActionListener(e -> System.exit(0));
         buttonPanel.add(exitButton);
         buttonPanel.revalidate();
         buttonPanel.repaint();
      });
   }

   private static void runInstall(String[] args) {
      if (args.length < 1) {
         log("Usage: java InstallerMain <gameFolder>");
         return;
      }

      String gameFolder = args[0];
      Path mcPath = locateMinecraft();
      if (mcPath == null || !Files.isDirectory(mcPath)) {
         log("Error: Could not locate Minecraft directory.");
         return;
      }
      updateMinecraftPathLabel(mcPath.toString());

      Path installerDir = Paths.get("").toAbsolutePath();
      Path gamesSrc = installerDir.resolve("games");
      Path gameSrc = gamesSrc.resolve(gameFolder);
      if (!Files.exists(gameSrc) || !Files.isDirectory(gameSrc)) {
         log("Error: Source folder " + gameSrc + " not found.");
         return;
      }

      Path gamesDest = mcPath.resolve("games");
      Path gameDest = gamesDest.resolve(gameFolder);

      try {
         log("Copying games folder into minecraft");
         copyRecursive(gamesSrc, gamesDest);

         Path optionsSrc = mcPath.resolve("options.txt");
         Path optionsDest = gameDest.resolve("options.txt");
         if (Files.exists(optionsSrc)) {
            Files.copy(optionsSrc, optionsDest, StandardCopyOption.REPLACE_EXISTING);
            log("Copied options.txt");
         } else {
            log("Warning: options.txt not found, skipping.");
         }

         Path configSrc = mcPath.resolve("config");
         Path configDest = gameDest.resolve("config");
         if (Files.exists(configSrc)) {
            copyRecursive(configSrc, configDest);
            log("Copied config files.");
         } else {
            log("Warning: config directory not found, skipping.");
         }
         log("Backing up launcher_profiles.json");
         LauncherProfiles.backup(mcPath);
         log("Adding new installation profile to launcher_profiles.json");
         LauncherProfiles.addInstallation(mcPath.toString(), gameDest.toString(), "fabric-loader-0.16.13-1.21.5");
         log("Waba-laba-dub-dub!");
         log("Install complete.");
         log("");
         log("The new folder that holds game directories and mc-upgrader is:");
         log("    " + gamesDest);
         log("mc-upgrader will upgrade the fabric loader, all mods and shaders");
         log("Execute upgrade.sh or upgrade.bat");
         showExitButton();

      } catch (IOException e) {
         log("Installation failed: " + e.getMessage());
         e.printStackTrace();
      }
   }

   private static void updateMinecraftPathLabel(String path) {
      SwingUtilities.invokeLater(() -> minecraftPathLabel.setText("Minecraft Folder: " + path));
   }

   private static void log(String message) {
      SwingUtilities.invokeLater(() -> logArea.append(LocalDateTime.now() + ": " + message + "\n"));
   }

   private static Path locateMinecraft() {
      Path devPath = Paths.get("..", "minecraft").toAbsolutePath().normalize();
      if (Files.isDirectory(devPath)) {
         log("Using local sandbox Minecraft directory: " + devPath);
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
               Files.createDirectories(targetPath.getParent());
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
