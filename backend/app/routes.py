# ruff: noqa: I001
import gzip
import json
from datetime import datetime
from typing import Annotated
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, Query, Request
from geoalchemy2 import functions as geo_func
from sqlalchemy import extract, func
from sqlalchemy.orm import Session

from app import models, schemas
from app.analytics import (
    average_signal,
    device_sessions,
    get_heatmap_points,
    get_high_cpu_threshold,
    get_uplink_downlink_stats,
    kpi_stats,
    last_measurement,
    list_devices,
    measurements_by_cpu_category,
    propagation_map,
)
from app.database import get_db
from app.schemas import PaginatedResponse, MeasurementResponse

router = APIRouter()
DbSession = Annotated[Session, Depends(get_db)]


def measurement_filters(    # noqa: PLR0913
    query,
    session_id: str | None = None,
    android_id: str | None = None,
    network_type: str | None = None,
    cell_id: str | None = None,
    min_rsrp: int | None = None,
    max_rsrp: int | None = None,
    min_sinr: int | None = None,
    min_throughput: float | None = None,
    start_date: datetime | None = None,
    end_date: datetime | None = None,
    min_latitude: float | None = None,
    max_latitude: float | None = None,
    min_longitude: float | None = None,
    max_longitude: float | None = None,
    min_host_cpu: float | None = None,
    max_host_cpu: float | None = None,
):
    if session_id:
        query = query.filter(models.Measurement.session_id == session_id)

    if android_id:
        query = query.filter(models.Measurement.android_id == android_id)

    if network_type:
        query = query.filter(models.Measurement.network_type == network_type)

    if cell_id:
        query = query.filter(models.Measurement.cell_id == cell_id)

    if min_rsrp is not None:
        query = query.filter(models.Measurement.rsrp >= min_rsrp)

    if max_rsrp is not None:
        query = query.filter(models.Measurement.rsrp <= max_rsrp)

    if min_sinr is not None:
        query = query.filter(models.Measurement.sinr >= min_sinr)

    if min_throughput is not None:
        query = query.filter(models.Measurement.throughput_mbps >= min_throughput)

    if start_date:
        query = query.filter(models.Measurement.measured_at >= start_date)

    if end_date:
        query = query.filter(models.Measurement.measured_at <= end_date)

    if (
        min_latitude is not None
        and max_latitude is not None
        and min_longitude is not None
        and max_longitude is not None
    ):
        query = query.filter(
            geo_func.ST_Within(
                models.Measurement.location,
                geo_func.ST_MakeEnvelope(
                    min_longitude,
                    min_latitude,
                    max_longitude,
                    max_latitude,
                    4326,
                ),
            )
        )

    if min_host_cpu is not None:
        query = query.filter(models.Measurement.host_cpu >= min_host_cpu)

    if max_host_cpu is not None:
        query = query.filter(models.Measurement.host_cpu <= max_host_cpu)

    return query


@router.get("/health")
def health():
    return {"status": "ok"}


