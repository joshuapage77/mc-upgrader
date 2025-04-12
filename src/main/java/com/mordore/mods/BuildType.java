package com.mordore.mods;

public enum BuildType {
   Release("release"),
   Snapshot("snapshot");

   private final String value;

   BuildType(String value) {
      this.value = value.toLowerCase();
   }

   public String getValue() {
      return value;
   }

   public static BuildType fromValue(String val) {
      for (BuildType c : values()) {
         if (c.value.equals(val)) return c;
      }
      return BuildType.Snapshot;
   }

   public boolean equals(String other) {
      return value.equals(other);
   }

}
