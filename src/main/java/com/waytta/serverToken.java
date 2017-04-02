package com.waytta;

final public class serverToken {
    private final String token;
    private final String server;

    public serverToken(String token, String server) {
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
