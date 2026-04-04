from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app import models, schemas
from app.analytics import average_signal

router = APIRouter()


@router.get("/health")#to do sprawdzania czy odbiera
def health():
    return {"status": "ok"}

@router.get("/measurements")
def get_measurements(db: Session = Depends(get_db)):
    return db.query(models.Measurement).limit(100).all()

@router.get("/analysis/average-signal")
def get_avg_signal(db: Session = Depends(get_db)):
    return average_signal(db)

@router.post("/measurements/batch")
def create_measurements_batch(
    batch: schemas.MeasurementBatch,
    db: Session = Depends(get_db),
):
    rows = [models.Measurement(**item.model_dump()) for item in batch.measurements]
    db.add_all(rows)
    db.commit()
    return {"inserted": len(rows)}