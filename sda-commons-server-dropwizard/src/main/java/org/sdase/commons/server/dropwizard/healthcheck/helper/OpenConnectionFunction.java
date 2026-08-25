package org.sdase.commons.server.dropwizard.healthcheck.helper;

import java.io.IOException;
import java.net.HttpURLConnection;

public interface OpenConnectionFunction {
  HttpURLConnection call(String url) throws IOException;
}
