from datetime import datetime

from geoalchemy2 import functions as geo_func
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.models import Measurement


def _to_float(value):
    return float(value) if value is not None else None


def average_signal(db: Session):
    result = db.query(
        func.avg(Measurement.rsrp).label("avg_rsrp"),
        func.avg(Measurement.sinr).label("avg_sinr"),
    ).first()

    return {
        "avg_rsrp": _to_float(result.avg_rsrp) if result else None,
        "avg_sinr": _to_float(result.avg_sinr) if result else None,
    }


def kpi_stats(db: Session, network_type: str | None = None, android_id: str | None = None):
    query = db.query(Measurement)

    if network_type:
        query = query.filter(Measurement.network_type == network_type)

    if android_id:
        query = query.filter(Measurement.android_id == android_id)

    result = query.with_entities(
        func.min(Measurement.rsrp).label("min_rsrp"),
        func.max(Measurement.rsrp).label("max_rsrp"),
        func.avg(Measurement.rsrp).label("avg_rsrp"),

        func.min(Measurement.rsrq).label("min_rsrq"),
        func.max(Measurement.rsrq).label("max_rsrq"),
        func.avg(Measurement.rsrq).label("avg_rsrq"),

        func.min(Measurement.sinr).label("min_sinr"),
        func.max(Measurement.sinr).label("max_sinr"),
        func.avg(Measurement.sinr).label("avg_sinr"),

        func.min(Measurement.throughput_mbps).label("min_throughput"),
        func.max(Measurement.throughput_mbps).label("max_throughput"),
        func.avg(Measurement.throughput_mbps).label("avg_throughput"),
    ).first()

    if not result:
        return {}

    return {
        "rsrp": {
            "min": result.min_rsrp,
            "max": result.max_rsrp,
            "avg": _to_float(result.avg_rsrp),
        },
        "rsrq": {
            "min": result.min_rsrq,
            "max": result.max_rsrq,
            "avg": _to_float(result.avg_rsrq),
        },
        "sinr": {
            "min": result.min_sinr,
            "max": result.max_sinr,
            "avg": _to_float(result.avg_sinr),
        },
        "throughput_mbps": {
            "min": _to_float(result.min_throughput),
            "max": _to_float(result.max_throughput),
            "avg": _to_float(result.avg_throughput),
        },
    }


def get_heatmap_points(
    db: Session,
    parameter: str = "rsrp",
    session_id: str | None = None,
    android_id: str | None = None,
    network_type: str | None = None,
    limit: int = 1000,
):
    allowed_parameters = {
        "rsrp": Measurement.rsrp,
        "rsrq": Measurement.rsrq,
        "sinr": Measurement.sinr,
        "throughput_mbps": Measurement.throughput_mbps,
    }

    metric_column = allowed_parameters.get(parameter, Measurement.rsrp)

    query = db.query(
        geo_func.ST_Y(geo_func.ST_GeometryFromWKB(Measurement.location)).label("latitude"),
        geo_func.ST_X(geo_func.ST_GeometryFromWKB(Measurement.location)).label("longitude"),
        metric_column.label("value"),
        Measurement.session_id,
        Measurement.android_id,
        Measurement.measured_at,
    ).filter(Measurement.location.isnot(None))

    if session_id:
        query = query.filter(Measurement.session_id == session_id)

    if android_id:
        query = query.filter(Measurement.android_id == android_id)

    if network_type:
        query = query.filter(Measurement.network_type == network_type)

    rows = query.order_by(Measurement.measured_at.desc()).limit(limit).all()

    return [
        {
            "latitude": _to_float(row.latitude),
            "longitude": _to_float(row.longitude),
            "value": _to_float(row.value),
            "session_id": str(row.session_id),
            "android_id": row.android_id,
            "measured_at": row.measured_at,
        }
        for row in rows
    ]


def list_devices(db: Session):
    rows = db.query(
        Measurement.android_id,
        func.count(Measurement.id).label("measurement_count"),
        func.count(func.distinct(Measurement.session_id)).label("session_count"),
        func.max(Measurement.measured_at).label("last_seen"),
        func.avg(Measurement.battery_level).label("avg_battery"),
        func.max(Measurement.battery_level).label("last_battery"),
    ).group_by(
        Measurement.android_id
    ).order_by(
        func.max(Measurement.measured_at).desc()
    ).all()

    return [
        {
            "android_id": row.android_id,
            "measurement_count": row.measurement_count,
            "session_count": row.session_count,
            "last_seen": row.last_seen,
            "avg_battery": _to_float(row.avg_battery),
            "last_battery": row.last_battery,
        }
        for row in rows
    ]


def device_sessions(db: Session, android_id: str, limit: int = 5):
    rows = db.query(
        Measurement.session_id,
        func.min(Measurement.measured_at).label("started_at"),
        func.max(Measurement.measured_at).label("ended_at"),
        func.count(Measurement.id).label("measurement_count"),
        func.avg(Measurement.rsrp).label("avg_rsrp"),
        func.avg(Measurement.sinr).label("avg_sinr"),
        func.avg(Measurement.throughput_mbps).label("avg_throughput"),
    ).filter(
        Measurement.android_id == android_id
    ).group_by(
        Measurement.session_id
    ).order_by(
        func.max(Measurement.measured_at).desc()
    ).limit(limit).all()

    return [
        {
            "session_id": str(row.session_id),
            "started_at": row.started_at,
            "ended_at": row.ended_at,
            "measurement_count": row.measurement_count,
            "avg_rsrp": _to_float(row.avg_rsrp),
            "avg_sinr": _to_float(row.avg_sinr),
            "avg_throughput": _to_float(row.avg_throughput),
        }
        for row in rows
    ]


def last_measurement(db: Session):
    row = db.query(Measurement).order_by(Measurement.measured_at.desc()).first()

    if not row:
        return None

    return {
        "id": row.id,
        "session_id": str(row.session_id),
        "android_id": row.android_id,
        "measured_at": row.measured_at,
        "latitude": row.latitude,
        "longitude": row.longitude,
        "rsrp": row.rsrp,
        "rsrq": row.rsrq,
        "sinr": row.sinr,
        "network_type": row.network_type,
        "battery_level": row.battery_level,
        "throughput_mbps": row.throughput_mbps,
        "test_duration": row.test_duration,
    }