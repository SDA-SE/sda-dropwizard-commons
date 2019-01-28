package org.sdase.commons.server.morphia;

import org.hibernate.validator.constraints.NotEmpty;

public class MongoConfiguration  {

   /**
    * <p>
    *    Comma separated list of hosts with their port that build the MongoDB cluster:
    * </p>
    * <pre>{@code mongo-db-a:27018,mongo-db-b:27018,mongo-db-c:27018}</pre>
    * <p>
    *    The default port if no port is specified is {@code :27017}:
    * </p>
    * <p>
    *    {@code mongo-db-a,mongo-db-b,mongo-db-c} is equal to {@code mongo-db-a:27017,mongo-db-b:27017,mongo-db-c:27017}
    * </p>
    * <p>
    *    Details in the
    *    <a href="https://docs.mongodb.com/manual/reference/connection-string/">connection string documentation</a> for
    *    host1 to hostN.
    * </p>
    */
   @NotEmpty
   private String hosts;

   /**
    * <p>
    *    The name of the mongo database to access.
    * </p>
    * <p>
    *    Details in the
    *    <a href="https://docs.mongodb.com/manual/reference/connection-string/">connection string documentation</a> for
    *    database.
    * </p>
    */
   @NotEmpty
   private String database;

   /**
    * <p>
    *    Additional options for the connection.
    * </p>
    * <p>
    *    Details in the
    *    <a href="https://docs.mongodb.com/manual/reference/connection-string/">connection string documentation</a> for
    *    options.
    * </p>
    */
   private String options = "";

   /**
    * <p>
    *    The username used for login at the MongoDB.
    * </p>
    * <p>
    *    Details in the
    *    <a href="https://docs.mongodb.com/manual/reference/connection-string/">connection string documentation</a> for
    *    username:password.
    * </p>
    */
   private String username;

   /**
    * <p>
    *    The password used for login at the MongoDB.
    * </p>
    * <p>
    *    Details in the
    *    <a href="https://docs.mongodb.com/manual/reference/connection-string/">connection string documentation</a> for
    *    username:password.
    * </p>
    */
   private String password;

   /**
    * If SSL should be used for the database connection.
    */
   private boolean useSsl;

   /**
    * The content of a CA certificate (list) in PEM format. This certificates are added to the
    * {@link javax.net.ssl.TrustManager}s to verify the connection. The string represents the content of a regular PEM
    * file, e.g.:
    * <pre><code>
    * -----BEGIN CERTIFICATE-----
    * MIIEkjCCA3qgAwIBAgIQCgFBQgAAAVOFc2oLheynCDANBgkqhkiG9w0BAQsFADA/
    * MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT
    * ...
    * X4Po1QYz+3dszkDqMp4fklxBwXRsW10KXzPMTZ+sOPAveyxindmjkW8lGy+QsRlG
    * PfZ+G6Z6h7mjem0Y+iWlkYcV4PIWL1iwBi8saCbGS5jN2p8M+X+Q7UNKEkROb3N6
    * KOqkqm57TH2H3eDJAkSnh6/DNFu0Qg==
    * -----END CERTIFICATE-----
    * </code></pre>
    */
   private String caCertificate;

