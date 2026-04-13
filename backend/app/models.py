from sqlalchemy import Column, Integer, Float, String, TIMESTAMP, Index
from sqlalchemy.dialects.postgresql import UUID, GEOGRAPHY
from sqlalchemy.sql import func
from app.database import Base


class Measurement(Base):
    __tablename__ = "measurements"

    id = Column(Integer, primary_key=True, index=True)
    session_id = Column(UUID, nullable=False)

    # id użytkownika
    imsi = Column(String, nullable=False)
    # id urządzenia
    imei = Column(String)

    # czas
    measured_at = Column(TIMESTAMP(timezone=True), nullable=False)

    # lokalizacja
    location = Column(GEOGRAPHY(geometry_type='POINT', srid=4326))
    latitude = Column(Float)
    longitude = Column(Float)

    # parametry sieci
    rsrp = Column(Integer)
    rsrq = Column(Integer)
    sinr = Column(Integer)
    network_type = Column(String)
    cell_id = Column(String)
    tac = Column(Integer)
    band = Column(Integer)

    # parametry systemowe
    battery_level = Column(Integer)
    processor_temp = Column(Float)
    os_version = Column(String)

    # testy wydajności
    throughput_mbps = Column(Float)
    test_start_time = Column(TIMESTAMP(timezone=True))
    test_end_time = Column(TIMESTAMP(timezone=True))

    # indeksy do wydajności
    __table_args__ = (
        Index('ids_measurements_location', location, postgersql_using='gist'),
        Index('idx_measurements_time', measure_at),
        Index('idx_measurement_cell', cell_id),
    )
