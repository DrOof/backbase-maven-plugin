package com.backbase.maven.plugins;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.html.HtmlEscapers;
import com.sun.scenario.effect.impl.sw.java.JSWBlend_SRC_OUTPeer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Entities;
import org.w3c.dom.Document;
import org.jsoup.nodes.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.zeroturnaround.zip.ZipUtil;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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

    public String getPortalSrcZip() {
        return portalSrc + ".zip";
    }

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
            transform();
            cleanup();
        } catch ( Exception e ) {
            throw new MojoExecutionException( "" + e );
        }
    }

    private void transform() throws IOException {
        List< Path > search = Find.Search( "*", portalSrc + "/" + artifactId, 1 );

        Path contentPath = null;
        for ( Path path : search ) {
            if ( parseUUID( path.getFileName().toString() ) ) {
                contentPath = Paths.get( path.toString(), "content" );
                break;
            }
        }
        File[] contentFiles = contentPath.toFile().listFiles();
        for ( File content : contentFiles ) {
            if ( isValidJSON( content ) ) {
                MAPPER.setNodeFactory( new JsonNodeFactory() {
                    @Override
                    public TextNode textNode( String text ) {
                        try {
                            TextNode jsonNodes = super.textNode( transformJson( text ) );
                            return jsonNodes;
                        } catch ( IOException e ) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                } );
                JsonNode node = MAPPER.readTree( content );

                Files.write( content.toPath(), node.toString().getBytes() );
            }
        }
    }

    private void unzip() throws MojoFailureException, IOException, ParserConfigurationException, SAXException, TransformerException {

        ZipUtil.unpack( Paths.get( getPortalSrcZip() ).toFile(), Paths.get( portalSrc ).toFile() );
        Path metadataXmlPath = Paths.get( portalSrc, "metadata.xml" );

        Files.delete( Paths.get( getPortalSrcZip() ) );
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

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse( metadataXmlPath.toFile() );
        doc.getElementsByTagName( "packageId" ).item( 0 ).setTextContent( artifactId );

        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty( OutputKeys.INDENT, "no" );
        tf.setOutputProperty( OutputKeys.METHOD, "xml" );
        DOMSource domSource = new DOMSource( doc );
        StreamResult sr = new StreamResult( metadataXmlPath.toFile() );
        tf.transform( domSource, sr );
        getLog().info( "Unzip the downloaded portal finished" );
    }

    private String transformJson( String jsonText ) throws IOException {
        org.jsoup.nodes.Document doc = Jsoup.parseBodyFragment( jsonText );
        doc.outputSettings().prettyPrint( false );
        doc.outputSettings().escapeMode( Entities.EscapeMode.base );
        for ( Element e : doc.body().children() ) {
            if ( e.hasText() ) {
                String encodedText = StringEscapeUtils.escapeHtml4( StringEscapeUtils.unescapeHtml4( e.text() ) );
                e.text( encodedText );
            }
        }

        String result = StringEscapeUtils.unescapeHtml4( doc.body().toString().replace( "<body>", "" ).replace( "</body>", "" ) );
        return result;
    }

    private void export() throws IOException, ParserConfigurationException, SAXException {
        String exportUrl = portalUrl + exportPath;
        FileUtils.forceDelete( new File( portalSrc ) );
        File file = new File( getPortalSrcZip() );
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