@router.get("/measurements", response_model=list[schemas.MeasurementResponse])
def get_measurements(
    db: DbSession,
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=1000),
):
    return (
        db.query(models.Measurement)
        .order_by(models.Measurement.measured_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )


@router.get("/measurements/filtered", response_model=list[schemas.MeasurementResponse])
def get_measurements_filtered(  # noqa: PLR0913
    db: DbSession,
    session_id: str | None = None,
    android_id: str | None = None,
    network_type: str | None = None,
    cell_id: str | None = None,
    min_rsrp: int | None = None,
    max_rsrp: int | None = None,
    min_sinr: int | None = None,
    min_throughput: float | None = None,
    start_date: datetime | None = None,
    end_date: datetime | None = None,
    min_latitude: float | None = None,
    max_latitude: float | None = None,
    min_longitude: float | None = None,
    max_longitude: float | None = None,
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=1000),
):
    query = db.query(models.Measurement)

    query = measurement_filters(
        query,
        session_id=session_id,
        android_id=android_id,
        network_type=network_type,
        cell_id=cell_id,
        min_rsrp=min_rsrp,
        max_rsrp=max_rsrp,
        min_sinr=min_sinr,
        min_throughput=min_throughput,
        start_date=start_date,
        end_date=end_date,
        min_latitude=min_latitude,
        max_latitude=max_latitude,
        min_longitude=min_longitude,
        max_longitude=max_longitude,
    )

    return (
        query.order_by(models.Measurement.measured_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )


@router.get("/measurements/paginated", response_model=schemas.PaginatedResponse[schemas.MeasurementResponse])
def get_measurements_paginated(
    db: DbSession,
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=1000),
):
    total = db.query(func.count(models.Measurement.id)).scalar()
    items = (
        db.query(models.Measurement)
        .order_by(models.Measurement.measured_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )
    return schemas.PaginatedResponse(items=items, total=total, skip=skip, limit=limit)


@router.get("/analysis/average-signal")
def get_avg_signal(db: DbSession):
    return average_signal(db)


@router.get("/analysis/kpi")
def get_kpi(
    db: DbSession,
    network_type: str | None = None,
    android_id: str | None = None,
):
    return kpi_stats(db, network_type=network_type, android_id=android_id)


@router.get("/analysis/heatmap")
def get_heatmap(    # noqa: PLR0913
    db: DbSession,
    parameter: str = Query(default="rsrp"),
    session_id: str | None = None,
    android_id: str | None = None,
    network_type: str | None = None,
    limit: int = Query(default=1000, le=5000),
):
    return get_heatmap_points(
        db=db,
        parameter=parameter,
        session_id=session_id,
        android_id=android_id,
        network_type=network_type,
        limit=limit,
    )


@router.get("/analysis/last-measurement")
def get_last_measurement(db: DbSession):
    return last_measurement(db)


@router.get("/devices")
def get_devices(db: DbSession):
    return list_devices(db)


@router.get("/devices/{android_id}/sessions")
def get_device_sessions(
    android_id: str,
    db: DbSession,
    limit: int = Query(default=5, le=20),
):
    return device_sessions(db, android_id=android_id, limit=limit)


@router.post("/measurements/batch", response_model=schemas.BatchResponse)
async def create_measurements_batch(request: Request, db: DbSession):
    raw_body = await request.body()

    if not raw_body:
        raise HTTPException(
            status_code=400,
            detail="Brak danych w żądaniu.",
        )

    encoding = request.headers.get("content-encoding", "").lower()

    try:
        if encoding == "gzip":
            decompressed = gzip.decompress(raw_body)
            payload_text = decompressed.decode("utf-8")
        else:
            payload_text = raw_body.decode("utf-8")

    except Exception as e:
        raise HTTPException(
            status_code=400,
            detail=f"Błąd dekompresji: Niepoprawny format Gzip lub kodowanie tekstowe. {e!s}",
        ) from e

    try:
        payload = json.loads(payload_text)
        batch = schemas.MeasurementBatch(**payload)

    except json.JSONDecodeError:
        raise HTTPException(
            status_code=400, detail="Dane nie są poprawnym formatem JSON."
        ) from None

    except Exception as e:
        raise HTTPException(status_code=422, detail=f"Błąd Pydantic: {e!s}") from e

    batch_id = batch.measurements[0].session_id if batch.measurements else None

    if not batch.measurements:
        return {"inserted": 0, "batch_id": batch_id}

    rows = [models.Measurement(**item.to_db_dict()) for item in batch.measurements]

    try:
        db.add_all(rows)
        db.commit()

        return {"inserted": len(rows), "batch_id": batch_id}

    except Exception as e:
        db.rollback()

        raise HTTPException(status_code=500, detail=f"Database insert failed: {e!s}") from e


@router.post("/sessions/start", response_model=schemas.SessionResponse)
def start_session():
    new_session_id = uuid4()
    return schemas.SessionResponse(
        session_id=new_session_id,
        started_at=datetime.now(datetime.UTC),
    )


@router.post("/sessions/{session_id}/stop")
def stop_session(session_id: str):
    return {"status": "ok", "message": f"Session {session_id} stopped"}


@router.get("/sessions")
def get_sessions(db: DbSession):
    sessions = (
        db.query(
            models.Measurement.session_id,
            func.min(models.Measurement.measured_at).label("started_at"),
            func.max(models.Measurement.measured_at).label("ended_at"),
            func.count(models.Measurement.id).label("measurement_count"),
            func.count(func.distinct(models.Measurement.android_id)).label("device_count"),
        )
        .group_by(models.Measurement.session_id)
        .order_by(func.max(models.Measurement.measured_at).desc())
        .all()
    )

    return [
        {
            "session_id": str(s.session_id),
            "started_at": s.started_at,
            "ended_at": s.ended_at,
            "measurement_count": s.measurement_count,
            "device_count": s.device_count,
        }
        for s in sessions
    ]


@router.get("/measurements/statistics", response_model=schemas.StatisticsResponse)
def get_measurements_stats( # noqa: PLR0913
    db: DbSession,
    start_date: datetime | None = None,
    end_date: datetime | None = None,
    min_latitude: float | None = None,
    max_latitude: float | None = None,
    min_longitude: float | None = None,
    max_longitude: float | None = None,
):
    q = db.query(models.Measurement)

    q = measurement_filters(
        q,
        start_date=start_date,
        end_date=end_date,
        min_latitude=min_latitude,
        max_latitude=max_latitude,
        min_longitude=min_longitude,
        max_longitude=max_longitude,
    )

    total = q.count()

    stats = q.with_entities(
        func.count(func.distinct(models.Measurement.session_id)).label("unique_sessions"),
        func.count(func.distinct(models.Measurement.android_id)).label("unique_devices"),
        func.avg(models.Measurement.rsrp).label("avg_rsrp"),
        func.min(models.Measurement.rsrp).label("min_rsrp"),
        func.max(models.Measurement.rsrp).label("max_rsrp"),
        func.avg(models.Measurement.sinr).label("avg_sinr"),
        func.min(models.Measurement.sinr).label("min_sinr"),
        func.max(models.Measurement.sinr).label("max_sinr"),
        func.avg(models.Measurement.throughput_mbps).label("avg_throughput"),
        func.max(models.Measurement.throughput_mbps).label("max_throughput"),
    ).first()

    filtered_base = measurement_filters(
        db.query(models.Measurement),
        start_date=start_date,
        end_date=end_date,
        min_latitude=min_latitude,
        max_latitude=max_latitude,
        min_longitude=min_longitude,
        max_longitude=max_longitude,
    )

    network_dist = {
        row[0]: row[1]
        for row in filtered_base.with_entities(
            models.Measurement.network_type,
            func.count(models.Measurement.id),
        )
        .group_by(models.Measurement.network_type)
        .all()
        if row[0]
    }

    band_dist = {
        str(row[0]): row[1]
        for row in filtered_base.with_entities(
            models.Measurement.band,
            func.count(models.Measurement.id),
        )
        .group_by(models.Measurement.band)
        .all()
        if row[0] is not None
    }

    hour_dist = {
        int(row[0]): row[1]
        for row in filtered_base.with_entities(
            extract("hour", models.Measurement.measured_at),
            func.count(models.Measurement.id),
        )
        .group_by(extract("hour", models.Measurement.measured_at))
        .all()
        if row[0] is not None
    }

    return {
        "total_measurements": total,
        "unique_sessions": stats.unique_sessions or 0,
        "unique_devices": stats.unique_devices or 0,
        "avg_rsrp": float(stats.avg_rsrp) if stats.avg_rsrp is not None else None,
        "min_rsrp": stats.min_rsrp,
        "max_rsrp": stats.max_rsrp,
        "avg_sinr": float(stats.avg_sinr) if stats.avg_sinr is not None else None,
        "min_sinr": stats.min_sinr,
        "max_sinr": stats.max_sinr,
        "avg_throughput": float(stats.avg_throughput) if stats.avg_throughput is not None else None,
        "max_throughput": float(stats.max_throughput) if stats.max_throughput is not None else None,
        "network_distribution": network_dist,
        "band_distribution": band_dist,
        "measurements_by_hour": hour_dist,
    }


@router.get("/measurements/analysis-cpu-filter", response_model=schemas.PaginatedResponse[schemas.MeasurementResponse])
def get_measurements_with_cpu_filter(
    db: DbSession,
    cpu_filter: str = Query(default="all"),  # "all", "without_high", "only_high"
    cpu_threshold: float = Query(default=50.0),
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=1000),
):
    """
    cpu_filter:
    - "all": wszystkie pomiary
    - "without_high": bez pomiarów ze zbyt wysokim CPU
    - "only_high": tylko pomiary ze zbyt wysokim CPU
    """
    query = db.query(models.Measurement)

    if cpu_filter == "without_high":
        query = query.filter(models.Measurement.host_cpu < cpu_threshold)
    elif cpu_filter == "only_high":
        query = query.filter(models.Measurement.host_cpu >= cpu_threshold)

    total = query.count()

    items = (
        query.order_by(models.Measurement.measured_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )

    serialized_items = [schemas.MeasurementResponse.model_validate(item) for item in items]

    return PaginatedResponse(items=serialized_items, total=total, skip=skip, limit=limit)

@router.get("/analysis/kpi-by-cpu")
def get_kpi_with_cpu_filter(
    db: DbSession,
    cpu_filter: str = Query(default="all"),
    cpu_threshold: float = Query(default=50.0),
):
    """KPI z uwzględnieniem filtrowania CPU"""
    query = db.query(models.Measurement)

    if cpu_filter == "without_high":
        query = query.filter(models.Measurement.host_cpu < cpu_threshold)
    elif cpu_filter == "only_high":
        query = query.filter(models.Measurement.host_cpu >= cpu_threshold)

    result = query.with_entities(
        func.avg(models.Measurement.throughput_mbps).label("avg_throughput"),
        func.avg(models.Measurement.latency_ms).label("avg_latency"),
        func.avg(models.Measurement.rsrp).label("avg_rsrp"),
    ).first()

    return {
        "cpu_filter": cpu_filter,
        "cpu_threshold": cpu_threshold,
        "avg_throughput": float(result.avg_throughput) if result and result.avg_throughput else None,
        "avg_latency": float(result.avg_latency) if result and result.avg_latency else None,
        "avg_rsrp": float(result.avg_rsrp) if result and result.avg_rsrp else None,
    }


@router.get("/analysis/propagation")
def get_propagation_map( # noqa: PLR0913
    db: DbSession,
    parameter: str = Query(default="rsrp"),
    android_id: str | None = None,
    session_id: str | None = None,
    network_type: str | None = None,
    resolution: int = Query(default=100, le=100),
):
    return propagation_map(
        db=db,
        parameter=parameter,
        android_id=android_id,
        session_id=session_id,
        network_type=network_type,
        resolution=resolution,
    )

@router.get("/analysis/ul-dl-stats")
def get_uplink_downlink_stats_endpoint(
    db: DbSession,
    session_id: str | None = None,
):
    return get_uplink_downlink_stats(db, session_id)   # bez importu wewnątrz


@router.get("/analysis/cpu-threshold")
def get_cpu_threshold_endpoint(db: DbSession):
    return {
        "threshold": get_high_cpu_threshold(db),
        "categories": measurements_by_cpu_category(db)
    }
