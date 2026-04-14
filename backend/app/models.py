from sqlalchemy import TIMESTAMP, Column, Float, Integer, String
from sqlalchemy.dialects.postgresql import UUID

from app.database import Base


class Measurement(Base):
    __tablename__ = "measurements"

    id = Column(Integer, primary_key=True, index=True)
    session_id = Column(UUID, nullable=False)

    imsi = Column(String, nullable=False)
    imei = Column(String)

    measured_at = Column(TIMESTAMP(timezone=True), nullable=False)

    latitude = Column(Float)
    longitude = Column(Float)

    rsrp = Column(Integer)
    sinr = Column(Integer)
    network_type = Column(String)
    cell_id = Column(String)

    battery_level = Column(Integer)
    processor_temp = Column(Float)
    os_version = Column(String)

    throughput_mbps = Column(Float)
    test_start_time = Column(TIMESTAMP(timezone=True))
    test_end_time = Column(TIMESTAMP(timezone=True))
