package com.backbase.maven.plugins;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import sun.misc.BASE64Encoder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Goal which imports portal model.
 */
@Mojo(name = "import-portal", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class ImportPortal extends AbstractMojo {

    @Parameter(property = "portal-archive-path", required = true)
    public String portalArchivePath;

    @Parameter(property = "portal-protocol", defaultValue = "http")
    public String portalProtocol;

    @Parameter(property = "portal-host", defaultValue = "localhost")
    public String portalHost;

    @Parameter(property = "portal-port", defaultValue = "7777")
    public int portalPort;

    @Parameter(property = "portal-context", defaultValue = "portalserver")
    public String portalContext;

    @Parameter(property = "portal-username", defaultValue = "admin")
    public String username;

    @Parameter(property = "portal-password", defaultValue = "admin")
    public String password;

    public URL portalUrl;

    public void execute() throws MojoExecutionException, MojoFailureException {
        buildPortalUrl();
        File file = new File(portalArchivePath);
        if (!file.exists()) {
            throw new MojoFailureException(portalArchivePath + " does not exists!");
        }
        try {
            String encodedUserPass = (new BASE64Encoder()).encode((username + ":" + password).getBytes());
            String finalUrl = embedUserPassInURL(portalUrl) + "/groups";
            HttpResponse getResponse = Request.Get(finalUrl)
                    .addHeader("Authorization", "Basic " + encodedUserPass)
                    .execute().returnResponse();

            Header cookies = getResponse.getFirstHeader("Set-Cookie");
            Header csrfToken = getResponse.getFirstHeader("X-BBXSRF");
            if (csrfToken == null)
                csrfToken = getResponse.getFirstHeader("x-bbxsrf");
            String uploadPath = "/orchestrator/import/upload";
            String serverUri = portalUrl + uploadPath;

            HttpClient httpclient = HttpClientBuilder.create().build();
            HttpPost httppost = new HttpPost(serverUri);
            MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
            reqEntity
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addBinaryBody("file", file);
            httppost.setEntity(reqEntity.build());
            httppost.setHeader("Cookie", cookies.getValue());
            httppost.setHeader("X-BBXSRF", csrfToken.getValue());
            httppost.getRequestLine();
            HttpResponse response = httpclient.execute(httppost);
            response.getStatusLine();
            HttpEntity resEntity = response.getEntity();

        } catch (IOException e) {
            getLog().error(e);
            throw new MojoFailureException("Error in Portal Rest Call " + e);
        }
    }

    private void buildPortalUrl() {
        try {
            portalUrl = new URL(portalProtocol, portalHost, portalPort, portalContext.startsWith("/") ? portalContext : "/" + portalContext);
        } catch (MalformedURLException e) {
            getLog().error("Creating Portal Url", e);
        }
    }

    private String embedUserPassInURL(URL url) {
        return url.toString().replaceFirst("://", "://" + username + ":" + password + "@");
    }
}
