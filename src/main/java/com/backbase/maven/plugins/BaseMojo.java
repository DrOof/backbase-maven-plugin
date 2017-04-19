package com.backbase.maven.plugins;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class BaseMojo extends AbstractMojo {

    static final HttpClient httpclient = HttpClientBuilder.create().build();

    @Parameter(property = "portal.protocol", defaultValue = "http")
    public String portalProtocol;

    @Parameter(property = "portal.host", defaultValue = "localhost")
    public String portalHost;

    @Parameter(property = "portal.port", defaultValue = "7777")
    public int portalPort;

    @Parameter(property = "portal.context", defaultValue = "portalserver")
    public String portalContext;

    @Parameter(property = "portal.username", defaultValue = "admin")
    public String username;

    @Parameter(property = "portal.password", defaultValue = "admin")
    public String password;

    URL portalUrl;

    Header cookies;

    Header csrfToken;

    public void execute() throws MojoExecutionException, MojoFailureException {
    }

    void login() throws IOException {
        String encodedUserPass = (new BASE64Encoder()).encode((username + ":" + password).getBytes());
        HttpResponse getResponse = Request.Get(portalUrl + "/groups")
                .addHeader("Authorization", "Basic " + encodedUserPass)
                .execute()
                .returnResponse();

        cookies = getResponse.getFirstHeader("Set-Cookie");
        csrfToken = getResponse.getFirstHeader("X-BBXSRF");
        if (csrfToken == null)
            csrfToken = getResponse.getFirstHeader("x-bbxsrf");
    }

    void buildPortalUrl() throws MojoExecutionException {
        try {
            portalUrl = new URL(portalProtocol, portalHost, portalPort, portalContext.startsWith("/") ? portalContext : "/" + portalContext);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Creating Portal Url", e);
        }
    }
}
