package com.mordore.install;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mordore.LauncherProfiles;
import com.mordore.Utils;
import com.mordore.config.Config;
import com.mordore.config.InstallerOptions;
import com.mordore.pojo.InstallSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Locale;
import java.net.URL;
import java.net.URI;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class InstallerMain {
   private static final Logger log = LoggerFactory.getLogger(InstallerMain.class);
   private static JTextArea logArea;
   private static JFrame frame;
   private static JLabel minecraftPathValue;
   private static JLabel javaPathValue;
   private static JButton installButton;
   private static JButton cancelButton;
   private static JTextField gamesPath;
   private static JTextField gameName;
   private static JPanel buttonPanel;

   public static void main(String[] args) {
      InstallerOptions opts = new InstallerOptions();
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
      SwingUtilities.invokeLater(() -> createAndShowGUI(args));
   }

   private static void lockWidth(JComponent c, int width) {
      Dimension size = new Dimension(width, c.getPreferredSize().height);
      c.setMinimumSize(size);
      c.setPreferredSize(size);
      c.setMaximumSize(size);
   }

   private static void onTextChange(JTextField field, Consumer<String> handler) {
      field.getDocument().addDocumentListener(new DocumentListener() {
         public void insertUpdate(DocumentEvent e) { handler.accept(field.getText()); }
         public void removeUpdate(DocumentEvent e) { handler.accept(field.getText()); }
         public void changedUpdate(DocumentEvent e) { handler.accept(field.getText()); }
      });
   }


   private static void createAndShowGUI(String[] args) {
      frame = new JFrame("Tyberian Installer   -  " + Config.getInstance().getVersion());
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setSize(1000, 600);

      ImageIcon icon = new ImageIcon(Utils.getSafeIcon("gear.png"));
      frame.setIconImage(icon.getImage());
      Toolkit.getDefaultToolkit().setDynamicLayout(true);
      Image altIcon = Toolkit.getDefaultToolkit().getImage(Utils.getSafeIcon("gear.png"));
      if (altIcon != null) frame.setIconImage(altIcon);

      final InstallSettings installSettings = new InstallSettings();
      ObjectMapper mapper = new ObjectMapper();

      try (InputStream is = Utils.getResource("game.json")) {
         if (is == null) throw new RuntimeException("game.json not found");
         JsonNode root = mapper.readTree(is);
         installSettings.setGameName(root.get("name").asText());
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
      String abortReason = "";

      try {
         if (Config.getInstance().getMinecraft() == null) throw new RuntimeException("Minecraft path could not be determined");
         installSettings.setMinecraftPath(Path.of(Config.getInstance().getMinecraft()));
         if (Config.getInstance().getJava() == null) throw new RuntimeException("Java path could not be determined");
         installSettings.setJavaPath(Config.getInstance().getJava());
         installSettings.setGamesPath(installSettings.getMinecraftPath().resolve("games"));
      } catch (RuntimeException re) {
         abortReason = re.getMessage();
      }

      JLabel descriptionLabel = new JLabel("Description:");
      JLabel descriptionValue = new JLabel("Installing " + installSettings.getGameName() + " game folder and update utility");

      JLabel minecraftPathLabel = new JLabel("Minecraft Folder:");
      minecraftPathValue = new JLabel("Unknown");

      JLabel javaPathLabel = new JLabel("Java Folder:");
      javaPathValue = new JLabel("Unknown");

      // Match label widths
      int infoLabelWidth = Stream.of(descriptionLabel, minecraftPathLabel, javaPathLabel)
            .mapToInt(l -> l.getPreferredSize().width)
            .max()
            .orElse(0);

      lockWidth(descriptionLabel, infoLabelWidth);
      lockWidth(minecraftPathLabel, infoLabelWidth);
      lockWidth(javaPathLabel, infoLabelWidth);

      // Build rows
      JPanel descriptionRow = new JPanel();
      descriptionRow.setLayout(new BoxLayout(descriptionRow, BoxLayout.X_AXIS));
      descriptionRow.add(descriptionLabel);
      descriptionRow.add(Box.createHorizontalStrut(10));
      descriptionRow.add(descriptionValue);

      JPanel mcPathRow = new JPanel();
      mcPathRow.setLayout(new BoxLayout(mcPathRow, BoxLayout.X_AXIS));
      mcPathRow.add(minecraftPathLabel);
      mcPathRow.add(Box.createHorizontalStrut(10));
      mcPathRow.add(minecraftPathValue);

      JPanel javaPathRow = new JPanel();
      javaPathRow.setLayout(new BoxLayout(javaPathRow, BoxLayout.X_AXIS));
      javaPathRow.add(javaPathLabel);
      javaPathRow.add(Box.createHorizontalStrut(10));
      javaPathRow.add(javaPathValue);

      descriptionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
      mcPathRow.setAlignmentX(Component.LEFT_ALIGNMENT);
      javaPathRow.setAlignmentX(Component.LEFT_ALIGNMENT);


      JPanel infoPanel = new JPanel();
      infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
      infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0)); // bottom padding
      infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

      // Add to infoPanel
      infoPanel.add(descriptionRow);
      infoPanel.add(Box.createVerticalStrut(5));
      infoPanel.add(mcPathRow);
      infoPanel.add(Box.createVerticalStrut(5));
      infoPanel.add(javaPathRow);

      JPanel labelsPanel = new JPanel();
      labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));

      labelsPanel.add(Box.createVerticalStrut(10));

      // Games folder row
      JLabel gamesFolderLabel = new JLabel("Games Folder:");
      gamesPath = new JTextField(installSettings.getGamesPath().toString(), 30);
      onTextChange(gamesPath, text -> installSettings.setGamesPath(Path.of(text)));
      JButton browseButton = new JButton("Browse");

      browseButton.addActionListener(e -> {
         JFileChooser chooser = new JFileChooser(installSettings.getMinecraftPath().toFile());
         chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
         chooser.setDialogTitle("Select Parent Folder");
         chooser.setApproveButtonText("Select");
         if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File parent = chooser.getSelectedFile();
            Path gamesDir = parent.toPath().resolve("games");
            gamesPath.setText(gamesDir.toAbsolutePath().toString());
         }
      });
      // Game name row
      JLabel gameNameLabel = new JLabel("Game Name:");
      gameName = new JTextField(installSettings.getGameName(), 30);
      onTextChange(gameName, installSettings::setGameName);

      int labelWidth = Math.max(
            gamesFolderLabel.getPreferredSize().width,
            gameNameLabel.getPreferredSize().width
      );
      Dimension labelSize = new Dimension(labelWidth, gamesFolderLabel.getPreferredSize().height);

      gamesFolderLabel.setPreferredSize(labelSize);
      gameNameLabel.setPreferredSize(labelSize);

      // layout for rows
      JPanel gamesRow = new JPanel();
      gamesRow.setLayout(new BoxLayout(gamesRow, BoxLayout.X_AXIS));
      gamesRow.add(gamesFolderLabel);
      gamesRow.add(Box.createHorizontalStrut(10));
      gamesRow.add(gamesPath);
      gamesRow.add(Box.createHorizontalStrut(10));
      gamesRow.add(browseButton);
      labelsPanel.add(gamesRow);

      labelsPanel.add(Box.createVerticalStrut(5));

      JPanel nameRow = new JPanel();
      nameRow.setLayout(new BoxLayout(nameRow, BoxLayout.X_AXIS));

      nameRow.add(gameNameLabel);
      nameRow.add(Box.createHorizontalStrut(10));
      nameRow.add(gameName);
      labelsPanel.add(nameRow);


      ImageIcon rawIcon = new ImageIcon(Utils.getSafeIcon("game-icon.png"));
      Image scaledImage = rawIcon.getImage().getScaledInstance(140, 140, Image.SCALE_SMOOTH);
      JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
      JPanel topPanel = new JPanel(new BorderLayout());
      JPanel paddedLabels = new JPanel();
      paddedLabels.setLayout(new BorderLayout());
      paddedLabels.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));

      paddedLabels.add(infoPanel, BorderLayout.NORTH);
      paddedLabels.add(labelsPanel, BorderLayout.SOUTH);

      topPanel.add(paddedLabels, BorderLayout.CENTER);
      topPanel.add(iconLabel, BorderLayout.EAST);
      topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

      logArea = new JTextArea();
      logArea.setEditable(false);
      logArea.setBackground(Color.LIGHT_GRAY);
      logArea.setForeground(UIManager.getColor("Label.foreground"));
      logArea.setMargin(new Insets(5, 10, 5, 10));
      JScrollPane scrollPane = new JScrollPane(logArea);
      scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

      installButton = new JButton("Install");
      cancelButton = new JButton("Cancel");

      installButton.addActionListener(e -> {
         installButton.setEnabled(false);
         cancelButton.setEnabled(false);
         new Thread(() -> {
            try {
               runInstall(installSettings);
            } catch (IOException | URISyntaxException ex) {
               uiLog(ex.getMessage());
               throw new RuntimeException(ex);
            } finally {
               showExitButton();
            }
         }).start();
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

      if (!abortReason.isEmpty()) {
         uiLog(abortReason);
         showExitButton();
      } else {
         updateInstallPathLabels(installSettings);
         if (Utils.pathContainsSegment(installSettings.getMinecraftPath(), "sandbox")) {
            uiLog("-- Using Sandbox minecraft --");
         }
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

   private static void runInstall(InstallSettings settings) throws IOException, URISyntaxException {
      log.debug("Current directory: {}", System.getProperty("user.dir"));

      Path gameSrc = extractResourceDirectory("/copy");
      if (!Files.exists(gameSrc) || !Files.isDirectory(gameSrc)) {
         uiLog("Error: Source folder " + gameSrc + " not found.");
         return;
      }

      Path mcupgraderSrc = extractResourceDirectory("/installer/mc-upgrader");
      if (!Files.exists(mcupgraderSrc) || !Files.isDirectory(mcupgraderSrc)) {
         uiLog("Error: Source folder " + mcupgraderSrc + " not found.");
         cleanupTempFolder(gameSrc);
         return;
      }

      try {
         uiLog("Creating game folder");
         Files.createDirectories(settings.getGameFolderPath());
         uiLog("Installing game content");
         copyRecursive(gameSrc, settings.getGameFolderPath());
         uiLog("Installing mc-upgrader");
         Path upgrader = settings.getGamesPath().resolve("mc-upgrader");
         copyRecursive(mcupgraderSrc, upgrader);
         uiLog("generating properties.json");
         generateProperties(settings, upgrader);
         Path optionsSrc = settings.getMinecraftPath().resolve("options.txt");
         Path optionsDest = settings.getGameFolderPath().resolve("options.txt");
         if (Files.exists(optionsSrc)) {
            Files.copy(optionsSrc, optionsDest, StandardCopyOption.REPLACE_EXISTING);
            uiLog("Copied options.txt");
         } else {
            uiLog("Warning: options.txt not found, skipping.");
         }

         Path configSrc = settings.getMinecraftPath().resolve("config");
         Path configDest = settings.getGameFolderPath().resolve("config");
         if (Files.exists(configSrc)) {
            copyRecursive(configSrc, configDest);
            uiLog("Copied config files.");
         } else {
            uiLog("Warning: config directory not found, skipping.");
         }

         uiLog("Backing up launcher_profiles.json");
         LauncherProfiles.backup(settings.getMinecraftPath());
         uiLog("Adding new installation profile to launcher_profiles.json");
         LauncherProfiles.addInstallation(settings, "fabric-loader-0.16.13-1.21.5");
         if (Utils.isWindows()) {
            uiLog("Creating user registry key for Java location");
            Utils.createRegistryKey("Software\\TyberianInstaller", "JavaPath", settings.getJavaPath().toString());
            Path source = Path.of("mc-upgrader.exe");
            if (!Files.exists(source)) {
               throw new RuntimeException("mc-upgrader.exe is missing from install package");
            }
            Files.copy(source, settings.getGamesPath().resolve("mc-upgrader"));
            Files.deleteIfExists(settings.getGamesPath().resolve("mc-upgrader").resolve("mc-upgrader.jar"));
            Files.deleteIfExists(settings.getGamesPath().resolve("mc-upgrader").resolve("upgrader.sh"));
         }
         uiLog("Waba-laba-dub-dub!");
         uiLog("Install complete.");
         uiLog("");
         uiLog("The new folder that holds game directories and mc-upgrader is:");
         uiLog("    " + settings.getGameFolder());
         uiLog("mc-upgrader will upgrade the fabric loader, all mods and shaders");
         uiLog("Execute upgrade.sh or upgrade.bat");

         uiLog("Install complete.");
      } catch (Exception e) {
         uiLog("Installation failed: " + e.getMessage());
         e.printStackTrace();
      } finally {
         cleanupTempFolder(gameSrc);
         cleanupTempFolder(mcupgraderSrc);
      }
   }

   private static void generateProperties(InstallSettings settings, Path upgrader) {
      Path propsPath = upgrader.resolve("properties.json");
      ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
      ObjectNode root;

      try {
         if (Files.exists(propsPath)) {
            try (InputStream in = Files.newInputStream(propsPath)) {
               root = (ObjectNode) mapper.readTree(in);
            }
         } else {
            root = mapper.createObjectNode();
         }

         if (!root.has("minecraft")) {
            root.put("minecraft", settings.getMinecraftPath().toString());
         }

         if (!root.has("java")) {
            root.put("java", settings.getJavaPath().toString());
         }

         root.put("version", Config.getInstance().getVersion());

         ArrayNode gamesNode = (ArrayNode) root.withArray("games");
         boolean found = false;
         for (JsonNode gameNode : gamesNode) {
            if (gameNode.has("name") && settings.getGameName().equals(gameNode.get("name").asText())) {
               ObjectNode gameObj = (ObjectNode) gameNode;
               String currentPath = gameObj.has("path") ? gameObj.get("path").asText() : null;
               if (!settings.getGameFolderPath().equals(currentPath)) {
                  log.debug("Updating existing game path: {}", settings.getGameFolderPath().toString());
                  gameObj.put("path", settings.getGameFolderPath().toString());
               }
               found = true;
               break;
            }
         }

         if (!found) {
            ObjectNode newGame = mapper.createObjectNode();
            newGame.put("name", settings.getGameName());
            newGame.put("path", settings.getGameFolderPath().toString());
            gamesNode.add(newGame);
         }

         try (OutputStream out = Files.newOutputStream(propsPath)) {
            mapper.writeValue(out, root);
         }
      } catch (IOException e) {
         throw new UncheckedIOException("Unable to create/update properties.json", e);
      }
   }


   private static void updateInstallPathLabels(InstallSettings installSettings) {
      SwingUtilities.invokeLater(() -> minecraftPathValue.setText(installSettings.getMinecraftPath().toString()));
      SwingUtilities.invokeLater(() -> javaPathValue.setText(installSettings.getJavaPath().toString()));
   }

   private static void uiLog(String message) {
      SwingUtilities.invokeLater(() -> logArea.append(LocalDateTime.now() + ": " + message + "\n"));
   }

   private static void copyRecursive(Path src, Path dest) throws IOException {
      if (src == null) throw new IOException("Source path is null");
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

   private static void cleanupTempFolder(Path tempFolder) {
      if (!Utils.isJar()) return;
      Utils.deleteDirectory(tempFolder);
   }

   private static Path extractResourceDirectory(String resourceRoot) throws IOException, URISyntaxException {
      if (Utils.isJar()) {
         URL resourceURL = InstallerMain.class.getResource(resourceRoot);
         if (resourceURL == null) throw new IOException("Resource not found: " + resourceRoot);
         // Packaged mode: extract from inside JAR
         Path tempDir = Files.createTempDirectory("installer_resources");
         URI raw = InstallerMain.class.getProtectionDomain().getCodeSource().getLocation().toURI();
         URI jarUri = new URI("jar", raw.toString(), null);
         uiLog("Using packages resource path: " + jarUri);
         try (FileSystem fs = FileSystems.newFileSystem(jarUri, new java.util.HashMap<>())) {
            Path jarPath = fs.getPath(resourceRoot);
            Files.walk(jarPath).forEach(source -> {
               try {
                  Path target = tempDir.resolve(jarPath.relativize(source).toString());
                  if (Files.isDirectory(source)) {
                     Files.createDirectories(target);
                  } else {
                     Files.createDirectories(target.getParent());
                     Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                  }
               } catch (IOException e) {
                  throw new UncheckedIOException(e);
               }
            });
         }
         return tempDir;
      } else {
         URL resourceURL = Path.of("maven/installer/target/classes").resolve(resourceRoot.substring(1)).toUri().toURL();
         log.debug("ResourceURL: {}", resourceURL);
         if (resourceURL == null) throw new IOException("Resource not found: " + resourceRoot);
         // Dev mode: just copy from filesystem path
         Path resourcePath = Paths.get(resourceURL.toURI());
         uiLog("Using raw filesystem path: " + resourcePath);
         return resourcePath;
      }
   }
}
