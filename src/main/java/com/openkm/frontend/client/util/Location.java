package com.openkm.frontend.client.util;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.http.client.URL;

/**
 * @author jllort
 *
 */
public class Location {
    private String hash;

    private String host;

    private String hostName;

    private String href;

    private String path;

    private String port;

    private String protocol;

    private String queryString;

    private HashMap<String, String> paramMap;

    public String getHash() {
        return hash;
    }

    public String getHost() {
        return host;
    }

    public String getHostName() {
        return hostName;
    }

    public String getHref() {
        return href;
    }

    public String getPath() {
        return path;
    }

    public String getContext() {
        String context = path.substring(path.indexOf("/") + 1);
        context = context.substring(0, context.indexOf("/"));
        return "/" + context;
    }

    public String getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getQueryString() {
        return queryString;
    }

    protected void setHash(final String hash) {
        this.hash = hash;
    }

    protected void setHost(final String host) {
        this.host = host;
    }

    protected void setHostName(final String hostName) {
        this.hostName = hostName;
    }

    protected void setHref(final String href) {
        this.href = href;
    }

    protected void setPath(final String path) {
        this.path = path;
    }

    protected void setPort(final String port) {
        this.port = port;
    }

    protected void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    protected void setQueryString(final String queryString) {
        this.queryString = queryString;
        paramMap = new HashMap<String, String>();

        if (queryString != null && queryString.length() > 1) {
            final String qs = queryString.substring(1);
            final String[] kvPairs = qs.split("&");
            for (final String kvPair : kvPairs) {
                final String[] kv = kvPair.split("=");
                if (kv.length > 1) {
                    paramMap.put(kv[0], URL.decodeQueryString(kv[1]));
                } else {
                    paramMap.put(kv[0], "");
                }
            }
        }
    }

    public String getParameter(final String name) {
        return paramMap.get(name);
    }

    public Map<String, String> getParameterMap() {
        return paramMap;
    }
}
