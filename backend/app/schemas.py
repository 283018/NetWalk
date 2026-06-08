from datetime import datetime
from enum import StrEnum
from functools import cached_property
from typing import Generic, TypeVar, cast
from uuid import UUID

from geoalchemy2.elements import WKBElement
from geoalchemy2.shape import from_shape, to_shape
from pydantic import BaseModel, ConfigDict, Field, computed_field, field_validator
from shapely.geometry import Point

LAT_RANGE = 90
LON_RANGE = 180


class Protocol(StrEnum):
    TCP = "TCP"
    UDP = "UDP"
    MIXED = "MIXED"


class MeasurementBase(BaseModel):
    model_config = ConfigDict(extra="ignore")
    session_id: UUID
    android_id: str
    cid: int | None
    measured_at: datetime
    rsrp: int | None = None
    rsrq: int | None = None
    sinr: int | None = None
    network_type: str | None = None
    cell_id: str | None = None
    tac: int | None = None
    radio_frequency: int | None = None
    band: int | None = None
    bandwidth: int | None = None
    battery_level: int | None = None
    battery_temp: float | None = None
    os_version: str | None = None
    throughput_mbps: float | None = None
    latency_ms: float | None = None
    jitter_ms: float | None = None
    test_start_time: datetime | None = None
    test_duration: float | None = None
    mean_rtt: float | None = None
    min_rtt: float | None = None
    max_rtt: float | None = None
    host_cpu: float | None = None
    remote_cpu: float | None = None
    retransmits: float | None = None
    protocol: Protocol | None = None
    ul_throughput_mbps: float | None = None
    ul_latency_ms: float | None = None
    ul_jitter_ms: float | None = None
    ul_mean_rtt: float | None = None
    ul_min_rtt: float | None = None
    ul_max_rtt: float | None = None
    ul_retransmits: int | None = None
    ul_lost_packets: int | None = None
    ul_lost_percent: float | None = None
    dl_throughput_mbps: float | None = None
    dl_latency_ms: float | None = None
    dl_jitter_ms: float | None = None
    dl_mean_rtt: float | None = None
    dl_min_rtt: float | None = None
    dl_max_rtt: float | None = None
    dl_retransmits: int | None = None
    dl_lost_packets: int | None = None
    dl_lost_percent: float | None = None
    test_carrier_mode: str | None = None
    cells_involved: str | None = None
    primary_cell_id: str | None = None


class MeasurementCreate(MeasurementBase):
    """
    JSON STRUCTURE FOR MOBILE APP REQUEST:
    ``` json
    {
        "session_id": "620363e2-f0ca-4c14-be20-3f7ff6967527",
        "android_id": "d01bb08aa9bdc9a5",
        "cid": 47108,
        "measured_at": "2026-05-14T03:37:24.682Z",
        "latitude": 37.4219983,
        "longitude": -122.084,
        "rsrp": -44,
        "rsrq": -3,
        "sinr": 3,
        "network_type": "10",
        "tac": 8514,
        "cell_id": "0",
        "radio_frequency": 7,
        "band": 1,
        "bandwidth": null,
        "battery_level": 47,
        "battery_temp": 25.0,
        "os_version": "Android 14"
        "throughput_mbps": 998.66,
        "latency_ms": 661.0,
        "jitter_ms": 214.0,
        "test_start_time": "2026-05-14T03:37:24Z",
        "test_duration": 10.25,
        "mean_rtt": 661.0,
        "min_rtt": 537.0,
        "max_rtt": 751.0,
        "host_cpu": 17.1,
        "remote_cpu": 114.6,
        "retransmits": 0
    }
    ```
    """

    latitude: float | None = Field(default=None, exclude=True)
    longitude: float | None = Field(default=None, exclude=True)

    @field_validator("latitude")
    @classmethod
    def validate_latitude(cls, v: float | None) -> float | None:
        if v is not None and (v < -LAT_RANGE or v > LAT_RANGE):
            msg = f"latitude must be between {-LAT_RANGE} and {LAT_RANGE}"
            raise ValueError(msg)
        return v

    @field_validator("longitude")
    @classmethod
    def validate_longitude(cls, v: float | None) -> float | None:
        if v is not None and (v < -LON_RANGE or v > LON_RANGE):
            msg = f"longitude must be between {-LON_RANGE} and {LON_RANGE}"
            raise ValueError(msg)
        return v

    def to_db_dict(self) -> dict:
        data = self.model_dump(exclude={"latitude", "longitude"})
        if self.latitude is not None and self.longitude is not None:
            data["location"] = from_shape(
                Point(self.longitude, self.latitude),
                srid=4326,
            )
        return data


class MeasurementResponse(MeasurementBase):
    """
    JSON STRUCTURE FOR FRONTEND RESPONSE:
    ``` json
    {
        "id": 12345,
        "session_id": "620363e2-f0ca-4c14-be20-3f7ff6967527",
        "android_id": "d01bb08aa9bdc9a5",
        "cid": 47108,
        "measured_at": "2026-05-14T03:37:24.682Z",
        "latitude": 37.4219983,
        "longitude": -122.084,
        "rsrp": -44,
        "rsrq": -3,
        "sinr": 3,
        "network_type": "10",
        "tac": 8514,
        "cell_id": "0",
        "radio_frequency": 7,
        "band": 1,
        "bandwidth": null,
        "battery_level": 47,
        "battery_temp": 25.0,
        "os_version": "Android 14"
        "throughput_mbps": 998.66,
        "latency_ms": 661.0,
        "jitter_ms": 214.0,
        "test_start_time": "2026-05-14T03:37:24Z",
        "test_duration": 10.25,
        "mean_rtt": 661.0,
        "min_rtt": 537.0,
        "max_rtt": 751.0,
        "host_cpu": 17.1,
        "remote_cpu": 114.6,
        "retransmits": 0
    }
    ```
    """

    id: int
    location: WKBElement | None = Field(default=None, exclude=True)

    model_config = ConfigDict(from_attributes=True, arbitrary_types_allowed=True)

    @computed_field
    @property
    def latitude(self) -> float | None:
        p = self._point
        return p.y if p else None

    @computed_field
    @property
    def longitude(self) -> float | None:
        p = self._point
        return p.x if p else None

    @cached_property
    def _point(self) -> Point | None:
        if self.location is None:
            return None
        return cast(Point, to_shape(self.location))


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


# Statystyki dla dashboardu
class StatisticsResponse(BaseModel):
    total_measurements: int
    unique_sessions: int
    unique_devices: int

    # rsrp
    avg_rsrp: float | None
    min_rsrp: int | None
    max_rsrp: int | None
    # sinr
    avg_sinr: float | None
    min_sinr: int | None
    max_sinr: int | None

    # prędkość
    avg_throughput: float | None
    max_throughput: float | None

    # wykresy i rozkłady
    network_distribution: dict[str, int]
    band_distribution: dict[str, int]
    measurements_by_hour: dict[int, int]


# Odpowiedź sesji
class SessionResponse(BaseModel):
    session_id: UUID
    started_at: datetime


T = TypeVar("T")


class PaginatedResponse(BaseModel, Generic[T]):
    items: list[T]
    total: int
    skip: int
    limit: int
