import com.backbase.maven.plugins.BBImport

def bImport = new BBImport()
bImport.portalArchivePath= "/Users/jahan/Documents/Portal-test-13f72a2f-be51-4b82-a03c-736ab0063601.zip"
bImport.username="admin"
bImport.password="admin"
bImport.portalUrl="http://localhost:7777"
bImport.execute()
