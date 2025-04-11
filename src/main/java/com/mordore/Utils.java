package com.mordore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
}
