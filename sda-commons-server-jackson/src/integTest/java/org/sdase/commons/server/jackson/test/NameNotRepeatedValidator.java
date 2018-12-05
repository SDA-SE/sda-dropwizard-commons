package org.sdase.commons.server.jackson.test;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NameNotRepeatedValidator implements ConstraintValidator<CheckNameRepeated, ValidationResource> {
   @Override
   public void initialize(CheckNameRepeated constraintAnnotation) {
      // Nothing to do here
   }

   @Override
   public boolean isValid(ValidationResource value, ConstraintValidatorContext context) {
      if (value.getFirstName() == null) {
         return true;
      }
      if (value.getFirstName().equalsIgnoreCase(value.getLastName())) {
         context.disableDefaultConstraintViolation();
         context.buildConstraintViolationWithTemplate("First name and last name must be different.")
               .addPropertyNode("firstName")
               .addBeanNode()
               .addConstraintViolation();
         context.buildConstraintViolationWithTemplate("First name and last name must be different.")
               .addPropertyNode("lastName")
               .addBeanNode()
               .addConstraintViolation();
         return false;
      }
      return true;
   }
}
