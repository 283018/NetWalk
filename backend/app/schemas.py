from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict


class MeasurementCreate(BaseModel):
    """
    JSON STRUCTURE FOR MOBILE APP REQUEST:
    {
        "session_id": "550e8400-e29b-41d4-a716-446655440000",
        "imsi": "310150123456789",
        "imei": "123456789012345",
        "measured_at": "2026-04-19T11:54:00Z",
        "latitude": 52.2297,
        "longitude": 21.0122,
        "rsrp": -75,
        "rsrq": -10,
        "sinr": 15,
        "network_type": "LTE",
        "cell_id": "12345678",
        "tac": 12345,
        "band": 3,
        "battery_level": 85,
        "processor_temp": 45.2,
        "os_version": "Android 13",
        "throughput_mbps": 120.5,
        "test_start_time": "2026-04-19T11:53:00Z",
        "test_end_time": "2026-04-19T11:54:00Z"
    }
    """
    session_id: UUID
    imsi: str
    imei: str | None = None
    measured_at: datetime
    latitude: float | None = None
    longitude: float | None = None
    rsrp: int | None = None
    rsrq: int | None = None
    sinr: int | None = None
    network_type: str | None = None
    cell_id: str | None = None
    tac: int | None = None
    band: int | None = None
    battery_level: int | None = None
    processor_temp: float | None = None
    os_version: str | None = None
    throughput_mbps: float | None = None
    test_start_time: datetime | None = None
    test_end_time: datetime | None = None

    def to_db_dict(self) -> dict:
        """
        Converts API filds to SQLAlchemy model field.
        """
        data = self.model_dump(exclude={"latitude", "longitude"})
        if self.latitude is not None and self.longitude is not None:
            # PostGIS WKT format: "POINT(longitude latitude)"
            data["location"] = f"POINT({self.longitude} {self.latitude})"
        return data

    model_config = {"from_attributes": True}


class MeasurementResponse(BaseModel):
    """
    JSON STRUCTURE FOR FRONTEND RESPONSE:
    {
        "id": 12345,
        "session_id": "550e8400-e29b-41d4-a716-446655440000",
        "imsi": "310150123456789",
        "imei": "123456789012345",
        "measured_at": "2026-04-19T11:54:00Z",
        "latitude": 52.2297,
        "longitude": 21.0122,
        "rsrp": -75,
        "rsrq": -10,
        "sinr": 15,
        "network_type": "LTE",
        "cell_id": "12345678",
        "tac": 12345,
        "band": 3,
        "battery_level": 85,
        "processor_temp": 45.2,
        "os_version": "Android 13",
        "throughput_mbps": 120.5,
        "test_start_time": "2026-04-19T11:53:00Z",
        "test_end_time": "2026-04-19T11:54:00Z"
    }
    """
    id: int
    session_id: UUID
    imsi: str
    imei: str | None = None
    measured_at: datetime
    latitude: float | None = None
    longitude: float | None = None
    rsrp: int | None = None
    rsrq: int | None = None
    sinr: int | None = None
    network_type: str | None = None
    cell_id: str | None = None
    tac: int | None = None
    band: int | None = None
    battery_level: int | None = None
    processor_temp: float | None = None
    os_version: str | None = None
    throughput_mbps: float | None = None
    test_start_time: datetime | None = None
    test_end_time: datetime | None = None

    model_config = ConfigDict(from_attributes=True)


class MeasurementBatch(BaseModel):
    """
    JSON STRUCTURE FOR BATCH REQUEST:
    {
        "measurements": [
            { ... MeasurementCreate object ... },
            { ... MeasurementCreate object ... }
        ]
    }
    """
    measurements: list[MeasurementCreate]


class BatchResponse(BaseModel):
    """
    JSON STRUCTURE FOR BATCH RESPONSE:
    {
        "inserted": 42,
        "batch_id": "550e8400-e29b-41d4-a716-446655440000"
    }
    """
    inserted: int
    batch_id: UUID | None = None
