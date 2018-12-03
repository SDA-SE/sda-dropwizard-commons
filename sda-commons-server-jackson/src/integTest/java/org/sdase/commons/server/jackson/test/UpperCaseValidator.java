package org.sdase.commons.server.jackson.test;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Locale;

public class UpperCaseValidator implements ConstraintValidator<UpperCase, String> {

   @Override
   public void initialize(UpperCase constraintAnnotation) {

   }

   @Override
   public boolean isValid(String value, ConstraintValidatorContext context) {
      if (value == null) {
         return true;
      }
      return value.equals(value.toUpperCase(Locale.ROOT));
   }
}
