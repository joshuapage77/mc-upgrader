package com.mordore.mods;

import com.mordore.ModCompatibilityResolver;
import com.mordore.pojo.ModVersion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnginehubArtifactProvider extends ModArtifactProvider {
   private static final Map<String, String> modMapper;
   static {
      modMapper = new HashMap<>();
      // maps mod slugs used on cursed forge and modrinth to enginehub snapshot project names
      modMapper.put("worldedit-cui", "worldeditcui");
   }
   private static final String BASE_URL = "https://builds.enginehub.org";
   private static final Logger log = LoggerFactory.getLogger(ModCompatibilityResolver.class);

   public EnginehubArtifactProvider(String loader) {
      super(loader);
      if (!"fabric".equals(loader)) throw new RuntimeException("Only Fabric is currently supported");
   }

   @Override
   public Map<String, ModVersion> getVersions(String slug, List<String> mcVersions) {
      return getVersions(slug, mcVersions, false);
   }

   @Override
   public Map<String, ModVersion> getVersions(String slug, List<String> mcVersions, boolean allowSnapshots) {
      String ehSlug = modMapper.get(slug);
      Map<String, ModVersion> latestPerMcVersion = new HashMap<>();
      //Engine hub (I think) is only snapshot builds. I can't tell anyway from the page if something is not a snapshot
      if (!allowSnapshots) return latestPerMcVersion;
      for (String mcVersion : mcVersions) {
         try {
            String branch = "mc/" + mcVersion;
            String jobUrl = BASE_URL + "/job/" + ehSlug + "?branch=" + branch;
            Document doc = Jsoup.connect(jobUrl).get();
            Elements links = doc.select("a.Link_MainLink__WSPVL[href^=/job/" + ehSlug + "/]");
            if (links.isEmpty()) {
               log.debug("Did not find build links for minecraft branch {}", mcVersion);
               continue;
            }
            String relBuildUrl = links.first().attr("href");
            String fullBuildUrl = BASE_URL + relBuildUrl;
            Document buildDoc = Jsoup.connect(fullBuildUrl).get();
            String jarUrl = extractJarUrl(buildDoc);
            if (jarUrl == null) {
               log.debug("Unable to extract jar url from build page document");
               continue;
            }
            String versionNumber = extractVersionNumber(jarUrl);
            ModVersion modVersion = new ModVersion(versionNumber, jarUrl, List.of(mcVersion), BuildType.Snapshot);
            latestPerMcVersion.put(mcVersion, modVersion);
         } catch (Exception e) {
            log.error("Error scraping engine hub: {}", e.getMessage());
         }
      }
      return latestPerMcVersion;
   }

   private static String extractJarUrl(Document doc) {
      ///System.out.println(doc.html());
      Elements links = doc.select("a[href*=.jar]");
      for (Element link : links) {
         String url = link.absUrl("href");
         if (!url.isEmpty()) return url;
      }
      return null;
   }

   private String extractVersionNumber(String url) {
      String marker = "WorldEditCUI-";
      int start = url.lastIndexOf(marker);
      if (start == -1) return "unknown";
      start += marker.length();

      int end = url.indexOf(".jar", start);
      if (end == -1 || start >= end) return "unknown";

      return url.substring(start, end);
   }
}