from uuid import uuid4

def test_new_carrier_fields_in_response(client):
    test_uuid = str(uuid4())
    payload = {
        "measurements": [{
            "session_id": test_uuid,
            "android_id": "test_device",
            "cid": 123,
            "measured_at": "2025-01-20T12:00:00Z",
            "latitude": 52.0,
            "longitude": 21.0,
            "test_carrier_mode": "CA_2A_12A",
            "cells_involved": "2,12",
            "primary_cell_id": "2"
        }]
    }

    post_resp = client.post("/measurements/batch", json=payload)
    assert post_resp.status_code == 200

    get_resp = client.get("/measurements?limit=1")
    assert get_resp.status_code == 200
    data = get_resp.json()
    assert len(data) > 0
    item = data[0]
    assert "test_carrier_mode" in item
    assert item["test_carrier_mode"] == "CA_2A_12A"
    assert "cells_involved" in item
    assert item["cells_involved"] == "2,12"
    assert "primary_cell_id" in item
    assert item["primary_cell_id"] == "2"