from uuid import uuid4


class TestMeasurementsAPI:
    def test_get_measurements_empty(self, client):
        response = client.get("/measurements")
        assert response.status_code == 200
        assert isinstance(response.json(), list)

    def test_create_batch_measurements(self, client):
        test_uuid = str(uuid4())
        test_data = {
            "measurements": [
                {
                    "session_id": test_uuid,
                    "imsi": "310150123456789",
                    "measured_at": "2026-04-27T10:00:00Z",
                    "latitude": 52.2297,
                    "longitude": 21.0122,
                    "rsrp": -75,
                    "network_type": "5G",
                    "sinr": 15,
                    "rsrq": -10,
                    "cell_id": "12345-678",
                },
                {
                    "session_id": test_uuid,
                    "imsi": "310150123456789",
                    "measured_at": "2026-04-27T10:05:00Z",
                    "latitude": 52.2300,
                    "longitude": 21.0130,
                    "rsrp": -82,
                    "network_type": "5G",
                },
            ]
        }

        response = client.post("/measurements/batch", json=test_data)
        assert response.status_code == 200
        assert "inserted" in response.json()
        assert response.json()["inserted"] == 2

    def test_get_measurements_after_insert(self, client):
        test_uuid = str(uuid4())
        test_data = {
            "measurements": [
                {
                    "session_id": test_uuid,
                    "imsi": "310150123456789",
                    "measured_at": "2026-04-27T10:00:00Z",
                    "latitude": 52.2297,
                    "longitude": 21.0122,
                    "rsrp": -75,
                }
            ]
        }
        client.post("/measurements/batch", json=test_data)

        response = client.get("/measurements")
        assert response.status_code == 200
        data = response.json()
        assert len(data) > 0

        first_item = data[0]
        assert "id" in first_item
        assert "rsrp" in first_item
        assert "network_type" in first_item


class TestAnalyticsAPI:
    def test_average_signal(self, client):
        response = client.get("/analysis/average-signal")
        assert response.status_code == 200
        data = response.json()
        assert "avg_rsrp" in data
        assert "avg_sinr" in data


class TestBatchValidation:
    def test_empty_batch_rejected(self, client):
        test_data = {"measurements": []}
        response = client.post("/measurements/batch", json=test_data)
        assert response.status_code == 400

    def test_missing_required_fields(self, client):
        test_data = {"measurements": [{"rsrp": -75}]}
        response = client.post("/measurements/batch", json=test_data)
        assert response.status_code == 422

    def test_invalid_latitude(self, client):
        test_data = {
            "measurements": [
                {
                    "session_id": str(uuid4()),
                    "imsi": "123456789012345",
                    "measured_at": "2026-04-27T10:00:00Z",
                    "latitude": 9999,
                    "longitude": 21.0122,
                }
            ]
        }
        response = client.post("/measurements/batch", json=test_data)
        assert response.status_code == 422
