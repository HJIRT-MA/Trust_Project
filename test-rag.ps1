$Username = "admin"
$Password = "admin"
$KeycloakUrl = "http://localhost:8081/realms/trustai-realm/protocol/openid-connect/token"
$RagUploadUrl = "http://localhost:8082/api/rag/documents"

Write-Host "1. Getting token from Keycloak..."
$body = @{
    client_id = "trustai-frontend"
    grant_type = "password"
    username = $Username
    password = $Password
}
$response = Invoke-RestMethod -Method Post -Uri $KeycloakUrl -Body $body -ContentType "application/x-www-form-urlencoded"
$token = $response.access_token

Write-Host "2. Creating a test document..."
$dummyText = "Le projet TrustAI Chain est une application basée sur l'intelligence artificielle pour la recherche documentaire sécurisée. Il utilise une architecture multi-tenant et des bases de données vectorielles."
Set-Content -Path "test_document.txt" -Value $dummyText

Write-Host "3. Uploading document to RAG Service..."
$headers = @{
    "Authorization" = "Bearer $token"
}
$filePath = Resolve-Path "test_document.txt"

# Create a multipart form data request
curl.exe -X POST $RagUploadUrl -H "Authorization: Bearer $token" -F "file=@$filePath"

Write-Host "`nDone! You can now search for 'projet TrustAI' in your browser!"
