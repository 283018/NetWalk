from datetime import datetime
from typing import Any, Self, cast
from uuid import UUID

from geoalchemy2.shape import to_shape
from pydantic import BaseModel, ConfigDict, Field, computed_field, model_validator
from shapely.geometry import Point


class MeasurementBase(BaseModel):
    session_id: UUID
    imsi: str
    imei: str | None = None
    measured_at: datetime
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


class MeasurementCreate(MeasurementBase):
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

    latitude: float | None = Field(default=None, exclude=True)
    longitude: float | None = Field(default=None, exclude=True)
    location: str | None = None

    @model_validator(mode="after")
    def create_wkt_location(self) -> Self:
        if self.latitude is not None and self.longitude is not None:
            self.location = f"POINT({self.longitude} {self.latitude})"
        return self


class MeasurementResponse(MeasurementBase):
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
    location: Any = Field(exclude=True)

    model_config = ConfigDict(from_attributes=True)

    @computed_field
    @property
    def latitude(self) -> float | None:
        if hasattr(self, "location") and self.location is not None:
            point = cast(Point, to_shape(self.location))
            return point.y
        return None

    @computed_field
    @property
    def longitude(self) -> float | None:
        if hasattr(self, "location") and self.location is not None:
            point = cast(Point, to_shape(self.location))
            return point.x
        return None


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
