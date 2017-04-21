package com.backbase.maven.plugins;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.IOUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

@Mojo(name = "export-portal", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class ExportPortal extends BaseMojo {

    /**
     * target folder where the file be exported
     */
    @Parameter(property = "target", required = true)
    public String target;

    /**
     * portal name
     */
    @Parameter(property = "portal", required = true)
    public String portal;

    private static final String exportPath = "/orchestrator/export/exportrequests";

    private static final String portalExportParentPath = "/orchestrator/export/files/";

    private static final String exportRequestBody = "<exportRequest>" +
            "    <portalExportRequest>" +
            "        <portalName>%s</portalName>" +
            "        <includeContent>%s</includeContent>" +
            "        <includeGroups>%s</includeGroups>" +
            "    </portalExportRequest>" +
            "</exportRequest>";

    /**
     * include contents stuff in exported zip file
     */
    @Parameter(property = "includeContents", required = false, defaultValue = "true")
    public String includeContents;

    /**
     * include group information in exported zip file
     */
    @Parameter(property = "includeGroups", required = false, defaultValue = "true")
    public String includeGroups;

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {

        buildPortalUrl();
        try {
            login();
            export();
            cleanup();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new MojoExecutionException("" + e);
        }
    }

    private void export() throws IOException, ParserConfigurationException, SAXException {
        String exportUrl = portalUrl + exportPath;

        File file = new File(target);
        String reqBody = String.format(exportRequestBody, portal, includeContents, includeGroups);
        HttpPost httpPost = new HttpPost(exportUrl);
        httpPost.setHeader("Cookie", cookies.getValue());
        httpPost.setHeader("X-BBXSRF", csrfToken.getValue());
        httpPost.setEntity(new StringEntity(reqBody, ContentType.TEXT_XML));

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        HttpResponse response = httpclient.execute(httpPost);
        StringReader stringReader = new StringReader(EntityUtils.toString(response.getEntity()));
        InputSource inputSource = new InputSource(stringReader);
        Document doc = dBuilder.parse(inputSource);
        String identifier = doc.getElementsByTagName("identifier").item(0).getTextContent();
        String downloadUrl = portalUrl + portalExportParentPath + identifier;

        HttpGet httpGet = new HttpGet(downloadUrl);
        httpGet.addHeader("Cookie", cookies.getValue());
        httpGet.addHeader("X-BBXSRF", csrfToken.getValue());
        HttpResponse httpResponse = httpclient.execute(httpGet);
        InputStream inputStream = httpResponse.getEntity().getContent();
        OutputStream outputStream = new FileOutputStream(file);
        IOUtil.copy(inputStream, outputStream);
        outputStream.close();
        httpGet.releaseConnection();
    }
}
