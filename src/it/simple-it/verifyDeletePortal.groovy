import com.backbase.maven.plugins.DeletePortal

def deletePortal = new DeletePortal()
deletePortal.username = "admin"
deletePortal.password = "admin"
deletePortal.portalProtocol = "http"
deletePortal.portalHost = "localhost"
deletePortal.portalPort = 7777
deletePortal.portalName = 'business'
deletePortal.portalContext = "portalserver"
//deletePortal.trustStorePath = "/tmp/java_cacerts"
//deletePortal.trustStorePassword = "changeit"

deletePortal.execute()

