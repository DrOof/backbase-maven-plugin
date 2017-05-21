import com.backbase.maven.plugins.ImportPortal

def importPortal = new ImportPortal()
importPortal.portalSrc= "/Users/jahan/projects/westpac/bb-fe/collection-wnzl-portal/src/main/resources/collection-wnzl-portal"
importPortal.username="admin"
importPortal.password="admin"
importPortal.portalProtocol="http"
importPortal.portalHost="localhost"
importPortal.portalPort=7777
importPortal.portalContext="portalserver"
importPortal.outputDir="/Users/jahan/projects/westpac/bb-fe/collection-wnzl-portal/target"
importPortal.artifactId="portal-business-1.0-SNAPSHOT"

importPortal.execute()

