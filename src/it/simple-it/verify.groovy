import com.backbase.maven.plugins.ImportPortal

def importPortal = new ImportPortal()
importPortal.portalArchivePath= "/Users/jahan/Documents/Portal-test-13f72a2f-be51-4b82-a03c-736ab0063601.zip"
importPortal.username="admin"
importPortal.password="admin"
importPortal.portalProtocol="http"
importPortal.portalHost="localhost"
importPortal.portalPort=7777
importPortal.portalContext="portalserver"
importPortal.username="admin"
importPortal.password="admin"
//importPortal.portalUrl=new URL("http://localhost:7777")
importPortal.execute()
