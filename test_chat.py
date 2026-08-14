import urllib.request
import urllib.parse
import json

# Get token
url = "http://localhost:8081/realms/trustai-realm/protocol/openid-connect/token"
data = urllib.parse.urlencode({"username": "admin", "password": "admin", "client_id": "trustai-client", "grant_type": "password"}).encode("utf-8")
req = urllib.request.Request(url, data=data)
with urllib.request.urlopen(req) as response:
    token_res = json.loads(response.read().decode())
    token = token_res["access_token"]

# Call /chat with conversationId=5
chat_url = "http://localhost:8082/api/rag/chat"
chat_data = json.dumps({"query": "donne plus dinformation", "topK": 3, "conversationId": 5}).encode("utf-8")
chat_req = urllib.request.Request(chat_url, data=chat_data, headers={"Content-Type": "application/json", "Authorization": "Bearer " + token})
try:
    with urllib.request.urlopen(chat_req) as res:
        print("Success:")
        print(res.read().decode())
except urllib.error.HTTPError as e:
    print("HTTP Error:", e.code)
    print(e.read().decode())
