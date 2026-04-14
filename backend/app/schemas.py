from datetime import datetime
from uuid import UUID

from pydantic import BaseModel


class MeasurementCreate(BaseModel):
    session_id: UUID
    imsi: str
    imei: str | None = None
    measured_at: datetime
    latitude: float | None = None
    longitude: float | None = None
    rsrp: int | None = None
    sinr: int | None = None
    network_type: str | None = None
    cell_id: str | None = None
    battery_level: int | None = None
    processor_temp: float | None = None
    os_version: str | None = None
    throughput_mbps: float | None = None
    test_start_time: datetime | None = None
    test_end_time: datetime | None = None


class MeasurementBatch(BaseModel):
    measurements: list[MeasurementCreate]
