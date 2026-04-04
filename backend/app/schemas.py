from pydantic import BaseModel
from datetime import datetime
from uuid import UUID
from typing import Optional


class MeasurementCreate(BaseModel):
    session_id: UUID
    imsi: str
    imei: Optional[str] = None
    measured_at: datetime
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    rsrp: Optional[int] = None
    sinr: Optional[int] = None
    network_type: Optional[str] = None
    cell_id: Optional[str] = None
    battery_level: Optional[int] = None
    processor_temp: Optional[float] = None
    os_version: Optional[str] = None
    throughput_mbps: Optional[float] = None
    test_start_time: Optional[datetime] = None
    test_end_time: Optional[datetime] = None


class MeasurementBatch(BaseModel):
    measurements: list[MeasurementCreate]