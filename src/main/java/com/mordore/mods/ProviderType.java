package com.mordore.mods;

public enum ProviderType {
   Modrinth("modrinth"),
   EngineHub("enginehub"),
   Invalid("invalid");

   private final String value;

   ProviderType(String value) {
      this.value = value.toLowerCase();
   }

   public String getValue() {
      return value;
   }

   public static ProviderType fromValue(String val) {
      for (ProviderType c : values()) {
         if (c.value.equals(val)) return c;
      }
      return ProviderType.Invalid;
   }

   public boolean equals(String other) {
      return value.equals(other);
   }

}
