from uuid import uuid4

def test_cpu_filter_endpoint(client):
    test_uuid = str(uuid4())
    cpus = [20, 40, 60, 80]
    for cpu in cpus:
        payload = {
            "measurements": [{
                "session_id": test_uuid,
                "android_id": "test_cpu",
                "cid": 1,
                "measured_at": "2025-01-20T12:00:00Z",
                "latitude": 52.0,
                "longitude": 21.0,
                "host_cpu": cpu
            }]
        }
        client.post("/measurements/batch", json=payload)

    # all
    resp = client.get("/measurements/analysis-cpu-filter?cpu_filter=all&cpu_threshold=50")
    assert resp.status_code == 200
    data = resp.json()
    assert data["total"] >= 4

    # only_high (>=50)
    resp = client.get("/measurements/analysis-cpu-filter?cpu_filter=only_high&cpu_threshold=50")
    data = resp.json()
    assert data["total"] >= 2  # 60 i 80
    assert all(item["host_cpu"] >= 50 for item in data["items"])

    # without_high (<50)
    resp = client.get("/measurements/analysis-cpu-filter?cpu_filter=without_high&cpu_threshold=50")
    data = resp.json()
    assert data["total"] >= 2  # 20 i 40
    assert all(item["host_cpu"] < 50 for item in data["items"])

def test_kpi_by_cpu(client):

    test_uuid = str(uuid4())
    payload_low = {
        "measurements": [{
            "session_id": test_uuid,
            "android_id": "kpi_test",
            "cid": 1,
            "measured_at": "2025-01-20T12:00:00Z",
            "latitude": 52.0,
            "longitude": 21.0,
            "host_cpu": 30,
            "throughput_mbps": 100,
            "latency_ms": 20,
            "rsrp": -70
        }]
    }
    payload_high = {
        "measurements": [{
            "session_id": test_uuid,
            "android_id": "kpi_test",
            "cid": 1,
            "measured_at": "2025-01-20T12:05:00Z",
            "latitude": 52.0,
            "longitude": 21.0,
            "host_cpu": 70,
            "throughput_mbps": 30,
            "latency_ms": 80,
            "rsrp": -95
        }]
    }
    client.post("/measurements/batch", json=payload_low)
    client.post("/measurements/batch", json=payload_high)

    # without_high
    resp = client.get("/analysis/kpi-by-cpu?cpu_filter=without_high&cpu_threshold=50")
    assert resp.status_code == 200
    data = resp.json()
    assert data["cpu_filter"] == "without_high"
    assert data["cpu_threshold"] == 50
    # średnie powinny pochodzić tylko z niskiego CPU
    assert data["avg_throughput"] == 100
    assert data["avg_latency"] == 20
    assert data["avg_rsrp"] == -70

    # only_high
    resp = client.get("/analysis/kpi-by-cpu?cpu_filter=only_high&cpu_threshold=50")
    data = resp.json()
    assert data["avg_throughput"] == 30
    assert data["avg_latency"] == 80
    assert data["avg_rsrp"] == -95

def test_cpu_threshold_endpoint(client):
    test_uuid = str(uuid4())
    cpus = [10, 20, 30, 40, 50, 60, 70, 80, 90]
    for cpu in cpus:
        payload = {
            "measurements": [{
                "session_id": test_uuid,
                "android_id": "thresh_test",
                "cid": 1,
                "measured_at": "2025-01-20T12:00:00Z",
                "latitude": 52.0,
                "longitude": 21.0,
                "host_cpu": cpu
            }]
        }
        client.post("/measurements/batch", json=payload)

    resp = client.get("/analysis/cpu-threshold")
    assert resp.status_code == 200
    data = resp.json()
    assert "threshold" in data
    assert "categories" in data
    assert "normal" in data["categories"]
    assert "high" in data["categories"]

    threshold = data["threshold"]
    normal = data["categories"]["normal"]
    high = data["categories"]["high"]
    assert normal + high == len(cpus)

    assert normal == sum(1 for c in cpus if c < threshold)