package javax.validation.constraints;

/**
 * @deprecated DO NOT USE THIS ANNOTATION! This is included here to make sure that the Email
 *     annotation from javax.validation:validation-api is available, as the JSON schema generator
 *     requires it. The problem is that Email is only available in a newer version of
 *     javax.validation:validation-api which we can't use due to incompatibilities with hibernate.
 */
// TODO: Remove this class once we upgraded to Dropwizard 2
@Deprecated
public class Email {}
