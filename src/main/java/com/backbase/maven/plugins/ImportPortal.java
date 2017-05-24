package com.backbase.maven.plugins;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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
import java.util.List;
import java.util.UUID;

/**
 * Goal which imports portal model.
 */
@Mojo( name = "import-portal", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class ImportPortal extends BaseMojo {

    @Parameter( property = "portal.src", required = true )
    public String portalSrc;

    private static final String importPath = "/orchestrator/import/upload";

    @Parameter( property = "artifactId", required = true )
    public String artifactId;

    public String outputDir;
    private String parentSrc;

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        outputDir = outputDir == null ? project.getModel().getBuild().getDirectory() : outputDir;
        parentSrc = Paths.get( portalSrc ).getFileName().toString();
        File result;

        try {
            result = buildZipFile().toFile();
        } catch ( IOException e ) {
            throw new MojoFailureException( e, "Building zip failed", e.getMessage() );
        }
        if ( !result.exists() )
            throw new MojoExecutionException( outputDir + ".zip" + " does not exists!" );
        buildPortalUrl();
        try {
            login();
            upload( result );
            cleanup();
        } catch ( IOException | InterruptedException e ) {
            throw new MojoExecutionException( "Error in Portal Rest Call " + e );
        }
    }

    private Path buildZipFile() throws IOException {
        List< Path > search = Find.Search( "*", portalSrc + "/" + artifactId, 1 );

        String innerTmpDir = outputDir + "/" + parentSrc + "/" + artifactId;
        String tmpDir = outputDir + "/" + parentSrc;

        for ( Path path : search ) {
            if ( parseUUID( path.getFileName().toString() ) ) {
                Path uuidPath = Paths.get( innerTmpDir, path.getFileName() + ".zip" );
                Files.createDirectories( uuidPath.getParent() );
                ZipUtil.pack( path.toFile(), uuidPath.toFile() );
                break;
            }
        }
        File contentServices = Paths.get( portalSrc, artifactId, "contentservices" ).toFile();
        if ( contentServices.exists() )
            ZipUtil.pack( contentServices, Paths.get( innerTmpDir, "contentservices.zip" ).toFile() );
        Files.copy( Paths.get( portalSrc, artifactId, "portalserver.xml" ), Paths.get( innerTmpDir, "portalserver.xml" ), StandardCopyOption.REPLACE_EXISTING );
        Files.copy( Paths.get( portalSrc, "metadata.xml" ), Paths.get( tmpDir, "metadata.xml" ), StandardCopyOption.REPLACE_EXISTING );
        Path result = Paths.get( outputDir, parentSrc + ".zip" );
        ZipUtil.pack( Paths.get( tmpDir ).toFile(), result.toFile() );
        return result;
    }

    private void upload( File file ) throws IOException, MojoExecutionException, InterruptedException {
        String importUrl = portalUrl + importPath;
        HttpPost httpPost = new HttpPost( importUrl );
        HttpEntity reqEntity = MultipartEntityBuilder
                .create()
                .setMode( HttpMultipartMode.BROWSER_COMPATIBLE )
                .addBinaryBody( "file", file )
                .build();
        httpPost.setEntity( reqEntity );

        httpPost.setHeader( "Cookie", cookies.getValue() );
        httpPost.setHeader( "X-BBXSRF", csrfToken.getValue() );
        HttpResponse httpResponse = httpclient.execute( httpPost );
        handleResponse( httpResponse, httpPost );
    }
}
