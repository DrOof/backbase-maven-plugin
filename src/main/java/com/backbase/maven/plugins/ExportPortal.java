package com.backbase.maven.plugins;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

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
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new MojoExecutionException("" + e);
        }
    }

    private void export() throws IOException, ParserConfigurationException, SAXException {
        String exportUrl = portalUrl + exportPath;

        File file = new File(target);
        String reqBody = String.format(exportRequestBody, portal, includeContents, includeGroups);
        String xmlResp = Request.Post(exportUrl)
                .bodyString(reqBody, ContentType.TEXT_XML)
                .addHeader("Cookie", cookies.getValue())
                .addHeader("X-BBXSRF", csrfToken.getValue())
                .execute()
                .returnContent()
                .asString();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(xmlResp));
        Document doc = dBuilder.parse(inputSource);
        String identifier = doc.getElementsByTagName("identifier").item(0).getTextContent();
        String downloadUrl = portalUrl + portalExportParentPath + identifier;
        Request.Get(downloadUrl)
                .addHeader("Cookie", cookies.getValue())
                .addHeader("X-BBXSRF", csrfToken.getValue())
                .execute()
                .saveContent(file);
    }
}
