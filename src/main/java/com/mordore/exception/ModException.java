package com.mordore.exception;

import com.mordore.pojo.ModVersion;

public class ModException extends RuntimeException {
   private final ModVersion version;

   public ModException(String message, ModVersion version) {
      super(message);
      this.version = version;
   }

   public ModVersion getVersion() {
      return version;
   }
}
