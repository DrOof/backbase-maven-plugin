import com.backbase.maven.plugins.ImportDashboard

def importDashboard = new ImportDashboard()
importDashboard.username = "admin"
importDashboard.password = "admin"
importDashboard.portalProtocol = "http"
importDashboard.portalHost = "localhost"
importDashboard.portalPort = 7777
importDashboard.portalContext = "portalserver"
//importDashboard.trustStorePath = "/tmp/java_cacerts"
//importDashboard.trustStorePassword = "changeit"

importDashboard.execute()

