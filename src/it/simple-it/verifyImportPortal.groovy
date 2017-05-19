import com.backbase.maven.plugins.ImportPortal

def importPortal = new ImportPortal()
importPortal.portalArchivePath= "/Users/jahan/Downloads/Portal-business-82f166ef-de61-47bf-a4ca-1038e67ca168.zip"
importPortal.username="admin"
importPortal.password="admin"
importPortal.portalProtocol="http"
importPortal.portalHost="localhost"
importPortal.portalPort=7777
importPortal.portalContext="portalserver"

importPortal.execute()

