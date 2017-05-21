package com.backbase.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.codehaus.plexus.util.IOUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.zeroturnaround.zip.ZipUtil;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Mojo( name = "export-portal", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true )
public class ExportPortal extends BaseMojo {

    /**
     * target folder where the file be exported
     */
    @Parameter( property = "portal.src", required = true )
    public String portalSrc;

    /**
     * portal name
     */
    @Parameter( property = "portalName", required = true )
    public String portalName;

    @Parameter( property = "artifactId", required = true )
    public String artifactId;

    private static final String exportPath = "/orchestrator/export/exportrequests";

    private static final String portalExportParentPath = "/orchestrator/export/files/";

    private static final String exportRequestBody =
            "<exportRequest>" +
                    "    <portalExportRequest>" +
                    "        <portalName>%s</portalName>" +
                    "        <includeContent>%s</includeContent>" +
                    "        <includeGroups>%s</includeGroups>" +
                    "    </portalExportRequest>" +
                    "</exportRequest>";

    /**
     * include contents stuff in exported zip file
     */
    @Parameter( property = "includeContents", defaultValue = "true" )
    public String includeContents;

    /**
     * include group information in exported zip file
     */
    @Parameter( property = "includeGroups", defaultValue = "true" )
    public String includeGroups;

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {

        buildPortalUrl();
        try {
            login();
            export();
            unzip();
            cleanup();
        } catch ( IOException | ParserConfigurationException | SAXException e ) {
            throw new MojoExecutionException( "" + e );
        }
    }

    private void unzip() throws MojoFailureException, IOException {
        ZipUtil.unpack( Paths.get( portalSrc + ".zip" ).toFile(), Paths.get( portalSrc ).toFile() );
        Files.delete( Paths.get( portalSrc + ".zip" ) );
        List< Path > portalExports = Find.Search( "Portal*", portalSrc, 1 );

        Collections.sort( portalExports, new Comparator< Path >() {
            public int compare( Path o1, Path o2 ) {
                int cmp = -2;
                try {
                    cmp = -1 * Files.readAttributes( o1, BasicFileAttributes.class ).creationTime().compareTo( Files.readAttributes( o2, BasicFileAttributes.class ).creationTime() );
                } catch ( IOException e ) {
                    getLog().error( "Can't read the creation time of file", e );
                }
                return cmp;
            }
        } );

        Files.move( portalExports.get( 0 ), Paths.get( Paths.get( portalSrc ).toString(), artifactId ), StandardCopyOption.REPLACE_EXISTING );

        List< Path > zipFiles = Find.Search( "*.zip", portalSrc );
        for ( Path zipFile : zipFiles ) {
            ZipUtil.unpack( zipFile.toFile(), Paths.get( zipFile.toString().replace( ".zip", "" ) ).toFile() );
            Files.delete( zipFile );
        }
        getLog().info( "Unzip the downloaded portal finished" );
    }

    private void export() throws IOException, ParserConfigurationException, SAXException {
        String exportUrl = portalUrl + exportPath;
        FileUtils.forceDelete( new File( portalSrc ) );
        File file = new File( portalSrc + ".zip" );
        String reqBody = String.format( exportRequestBody, portalName, includeContents, includeGroups );
        HttpPost httpPost = new HttpPost( exportUrl );
        httpPost.setHeader( "Cookie", cookies.getValue() );
        httpPost.setHeader( "X-BBXSRF", csrfToken.getValue() );
        httpPost.setEntity( new StringEntity( reqBody, ContentType.TEXT_XML ) );

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        HttpResponse response = httpclient.execute( httpPost );
        String respString = EntityUtils.toString( response.getEntity() );
        StringReader stringReader = new StringReader( respString );
        InputSource inputSource = new InputSource( stringReader );
        Document doc = dBuilder.parse( inputSource );
        String identifier = doc.getElementsByTagName( "identifier" ).item( 0 ).getTextContent();
        String downloadUrl = portalUrl + portalExportParentPath + identifier;

        HttpGet httpGet = new HttpGet( downloadUrl );
        httpGet.addHeader( "Cookie", cookies.getValue() );
        httpGet.addHeader( "X-BBXSRF", csrfToken.getValue() );
        HttpResponse httpResponse = httpclient.execute( httpGet );
        InputStream inputStream = httpResponse.getEntity().getContent();
        OutputStream outputStream = new FileOutputStream( file );
        IOUtil.copy( inputStream, outputStream );
        outputStream.close();
        httpGet.releaseConnection();
        getLog().info( "Download exported portal finished" );
    }
}
