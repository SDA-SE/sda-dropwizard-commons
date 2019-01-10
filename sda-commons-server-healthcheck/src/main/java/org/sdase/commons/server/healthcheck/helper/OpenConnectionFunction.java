package org.sdase.commons.server.healthcheck.helper;

import java.io.IOException;
import java.net.HttpURLConnection;

public interface OpenConnectionFunction {
   HttpURLConnection call(String url) throws IOException;
}
