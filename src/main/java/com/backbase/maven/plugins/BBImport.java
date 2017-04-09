package com.backbase.maven.plugins;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.*;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import sun.misc.BASE64Encoder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;

/**
 * Goal which imports portal model.
 */
@Mojo(name = "bb-import", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class BBImport
        extends AbstractMojo {
    /**
     * portalArchivePath
     */
    @Parameter(property = "bbimport.portal.archive.path", required = true)
    public String portalArchivePath;

    @Parameter(property = "bbimport.portal.username", defaultValue = "admin")
    public String username;

    @Parameter(property = "bbimport.portal.password", defaultValue = "admin")
    public String password;

    /**
     * portalUrl
     */
    @Parameter(property = "bb-import.portal.url", defaultValue = "http://localhost:7777")
    public String portalUrl;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File file = new File(portalArchivePath);
        if (!file.exists()) {
            throw new MojoFailureException(portalArchivePath + " does not exists!");
        }
        try {
            String encodedUserPass = (new BASE64Encoder()).encode((username + ":" + password).getBytes());
            String finalUrl = embedUserPassInURL(portalUrl) + "/portalserver/groups";
            HttpResponse getResponse = Request.Get(finalUrl)
                    .addHeader("Authorization", "Basic " + encodedUserPass)
                    .execute().returnResponse();

            Header cookies = getResponse.getFirstHeader("Set-Cookie");
            Header csrfToken = getResponse.getFirstHeader("X-BBXSRF");
            if (csrfToken == null)
                csrfToken = getResponse.getFirstHeader("x-bbxsrf");
            String uploadPath = "/portalserver/orchestrator/import/upload";
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

    private String embedUserPassInURL(String url) {
        return url.replace("http://", "http://" + username + ":" + password + "@");
    }
}
