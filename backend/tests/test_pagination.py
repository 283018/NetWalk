from uuid import uuid4

def test_paginated_endpoint(client):
    test_uuid = str(uuid4())
    for i in range(5):
        payload = {
            "measurements": [{
                "session_id": test_uuid,
                "android_id": "test",
                "cid": i,
                "measured_at": f"2025-01-20T12:{i:02d}:00Z",
                "latitude": 52.0,
                "longitude": 21.0,
                "rsrp": -70 - i
            }]
        }
        client.post("/measurements/batch", json=payload)

    # skip=0, limit=2
    resp = client.get("/measurements/paginated?skip=0&limit=2")
    assert resp.status_code == 200
    data = resp.json()
    assert "items" in data
    assert "total" in data
    assert data["skip"] == 0
    assert data["limit"] == 2
    assert len(data["items"]) == 2
    total = data["total"]
    assert total >= 5

    # skip=2, limit=2
    resp2 = client.get(f"/measurements/paginated?skip=2&limit=2")
    assert resp2.status_code == 200
    data2 = resp2.json()
    assert data2["skip"] == 2
    assert len(data2["items"]) == 2
    # Sprawdź, że to różne elementy (opcjonalnie porównaj id)
    assert data2["items"][0]["id"] != data["items"][0]["id"]