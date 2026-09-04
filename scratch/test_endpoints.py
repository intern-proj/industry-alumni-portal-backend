import urllib.request
import json

def test_endpoint(url):
    print(f"Testing {url}")
    try:
        req = urllib.request.Request(url)
        with urllib.request.urlopen(req) as response:
            data = json.loads(response.read().decode())
            print(f"Success! Keys: {data.keys() if isinstance(data, dict) else 'List of length ' + str(len(data))}")
            if isinstance(data, dict):
                for k, v in data.items():
                    print(f"  {k}: {type(v)} (len {len(v) if isinstance(v, (list, str, dict)) else v})")
                    if k == 'data' and isinstance(v, dict):
                         print(f"    data keys: {v.keys()}")
    except Exception as e:
        print(f"Error: {e}")

test_endpoint('http://localhost:8080/api/v1/vacancies/public')
test_endpoint('http://localhost:8080/api/v1/events')
test_endpoint('http://localhost:8080/api/v1/partners/public')
test_endpoint('http://localhost:8080/api/v1/user-profiles/partners')
