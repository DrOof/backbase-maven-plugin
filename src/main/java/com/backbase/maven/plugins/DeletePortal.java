package com.backbase.maven.plugins;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;

/**
 * Goal which imports portal model.
 */
@Mojo( name = "delete-portal", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class DeletePortal extends BaseMojo {

    @Parameter( property = "portalName", required = true )
    public String portalName;

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        super.execute();
        try {
            buildPortalUrl();
            login();
            delete();
        } catch ( IOException | InterruptedException e ) {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    private void delete() throws IOException, InterruptedException, MojoFailureException {
        String importUrl = portalUrl + "/portals/" + portalName + "/deletePortal";
        HttpDelete httpDelete = new HttpDelete( importUrl );
        httpDelete.setHeader( "Cookie", cookies.getValue() );
        httpDelete.setHeader( "X-BBXSRF", csrfToken.getValue() );
        HttpResponse httpResponse = httpclient.execute( httpDelete );
        handleResponse( httpResponse, httpDelete );
    }
}
