package com.backbase.maven.plugins;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Goal which imports basic widgets and dashboard.
 */
@Mojo( name = "import-dashboard", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class ImportDashboard extends BaseMojo {

    private static final String importPath = "/import/";
    private static final String basicWidgetList = "&portals%5Bdashboard%5D.importIt=true&_portals%5Bdashboard%5D.importIt=on&_portals%5Bdashboard%5D.deleteIt=on&importGroupsFlag=true&_importGroupsFlag=on&importUsersFlag=true&_importUsersFlag=on&serverItems%5Bbackbase.com.2014.zenith%2Fcontent-repository.xml%5D%5BcontentRepository%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Fcontent-repository.xml%5D%5BcontentRepository%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-containers.xml%5D%5BRootContainer%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-containers.xml%5D%5BRootContainer%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-containers.xml%5D%5BRowWithSlide_container%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-containers.xml%5D%5BRowWithSlide_container%5D=on&serverItems%5Bbackbase.com.2012.aurora%2Ftemplate-widgets.xml%5D%5BStandard_Widget%5D=true&_serverItems%5Bbackbase.com.2012.aurora%2Ftemplate-widgets.xml%5D%5BStandard_Widget%5D=on&serverItems%5Bbackbase.com.2012.aurora%2Ftemplate-widgets.xml%5D%5BW3C_Widget%5D=true&_serverItems%5Bbackbase.com.2012.aurora%2Ftemplate-widgets.xml%5D%5BW3C_Widget%5D=on&serverItems%5Bbackbase.com.2013.aurora%2Fcatalog-containers-adminDesignerOnly.xml%5D%5BManageable_Area_Closure%5D=true&_serverItems%5Bbackbase.com.2013.aurora%2Fcatalog-containers-adminDesignerOnly.xml%5D%5BManageable_Area_Closure%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-widgets.xml%5D%5BAppTitle_widget%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-widgets.xml%5D%5BAppTitle_widget%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-widgets.xml%5D%5BSideNav_widget%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-widgets.xml%5D%5BSideNav_widget%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-widgets.xml%5D%5BRootWidget%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-widgets.xml%5D%5BRootWidget%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-widgets.xml%5D%5BPortalNavigation_widget%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-widgets.xml%5D%5BPortalNavigation_widget%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-widgets.xml%5D%5BAjaxButton_widget%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-widgets.xml%5D%5BAjaxButton_widget%5D=on&serverItems%5Bbackbase.com.2012.aurora%2Ftemplate-containers.xml%5D%5BColumn_table%5D=true&_serverItems%5Bbackbase.com.2012.aurora%2Ftemplate-containers.xml%5D%5BColumn_table%5D=on&serverItems%5Bbackbase.com.2013.aurora%2Ftemplate-pages.xml%5D%5BBlankPageTemplate%5D=true&_serverItems%5Bbackbase.com.2013.aurora%2Ftemplate-pages.xml%5D%5BBlankPageTemplate%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-pages.xml%5D%5Bcxp-manager-page%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-pages.xml%5D%5Bcxp-manager-page%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-pages.xml%5D%5BRootPage%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Fenterprise-catalog-pages.xml%5D%5BRootPage%5D=on&serverItems%5B..%2FdefaultImportData%2Ftemplates.xml%5D%5Bportal-default%5D=true&_serverItems%5B..%2FdefaultImportData%2Ftemplates.xml%5D%5Bportal-default%5D=on&serverItems%5B..%2FdefaultImportData%2Ftemplates.xml%5D%5Bpage-default%5D=true&_serverItems%5B..%2FdefaultImportData%2Ftemplates.xml%5D%5Bpage-default%5D=on&serverItems%5B..%2FdefaultImportData%2Ftemplates.xml%5D%5Bwidget-default%5D=true&_serverItems%5B..%2FdefaultImportData%2Ftemplates.xml%5D%5Bwidget-default%5D=on&serverItems%5B..%2FdefaultImportData%2Ftemplates.xml%5D%5Bcontainer-default%5D=true&_serverItems%5B..%2FdefaultImportData%2Ftemplates.xml%5D%5Bcontainer-default%5D=on&serverItems%5B..%2FdefaultImportData%2Ftemplates.xml%5D%5Blink-default%5D=true&_serverItems%5B..%2FdefaultImportData%2Ftemplates.xml%5D%5Blink-default%5D=on&serverItems%5Bbackbase.com.2012.darts%2Fcatalog-containers.xml%5D%5BTargetingContainer%5D=true&_serverItems%5Bbackbase.com.2012.darts%2Fcatalog-containers.xml%5D%5BTargetingContainer%5D=on&serverItems%5Bbackbase.com.2012.darts%2Fcatalog-containers.xml%5D%5BTargetingContainerChild%5D=true&_serverItems%5Bbackbase.com.2012.darts%2Fcatalog-containers.xml%5D%5BTargetingContainerChild%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-pages.xml%5D%5BBB_Dashboard%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-pages.xml%5D%5BBB_Dashboard%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-pages.xml%5D%5BBB_Dashboard_Migration%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-pages.xml%5D%5BBB_Dashboard_Migration%5D=on&serverItems%5Bbackbase.com.2012.darts%2Ftemplate-containers.xml%5D%5BTCont%5D=true&_serverItems%5Bbackbase.com.2012.darts%2Ftemplate-containers.xml%5D%5BTCont%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-containers.xml%5D%5BRowWithSlide%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-containers.xml%5D%5BRowWithSlide%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-containers.xml%5D%5BSimpleTabBox%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-containers.xml%5D%5BSimpleTabBox%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-containers.xml%5D%5BResponsiveGrid%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-containers.xml%5D%5BResponsiveGrid%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-containers.xml%5D%5BStaticLeftFlexRight%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-containers.xml%5D%5BStaticLeftFlexRight%5D=on&serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-containers.xml%5D%5BResizeableTwoColumn%5D=true&_serverItems%5Bbackbase.com.2014.zenith%2Ftemplate-containers.xml%5D%5BResizeableTwoColumn%5D=on&serverItems%5Bbackbase.com.2013.aurora%2Ftemplate-containers.xml%5D%5BManageableArea%5D=true&_serverItems%5Bbackbase.com.2013.aurora%2Ftemplate-containers.xml%5D%5BManageableArea%5D=on";

    ImportDashboard() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        super();
    }

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        buildPortalUrl();
        try {
            login();
            uploadWidgets();
            cleanup();
        } catch ( IOException e ) {
            throw new MojoExecutionException( "Error in Portal Rest Call " + e );
        }
    }

    private void uploadWidgets() throws IOException, MojoExecutionException {
        String importUrl = portalUrl + importPath;

        HttpPost httpPost = new HttpPost( importUrl );
        httpPost.setEntity( new StringEntity( "BBXSRF=" + csrfToken.getValue() + basicWidgetList, ContentType.APPLICATION_FORM_URLENCODED ) );
        httpPost.addHeader( "Cookie", cookies.getValue() );
        httpPost.addHeader( "X-BBXSRF", csrfToken.getValue() );
        HttpResponse response = httpclient.execute( httpPost );
        handleResponse( response, httpPost );
    }
}