import com.backbase.maven.plugins.ExportPortal

def exportPortal = new ExportPortal()
exportPortal.portalArchivePath= "/Users/jahan/Documents/Portal-test-13f72a2f-be51-4b82-a03c-736ab0063601-exported.zip"
exportPortal.username="admin"
exportPortal.password="admin"
exportPortal.portalProtocol="http"
exportPortal.portalHost="localhost"
exportPortal.portalPort=7777
exportPortal.portalContext="portalserver"
exportPortal.portalName="test"
exportPortal.execute()

