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

    @Parameter(property = "portal-archive-path", required = true)
    public String portalArchivePath;

    @Parameter(property = "portal-name", required = true)
    public String portalName;

    private static final String exportPath = "/orchestrator/export/exportrequests";

    private static final String portalExportParentPath = "/orchestrator/export/files/";

    private static final String exportRequestBody = "<exportRequest>" +
            "    <portalExportRequest>" +
            "        <portalName>%s</portalName>" +
            "        <includeContent>false</includeContent>" +
            "        <includeGroups>false</includeGroups>" +
            "    </portalExportRequest>" +
            "</exportRequest>";

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

        File file = new File(portalArchivePath);
        String reqBody = String.format(exportRequestBody, portalName);
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
