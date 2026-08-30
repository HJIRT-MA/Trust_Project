import urllib.request
import urllib.parse
import json

# 1. Get Token from Keycloak
token_url = "http://localhost:8081/realms/trustai-realm/protocol/openid-connect/token"
data = urllib.parse.urlencode({
    "client_id": "trustai-client",
    "username": "admin",
    "password": "admin",
    "grant_type": "password"
}).encode("utf-8")

req = urllib.request.Request(token_url, data=data)
try:
    with urllib.request.urlopen(req) as response:
        token_data = json.loads(response.read().decode())
        access_token = token_data["access_token"]
        print("Got token!")
except Exception as e:
    print("Failed to get token:", e)
    exit(1)

# 2. Call /api/proofs
api_url = "http://localhost:8082/api/proofs"
req2 = urllib.request.Request(api_url)
req2.add_header("Authorization", f"Bearer {access_token}")

try:
    with urllib.request.urlopen(req2) as response:
        print("API Response Code:", response.getcode())
        print("API Response Body:", response.read().decode())
except urllib.error.HTTPError as e:
    print("API HTTP Error:", e.code, e.read().decode())
except Exception as e:
    print("API Error:", e)
