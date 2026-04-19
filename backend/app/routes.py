from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app import models, schemas
from app.analytics import average_signal
from app.database import get_db

router = APIRouter()


@router.get("/health")  # to do sprawdzania czy odbiera
def health():
    return {"status": "ok"}


# Zmiana: używamy MeasurementResponse zamiast MeasurementCreate
@router.get("/measurements", response_model=list[schemas.MeasurementResponse])
def get_measurements(db: Session = Depends(get_db)):
    measurements = db.query(models.Measurement).limit(100).all()
    # Zmiana location na lat/lng dla response'u
    result = []
    for m in measurements:
        resp = schemas.MeasurementResponse.model_validate(m)
        if m.location:
            resp.latitude = m.latitude
            resp.longitude = m.longitude
        result.append(resp)
    return result


@router.get("/analysis/average-signal")
def get_avg_signal(db: Session = Depends(get_db)):  # noqa: B008
    return average_signal(db)


@router.post("/measurements/batch", response_model=schemas.BatchResponse)
def create_measurements_batch(
    batch: schemas.MeasurementBatch,
    db: Session = Depends(get_db),  # noqa: B008
):
    if not batch.measurements:
        raise HTTPException(status_code=400, detail="Batch must contain at least one measurment.")

    # konwersja latitude/longitude na PostGIS WKT format: "POINT(longitude latituse)"
    rows = [models.Measurement(**item.to_db_dict()) for item in batch.measurements]

    try:
        db.add_all(rows)
        db.commit()
        return {"inserted": len(rows)}
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=f"Database insert failed: {e!s}") from e
