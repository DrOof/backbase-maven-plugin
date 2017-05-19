import com.backbase.maven.plugins.ExportPortal

def exportPortal = new ExportPortal()
exportPortal.target= "/Users/jahan/projects/westpac/bb-fe/collection-wnzl-portal/src/main/resources/collection-wnzl-portal"
exportPortal.username="admin"
exportPortal.password="admin"
exportPortal.portalProtocol="http"
exportPortal.portalHost="localhost"
exportPortal.portalPort=7777
exportPortal.portalContext="portalserver"
exportPortal.portal="business"
exportPortal.includeContents="true"
exportPortal.includeGroups="true"
exportPortal.artifact="portal-business-1.0-SNAPSHOT"
exportPortal.execute()

