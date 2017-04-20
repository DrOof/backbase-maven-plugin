package com.backbase.maven.plugins;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;

/**
 * Goal which imports portal model.
 */
@Mojo(name = "import-portal", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class ImportPortal extends BaseMojo {

    @Parameter(property = "portal-archive-path", required = true)
    public String portalArchivePath;

    private static final String importPath = "/orchestrator/import/upload";

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        File file = new File(portalArchivePath);
        if (!file.exists())
            throw new MojoExecutionException(portalArchivePath + " does not exists!");
        buildPortalUrl();
        try {
            login();
            upload(file);
        } catch (IOException e) {
            throw new MojoExecutionException("Error in Portal Rest Call " + e);
        }
    }

    private void upload(File file) throws IOException, MojoExecutionException {
        String importUrl = portalUrl + importPath;
        HttpPost httpPost = new HttpPost(importUrl);
        HttpEntity reqEntity = MultipartEntityBuilder
                .create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addBinaryBody("file", file)
                .build();
        httpPost.setEntity(reqEntity);

        httpPost.setHeader("Cookie", cookies.getValue());
        httpPost.setHeader("X-BBXSRF", csrfToken.getValue());
        HttpResponse httpResponse = httpclient.execute(httpPost);
        handleResponse(httpResponse);
    }
}
