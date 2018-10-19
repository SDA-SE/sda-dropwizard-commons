package com.sdase.commons.server.testing;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * To be used with {@link EnvironmentRule}
 */
final class Environment {

   private Environment() {
      //
   }

   static void setEnv(String key, String value) {
      try {
         Map<String, String> writableEnv = getWriteableEnv();
         writableEnv.put(key, value);
      } catch (Exception e) {
         throw new IllegalStateException("Failed to set environment variable", e);
      }
   }

   static void unsetEnv(String key) {
      try {
         Map<String, String> writableEnv = getWriteableEnv();
         writableEnv.remove(key);
      } catch (Exception e) {
         throw new IllegalStateException("Failed to unset environment variable", e);
      }
   }

   private static Map<String, String> getWriteableEnv() throws IllegalAccessException, NoSuchFieldException {
      Map<String, String> env = System.getenv();
      Class<?> cl = env.getClass();
      Field field = cl.getDeclaredField("m");
      field.setAccessible(true);
      //noinspection unchecked
      return (Map<String, String>) field.get(env);
   }
}
