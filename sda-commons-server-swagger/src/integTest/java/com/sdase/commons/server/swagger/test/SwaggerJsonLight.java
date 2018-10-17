package com.sdase.commons.server.swagger.test;

import static com.google.common.base.MoreObjects.toStringHelper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.models.Info;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class SwaggerJsonLight {

   private final Info info;
   private final String basePath;
   private final Map<String, SwaggerPath> paths;
   private final Map<String, SwaggerDefinition> definitions;

   public SwaggerJsonLight(
         @JsonProperty("info") Info info,
         @JsonProperty("basePath") String basePath,
         @JsonProperty("paths") Map<String, SwaggerPath> paths,
         @JsonProperty("definitions") Map<String, SwaggerDefinition> definitions) {

      this.info = info;
      this.basePath = basePath;
      this.paths = paths;
      this.definitions = definitions;
   }

   public Info getInfo() {
      return info;
   }

   public String getBasePath() {
      return basePath;
   }

   public Map<String, SwaggerPath> getPaths() {
      return paths;
   }

   public Map<String, SwaggerDefinition> getDefinitions() {
      return definitions;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      SwaggerJsonLight that = (SwaggerJsonLight) o;
      return Objects.equals(info, that.info) &&
            Objects.equals(basePath, that.basePath) &&
            Objects.equals(paths, that.paths) &&
            Objects.equals(definitions, that.definitions);
   }

   @Override
   public int hashCode() {
      return Objects.hash(info, basePath, paths, definitions);
   }

   @Override
   public String toString() {
      return toStringHelper(this)
            .add("info", info)
            .add("basePath", basePath)
            .add("paths", paths)
            .add("definitions", definitions)
            .toString();
   }

   @JsonIgnoreProperties(ignoreUnknown = true)
   public static class SwaggerPath {

      private final SwaggerOperation get;
      private final SwaggerOperation post;
      private final SwaggerOperation delete;

      @JsonCreator
      public SwaggerPath(
            @JsonProperty("get") SwaggerOperation get,
            @JsonProperty("post") SwaggerOperation post,
            @JsonProperty("delete") SwaggerOperation delete) {

         this.get = get;
         this.post = post;
         this.delete = delete;
      }

      public SwaggerOperation getGet() {
         return get;
      }

      public SwaggerOperation getPost() {
         return post;
      }

      public SwaggerOperation getDelete() {
         return delete;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         }
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         SwaggerPath that = (SwaggerPath) o;
         return Objects.equals(get, that.get) &&
               Objects.equals(post, that.post) &&
               Objects.equals(delete, that.delete);
      }

      @Override
      public int hashCode() {
         return Objects.hash(get, post, delete);
      }

      @Override
      public String toString() {
         return toStringHelper(this)
               .add("get", get)
               .add("post", post)
               .add("delete", delete)
               .toString();
      }
   }

   @JsonIgnoreProperties(ignoreUnknown = true)
   public static class SwaggerOperation {

      private final String summary;

      @JsonCreator
      public SwaggerOperation(@JsonProperty("summary") String summary) {
         this.summary = summary;
      }

      public String getSummary() {
         return summary;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         }
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         return Objects.equals(summary, ((SwaggerOperation) o).summary);
      }

      @Override
      public int hashCode() {
         return Objects.hashCode(summary);
      }

      @Override
      public String toString() {
         return toStringHelper(this).add("summary", summary).toString();
      }
   }

   @JsonIgnoreProperties(ignoreUnknown = true)
   public static class SwaggerDefinition {

      private final Map<String, Object> properties;

      @JsonCreator
      public SwaggerDefinition(
            @JsonProperty("properties") Map<String, Object> properties) {

         this.properties = properties;
      }

      public Map<String, Object> getProperties() {
         return properties;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         }
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         return Objects.equals(properties, ((SwaggerDefinition) o).properties);
      }

      @Override
      public int hashCode() {
         return Objects.hashCode(properties);
      }

      @Override
      public String toString() {
         return toStringHelper(this).add("properties", properties).toString();
      }
   }
}
