from uuid import uuid4
def test_ul_dl_stats(client):
    test_uuid = str(uuid4())
    payload = {
        "measurements": [{
            "session_id": test_uuid,
            "android_id": "ul_dl_test",
            "cid": 1,
            "measured_at": "2025-01-20T12:00:00Z",
            "latitude": 52.0,
            "longitude": 21.0,
            "dl_throughput_mbps": 120.5,
            "dl_latency_ms": 22.0,
            "dl_jitter_ms": 5.2,
            "dl_lost_packets": 0,
            "ul_throughput_mbps": 25.3,
            "ul_latency_ms": 35.0,
            "ul_jitter_ms": 8.1,
            "ul_lost_packets": 2
        }]
    }
    client.post("/measurements/batch", json=payload)

    resp = client.get("/analysis/ul-dl-stats")
    assert resp.status_code == 200
    data = resp.json()
    assert "downlink" in data
    assert "uplink" in data
    assert data["downlink"]["throughput"] == 120.5
    assert data["downlink"]["latency"] == 22.0
    assert data["uplink"]["lost_packets"] == 2

    resp2 = client.get(f"/analysis/ul-dl-stats?session_id={test_uuid}")
    assert resp2.status_code == 200
    data2 = resp2.json()
    assert data2["downlink"]["throughput"] == 120.5