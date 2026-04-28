import gzip
import json

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Request
from sqlalchemy.orm import Session

from app import models, schemas
from app.analytics import average_signal
from app.database import get_db

router = APIRouter()
DbSession = Annotated[Session, Depends(get_db)]


@router.get("/health")  # to do sprawdzania czy odbiera
def health():
    return {"status": "ok"}


# Zmiana: używamy MeasurementResponse zamiast MeasurementCreate
@router.get(
    "/measurements",
    response_model=list[schemas.MeasurementResponse],
)
def get_measurements(db: DbSession):
    return db.query(models.Measurement).limit(100).all()


@router.get("/analysis/average-signal")
def get_avg_signal(db: DbSession):
    return average_signal(db)


@router.post("/measurements/batch", response_model=schemas.BatchResponse)
async def create_measurements_batch(request: Request, db: DbSession):
    raw_body = await request.body()

    if not raw_body:
        raise HTTPException(status_code=400, detail="Brak danych w żądaniu.")

    encoding = request.headers.get("content-encoding", "").lower()

    try:
        if encoding == "gzip":
            decompressed = gzip.decompress(raw_body)
            payload_text = decompressed.decode('utf-8')
        else:
            # jak nie bedize nagłówka Gzip, zakładam, że to zwykły json
            payload_text = raw_body.decode('utf-8')
    except Exception as e:
        raise HTTPException(
            status_code=400,
            detail=f"Błąd dekompresji: Niepoprawny format Gzip lub kodowanie tekstowe. {str(e)}"
        )

    try:
        payload = json.loads(payload_text)
        batch = schemas.MeasurementBatch(**payload)
    except json.JSONDecodeError:
        raise HTTPException(status_code=400, detail="dane nie są poprawnym formatem JSON.")
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"Błąd Pydantic: {str(e)}")

    batch_id = batch.measurements[0].session_id if batch.measurements else None

    # jak lista pusta
    if not batch.measurements:
        return {"inserted": 0, "batch_id": batch_id}

    # konwersja latitude/longitude na PostGIS WKT format: "POINT(longitude latitude)"
    # model_dump automatycznie exluduje lat/lng i includuje loc
    rows = [models.Measurement(**item.model_dump()) for item in batch.measurements]

    try:
        db.add_all(rows)
        db.commit()
        return {"inserted": len(rows),
                "batch_id": batch_id}
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=f"Database insert failed: {e!s}") from e
