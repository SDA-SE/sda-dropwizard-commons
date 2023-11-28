package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.configuration.UndefinedEnvironmentVariableException;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.TextStringBuilder;
import org.apache.commons.text.lookup.StringLookup;

/**
 * A {@link StringSubstitutor} that picks properties from {@link System#getProperty(String)}
 * (preferred) and {@link System#getenv(String)} (secondary) that may be modified by operators.
 * Substitution variables are identified according the defaults of {@link StringSubstitutor}.
 *
 * @see SystemPropertyAndEnvironmentLookup Details about lookup and modification by operators
 */
public class SystemPropertyAndEnvironmentSubstitutor extends StringSubstitutor {

  public SystemPropertyAndEnvironmentSubstitutor() {
    this(true, false);
  }

  public SystemPropertyAndEnvironmentSubstitutor(boolean strict) {
    this(strict, false);
  }

  /**
   * @param strict {@code true} if looking up undefined environment variables should throw a {@link
   *     UndefinedEnvironmentVariableException}, {@code false} otherwise.
   * @param substitutionInVariables a flag whether substitution is done in variable names.
   * @see org.apache.commons.text.StringSubstitutor#setEnableSubstitutionInVariables(boolean)
   */
  public SystemPropertyAndEnvironmentSubstitutor(boolean strict, boolean substitutionInVariables) {
    this(strict, substitutionInVariables, new SystemPropertyAndEnvironmentLookup());
  }

  /**
   * @param strict {@code true} if looking up undefined environment variables should throw a {@link
   *     UndefinedEnvironmentVariableException}, {@code false} otherwise.
   * @param substitutionInVariables a flag whether substitution is done in variable names.
   * @param stringLookup the lookup that turns keys into values
   * @see org.apache.commons.text.StringSubstitutor#setEnableSubstitutionInVariables(boolean)
   */
  public SystemPropertyAndEnvironmentSubstitutor(
      boolean strict, boolean substitutionInVariables, StringLookup stringLookup) {
    super(stringLookup);
    this.setEnableUndefinedVariableException(strict);
    this.setEnableSubstitutionInVariables(substitutionInVariables);
  }

  @Override
  protected boolean substitute(TextStringBuilder buf, int offset, int length) {
    try {
      return super.substitute(buf, offset, length);
    } catch (IllegalArgumentException e) {
      if (e.getMessage() != null && e.getMessage().contains("Cannot resolve variable")) {
        throw new UndefinedEnvironmentVariableException(e.getMessage());
      }
      throw e;
    }
  }
}
