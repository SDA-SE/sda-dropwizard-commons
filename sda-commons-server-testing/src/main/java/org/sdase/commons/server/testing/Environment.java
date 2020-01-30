package org.sdase.commons.server.testing;

import java.lang.reflect.Field;
import java.util.Map;

/** To be used with {@link EnvironmentRule} */
public final class Environment {

  private Environment() {
    //
  }

  public static void setEnv(String key, String value) {
    try {
      Map<String, String> writableEnv = getWriteableEnv();
      writableEnv.put(key, value);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to set environment variable", e);
    }
  }

  public static void unsetEnv(String key) {
    try {
      Map<String, String> writableEnv = getWriteableEnv();
      writableEnv.remove(key);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to unset environment variable", e);
    }
  }

  private static Map<String, String> getWriteableEnv()
      throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException {

    try {
      // based on solution in
      // https://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
      // windows environment
      Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
      Field theCaseInsensitiveEnvironmentField =
          processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
      theCaseInsensitiveEnvironmentField.setAccessible(true);
      return (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
    } catch (NoSuchFieldException e) {
      // other environments
      Map<String, String> env = System.getenv();
      Class<?> cl = env.getClass();
      Field field = cl.getDeclaredField("m");
      field.setAccessible(true);
      //noinspection unchecked
      return (Map<String, String>) field.get(env);
    }
  }
}