   /**
    * The Base 64 encoded content of a CA certificate (list) in PEM format. This certificates are added to the
    * {@link javax.net.ssl.TrustManager}s to verify the connection. The string represents the Base 64 encoded content
    * of a regular PEM file, e.g. {@code LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVrakNDQTNxZ0F3SUJBZ0lRQ2dGQlFnQUFBVk9GYzJvTGhleW5DREFOQmdrcWhraUc5dzBCQVFzRkFEQS8KTVNRd0lnWURWUVFLRXh0RWFXZHBkR0ZzSUZOcFoyNWhkSFZ5WlNCVWNuVnpkQ0JEYnk0eEZ6QVZCZ05WQkFNVApEa1JUVkNCU2IyOTBJRU5CSUZnek1CNFhEVEUyTURNeE56RTJOREEwTmxvWERUSXhNRE14TnpFMk5EQTBObG93ClNqRUxNQWtHQTFVRUJoTUNWVk14RmpBVUJnTlZCQW9URFV4bGRDZHpJRVZ1WTNKNWNIUXhJekFoQmdOVkJBTVQKR2t4bGRDZHpJRVZ1WTNKNWNIUWdRWFYwYUc5eWFYUjVJRmd6TUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQwpBUThBTUlJQkNnS0NBUUVBbk5NTThGcmxMa2UzY2wwM2c3Tm9ZekRxMXpVbUdTWGh2YjQxOFhDU0w3ZTRTMEVGCnE2bWVOUWhZN0xFcXhHaUhDNlBqZGVUbTg2ZGljYnA1Z1dBZjE1R2FuL1BRZUdkeHlHa09sWkhQL3VhWjZXQTgKU014K3lrMTNFaVNkUnh0YTY3bnNIamNBSEp5c2U2Y0Y2czVLNjcxQjVUYVl1Y3Y5YlR5V2FOOGpLa0tRRElaMApaOGgvcFpxNFVtRVVFejlsNllLSHk5djZEbGIyaG9uemhUK1hocSt3M0JydmF3MlZGbjNFSzZCbHNwa0VObldBCmE2eEs4eHVRU1hndm9wWlBLaUFsS1FUR2RNRFFNYzJQTVRpVkZycW9NN2hEOGJFZnd6Qi9vbmt4RXowdE52amoKL1BJemFyazVNY1d2eEkwTkhXUVdNNnI2aENtMjFBdkEySDNEa3dJREFRQUJvNElCZlRDQ0FYa3dFZ1lEVlIwVApBUUgvQkFnd0JnRUIvd0lCQURBT0JnTlZIUThCQWY4RUJBTUNBWVl3ZndZSUt3WUJCUVVIQVFFRWN6QnhNRElHCkNDc0dBUVVGQnpBQmhpWm9kSFJ3T2k4dmFYTnlaeTUwY25WemRHbGtMbTlqYzNBdWFXUmxiblJ5ZFhOMExtTnYKYlRBN0JnZ3JCZ0VGQlFjd0FvWXZhSFIwY0RvdkwyRndjSE11YVdSbGJuUnlkWE4wTG1OdmJTOXliMjkwY3k5awpjM1J5YjI5MFkyRjRNeTV3TjJNd0h3WURWUjBqQkJnd0ZvQVV4S2V4cEhzc2NmcmI0VXVRZGYvRUZXQ0ZpUkF3ClZBWURWUjBnQkUwd1N6QUlCZ1puZ1F3QkFnRXdQd1lMS3dZQkJBR0MzeE1CQVFFd01EQXVCZ2dyQmdFRkJRY0MKQVJZaWFIUjBjRG92TDJOd2N5NXliMjkwTFhneExteGxkSE5sYm1OeWVYQjBMbTl5WnpBOEJnTlZIUjhFTlRBegpNREdnTDZBdGhpdG9kSFJ3T2k4dlkzSnNMbWxrWlc1MGNuVnpkQzVqYjIwdlJGTlVVazlQVkVOQldETkRVa3d1ClkzSnNNQjBHQTFVZERnUVdCQlNvU21wakJIM2R1dWJST2JlbVJXWHY4Nmpzb1RBTkJna3Foa2lHOXcwQkFRc0YKQUFPQ0FRRUEzVFBYRWZOaldEamRHQlg3Q1ZXK2RsYTVjRWlsYVVjbmU4SWtDSkx4V2g5S0VpazNKSFJSSEdKbwp1TTJWY0dmbDk2UzhUaWhSelp2b3JvZWQ2dGk2V3FFQm10enczV29kYXRnK1Z5T2VwaDRFWXByLzF3WEt0eDgvCndBcEl2SlN3dG1WaTRNRlU1YU1xclNERTZlYTczTWoydGNNeW81ak1kNmptZVdVSEs4c28vam9XVW9IT1Vnd3UKWDRQbzFRWXorM2RzemtEcU1wNGZrbHhCd1hSc1cxMEtYelBNVForc09QQXZleXhpbmRtamtXOGxHeStRc1JsRwpQZlorRzZaNmg3bWplbTBZK2lXbGtZY1Y0UElXTDFpd0JpOHNhQ2JHUzVqTjJwOE0rWCtRN1VOS0VrUk9iM042CktPcWtxbTU3VEgySDNlREpBa1NuaDYvRE5GdTBRZz09Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0=}
    * which represents the full example of {@link #caCertificate}.
    */
   private String caCertificateBase64;


   public String getHosts() {
      return hosts;
   }

   public MongoConfiguration setHosts(String hosts) {
      this.hosts = hosts;
      return this;
   }

   public String getDatabase() {
      return database;
   }

   public MongoConfiguration setDatabase(String database) {
      this.database = database;
      return this;
   }

   public String getOptions() {
      return options;
   }

   public MongoConfiguration setOptions(String options) {
      this.options = options;
      return this;
   }

   public String getUsername() {
      return username;
   }

   public MongoConfiguration setUsername(String username) {
      this.username = username;
      return this;
   }

   public String getPassword() {
      return password;
   }

   public MongoConfiguration setPassword(String password) {
      this.password = password;
      return this;
   }

   public boolean isUseSsl() {
      return useSsl;
   }

   public MongoConfiguration setUseSsl(boolean useSsl) {
      this.useSsl = useSsl;
      return this;
   }

   public String getCaCertificate() {
      return caCertificate;
   }

   public MongoConfiguration setCaCertificate(String caCertificate) {
      this.caCertificate = caCertificate;
      return this;
   }

   public String getCaCertificateBase64() {
      return caCertificateBase64;
   }

   public MongoConfiguration setCaCertificateBase64(String caCertificateBase64) {
      this.caCertificateBase64 = caCertificateBase64;
      return this;
   }
}
