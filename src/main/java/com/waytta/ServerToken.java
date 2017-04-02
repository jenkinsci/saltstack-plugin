package com.waytta;

final public class ServerToken {
    private final String token;
    private final String server;

    public ServerToken(String token, String server) {
        this.token = token;
        this.server = server;
    }

    public String getToken() {
        return token;
    }

    public String getServer() {
        return server;
    }

}
