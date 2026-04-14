from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app import models, schemas
from app.analytics import average_signal
from app.database import get_db

router = APIRouter()


@router.get("/health")  # to do sprawdzania czy odbiera
def health():
    return {"status": "ok"}


# na razie używamy tego samego modelu, pożniej lepiej zmienić - chociażby żeby id pokazywał
@router.get("/measurements", response_model=list[schemas.MeasurementCreate])
def get_measurements(db: Session = Depends(get_db)):  # noqa: B008
    return db.query(models.Measurement).limit(100).all()


@router.get("/analysis/average-signal")
def get_avg_signal(db: Session = Depends(get_db)):  # noqa: B008
    return average_signal(db)


@router.post("/measurements/batch")
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
