import urllib.request
import urllib.parse
import json

token_url = "http://localhost:8081/realms/trustai-realm/protocol/openid-connect/token"
data = urllib.parse.urlencode({
    "client_id": "trustai-client",
    "username": "admin",
    "password": "admin",
    "grant_type": "password"
}).encode("utf-8")
req = urllib.request.Request(token_url, data=data)
token_data = json.loads(urllib.request.urlopen(req).read().decode())
access_token = token_data["access_token"]

api_url = "http://localhost:8082/api/proofs/stats"
req2 = urllib.request.Request(api_url)
req2.add_header("Authorization", f"Bearer {access_token}")

try:
    response = urllib.request.urlopen(req2)
    print("Stats API Response Code:", response.getcode())
except urllib.error.HTTPError as e:
    print("Stats API Error:", e.code, e.read().decode())
