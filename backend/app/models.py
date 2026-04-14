from geoalchemy2 import Geography
from geoalchemy2.shape import to_shape
from sqlalchemy import TIMESTAMP, Column, Float, Index, Integer, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.ext.hybrid import hybrid_property

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
    location = Column(
        Geography(
            geometry_type="POINT",
            srid=4326,
            spatial_index=True,
        )
    )
    # przechowujemy pojedyncze lokalizacje w bazie
    # latitude/longitude jest dynamicznie rozdzielane w aplikacji lub querry

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
        Index("ids_measurements_location", location, postgresql_using="gist"),
        Index("idx_measurements_time", measured_at),
        Index("idx_measurement_cell", cell_id),
    )

    @hybrid_property
    def latitude(self) -> float | None:
        return to_shape(self.location).y if self.location else None

    @hybrid_property
    def longitude(self) -> float | None:
        return to_shape(self.location).x if self.location else None
