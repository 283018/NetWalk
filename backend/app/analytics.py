from sqlalchemy.orm import Session
from sqlalchemy import func
from app.models import Measurement


def average_signal(db: Session):
    result = db.query(
        func.avg(Measurement.rsrp).label("avg_rsrp"),
        func.avg(Measurement.sinr).label("avg_sinr")
    ).first()

    return {
        "avg_rsrp": float(result.avg_rsrp) if result.avg_rsrp else None,
        "avg_sinr": float(result.avg_sinr) if result.avg_sinr else None,
    }