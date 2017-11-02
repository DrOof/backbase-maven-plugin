package com.backbase.maven.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.prefix.PluginPrefixResolver;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.net.ssl.SSLContext;
import java.io.CharConversionException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.UUID;

public abstract class BaseMojo extends AbstractMojo {

    HttpClient httpclient;

    private CookieStore httpCookieStore;

    final static ObjectMapper MAPPER = new ObjectMapper();

    @Parameter( readonly = true, defaultValue = "${project}" )
    MavenProject project;

    @Component
    protected PluginPrefixResolver pluginPrefixResolver;

    @Component
    protected PluginVersionResolver pluginVersionResolver;

    @Parameter( property = "portal.protocol", defaultValue = "http" )
    public String portalProtocol;

    @Parameter( property = "portal.host", defaultValue = "localhost" )
    public String portalHost;

    @Parameter( property = "portal.port", defaultValue = "7777" )
    public int portalPort;

    @Parameter( property = "portal.context", defaultValue = "portalserver" )
    public String portalContext;

    @Parameter( property = "portal.username", defaultValue = "admin" )
    public String username;

    @Parameter( property = "portal.password", defaultValue = "admin" )
    public String password;

    @Parameter( property = "failOnException", defaultValue = "true" )
    public String failOnException;

    @Parameter( property = "javax.net.ssl.trustStore" )
    public String trustStorePath;

    @Parameter( property = "javax.net.ssl.trustStorePassword", defaultValue = "changeit" )
    public String trustStorePassword;

    URL portalUrl;

    Header cookies;

    Header csrfToken;

    boolean parseUUID( String uuid ) {
        boolean result = true;
        try {
            UUID.fromString( uuid );
        } catch ( Exception e ) {
            result = false;
        }
        return result;
    }

    public static boolean isValidJSON( final File file ) throws IOException {
        boolean valid = true;
        try {
            MAPPER.readTree( file );
        } catch ( JsonProcessingException | CharConversionException e ) {
            valid = false;
        }
        return valid;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        httpCookieStore = new BasicCookieStore();
        HttpClientBuilder clientBuilder = HttpClientBuilder.create().setDefaultCookieStore( httpCookieStore );
        if ( trustStorePath != null ) {
            SSLContext sslContext;
            try {
                URL url = new File( trustStorePath ).toURI().toURL();
                sslContext = SSLContextBuilder
                        .create()
                        .loadTrustMaterial( url, trustStorePassword.toCharArray() )
                        .build();
            } catch ( NoSuchAlgorithmException | KeyManagementException | KeyStoreException | CertificateException | IOException e ) {
                throw new MojoExecutionException( "", e );
            }
            clientBuilder.setSSLContext( sslContext );
        }

        httpclient = clientBuilder.build();
    }

    void cleanup() {
        httpCookieStore.clear();
    }

    void login() throws IOException {
        httpCookieStore.clear();
        String encodedUserPass = ( new Base64() ).encodeAsString( ( username + ":" + password ).getBytes() );
        HttpGet httpGet = new HttpGet( portalUrl + "/groups" );
        httpGet.addHeader( "Authorization", "Basic " + encodedUserPass );
        HttpResponse httpResponse = httpclient.execute( httpGet );
        refreshCookies( httpResponse );
    }

    private void refreshCookies( HttpResponse response ) {
        cookies = response.getFirstHeader( "Set-Cookie" );
        csrfToken = response.getFirstHeader( "X-BBXSRF" );
        if ( csrfToken == null )
            csrfToken = response.getFirstHeader( "x-bbxsrf" );
    }

    void buildPortalUrl() throws MojoExecutionException {
        try {
            portalUrl = new URL( portalProtocol, portalHost, portalPort, portalContext.startsWith( "/" ) ? portalContext : "/" + portalContext );
        } catch ( MalformedURLException e ) {
            throw new MojoExecutionException( "Creating Portal Url", e );
        }
    }

    void handleResponse( HttpResponse response, HttpRequestBase request ) throws IOException, MojoFailureException {
        int statusCode = response.getStatusLine().getStatusCode();
        if ( statusCode >= 400 ) {
            String content = EntityUtils.toString( response.getEntity(), "UTF-8" );
            getLog().error( content );
            if ( failOnException.equalsIgnoreCase( "true" ) ) throw new MojoFailureException( content );
            else getLog().error( content );
        } else {
            getLog().info( "Succeeded with " + statusCode + " response code" );
            refreshCookies( response );
        }
        request.releaseConnection();
    }

}
