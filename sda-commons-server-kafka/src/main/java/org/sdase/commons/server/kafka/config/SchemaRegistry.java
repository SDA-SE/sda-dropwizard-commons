package org.sdase.commons.server.kafka.config;

public class SchemaRegistry {

    private String server;

    private Integer port;

    @Override
    public String toString() {
            return server.concat(":").concat(port.toString());
        }

    public String getServer() {
            return server;
        }

    public void setServer(String server) {
            this.server = server;
        }

    public Integer getPort() {
            return port;
        }

    public void setPort(Integer port) {
            this.port = port;
        }

    public String getUrl() {
        StringBuilder buf = new StringBuilder("http://").append(server);
        if (port != null) {
            buf.append(':').append(port);
        }
        return buf.toString();
    }
}
