package org.sdase.commons.client.jersey.filter;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_ENCODING;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LENGTH;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public final class CompressedResponseHeaderFilter implements ClientResponseFilter {

  private static final String CONTENT_MD5 = "Content-MD5";

  private static final Set<String> COMPRESSED_ENCODINGS = Set.of("br", "deflate", "gzip", "x-gzip");

  private static final Set<String> ENTITY_TRANSPORT_HEADERS =
      Set.of(CONTENT_ENCODING, CONTENT_LENGTH, CONTENT_MD5);

  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
    String contentEncoding = responseContext.getHeaderString(CONTENT_ENCODING);
    if (contentEncoding == null || !isCompressed(contentEncoding)) {
      return;
    }

    responseContext
        .getHeaders()
        .keySet()
        .removeIf(
            header ->
                ENTITY_TRANSPORT_HEADERS.stream()
                    .anyMatch(entityHeader -> entityHeader.equalsIgnoreCase(header)));
  }

  private boolean isCompressed(String contentEncoding) {
    return Arrays.stream(contentEncoding.split(","))
        .map(encoding -> encoding.trim().toLowerCase(Locale.ROOT))
        .anyMatch(COMPRESSED_ENCODINGS::contains);
  }
}
