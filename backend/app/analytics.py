from geoalchemy2 import functions as geo_func
from sqlalchemy import func, text
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
        geo_func.ST_Y(geo_func.ST_GeomFromWKB(Measurement.location)).label("latitude"),
        geo_func.ST_X(geo_func.ST_GeomFromWKB(Measurement.location)).label("longitude"),
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
    rows = (
        db.query(
            Measurement.android_id,
            func.count(Measurement.id).label("measurement_count"),
            func.count(func.distinct(Measurement.session_id)).label("session_count"),
            func.max(Measurement.measured_at).label("last_seen"),
            func.avg(Measurement.battery_level).label("avg_battery"),
            func.max(Measurement.battery_level).label("last_battery"),
        )
        .group_by(Measurement.android_id)
        .order_by(func.max(Measurement.measured_at).desc())
        .all()
    )

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
    rows = (
        db.query(
            Measurement.session_id,
            func.min(Measurement.measured_at).label("started_at"),
            func.max(Measurement.measured_at).label("ended_at"),
            func.count(Measurement.id).label("measurement_count"),
            func.avg(Measurement.rsrp).label("avg_rsrp"),
            func.avg(Measurement.sinr).label("avg_sinr"),
            func.avg(Measurement.throughput_mbps).label("avg_throughput"),
        )
        .filter(Measurement.android_id == android_id)
        .group_by(Measurement.session_id)
        .order_by(func.max(Measurement.measured_at).desc())
        .limit(limit)
        .all()
    )

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


def propagation_map(
    db: Session,
    parameter: str = "rsrp",
    android_id: str | None = None,
    session_id: str | None = None,
    network_type: str | None = None,
    resolution: int = 50,
):
    allowed = {"rsrp", "rsrq", "sinr", "throughput_mbps"}
    col = parameter if parameter in allowed else "rsrp"

    # filtry
    filters = ["location IS NOT NULL", f"{col} IS NOT NULL"]
    params = {"resolution": resolution - 1}

    if android_id:
        filters.append("android_id = :android_id")
        params["android_id"] = android_id
    if session_id:
        filters.append("session_id = :session_id")
        params["session_id"] = session_id
    if network_type:
        filters.append("network_type = :network_type")
        params["network_type"] = network_type

    where = " AND ".join(filters)

    bounds = db.execute(
        text(
            f"""
        SELECT
            ST_YMin(ST_Extent(location::geometry)) as lat_min,
            ST_YMax(ST_Extent(location::geometry)) as lat_max,
            ST_XMin(ST_Extent(location::geometry)) as lon_min,
            ST_XMax(ST_Extent(location::geometry)) as lon_max
        FROM measurements
        WHERE {where}
    """
        ),
        params,
    ).first()

    if not bounds or bounds.lat_min is None:
        return {"points": [], "bounds": None}

    params.update(
        {
            "lat_min": bounds.lat_min - 0.001,
            "lat_max": bounds.lat_max + 0.001,
            "lon_min": bounds.lon_min - 0.001,
            "lon_max": bounds.lon_max + 0.001,
        }
    )

    result = db.execute(
        text(
            f"""
        WITH grid AS (
            SELECT
                :lat_min + (:lat_max - :lat_min) * i / :resolution AS lat,
                :lon_min + (:lon_max - :lon_min) * j / :resolution AS lon
            FROM
                generate_series(0, :resolution) AS i,
                generate_series(0, :resolution) AS j
        ),
        measurements_filtered AS (
            SELECT
                {col} AS value,
                location
            FROM measurements
            WHERE {where}
        ),
        idw AS (
            SELECT
                g.lat,
                g.lon,
                SUM(m.value / POWER(NULLIF(ST_Distance(
                    ST_SetSRID(ST_MakePoint(g.lon, g.lat), 4326)::geography,
                    m.location
                ), 0), 2)) /
                SUM(1.0 / POWER(NULLIF(ST_Distance(
                    ST_SetSRID(ST_MakePoint(g.lon, g.lat), 4326)::geography,
                    m.location
                ), 0), 2)) AS value
            FROM grid g
            CROSS JOIN measurements_filtered m
            GROUP BY g.lat, g.lon
        )
        SELECT lat, lon, value FROM idw
        WHERE value IS NOT NULL
          AND EXISTS (
              SELECT 1 FROM measurements_filtered m2
              WHERE ST_Distance(
                  ST_SetSRID(ST_MakePoint(idw.lon, idw.lat), 4326)::geography,
                  m2.location
              ) < 500
          )
        ORDER BY lat, lon
    """
        ),
        params,
    ).all()

    return {
        "points": [
            {"lat": float(r.lat), "lon": float(r.lon), "value": float(r.value)} for r in result
        ],
        "bounds": {
            "lat_min": float(bounds.lat_min),
            "lat_max": float(bounds.lat_max),
            "lon_min": float(bounds.lon_min),
            "lon_max": float(bounds.lon_max),
        },
        "parameter": parameter,
    }


def get_high_cpu_threshold() -> float:
    # Stały próg wysokiego obciążenia CPU
    return 95.0


def measurements_by_cpu_category(db: Session, threshold: float | None = None):
    """Zwróć pomiary pogrupowane: normalne, wysokie CPU"""
    if threshold is None:
        threshold = get_high_cpu_threshold()

    normal = db.query(Measurement).filter(Measurement.host_cpu < threshold).count()
    high = db.query(Measurement).filter(Measurement.host_cpu >= threshold).count()

    return {"normal": normal, "high": high, "threshold": threshold}


def get_uplink_downlink_stats(db: Session, session_id: str | None = None):
    """Statystyki oddzielnie dla UL i DL"""
    query = db.query(Measurement)
    if session_id:
        query = query.filter(Measurement.session_id == session_id)

    result = query.with_entities(
        func.avg(Measurement.dl_throughput_mbps).label("dl_throughput"),
        func.avg(Measurement.dl_latency_ms).label("dl_latency"),
        func.avg(Measurement.dl_jitter_ms).label("dl_jitter"),
        func.sum(Measurement.dl_lost_packets).label("dl_lost_packets"),
        func.avg(Measurement.ul_throughput_mbps).label("ul_throughput"),
        func.avg(Measurement.ul_latency_ms).label("ul_latency"),
        func.avg(Measurement.ul_jitter_ms).label("ul_jitter"),
        func.sum(Measurement.ul_lost_packets).label("ul_lost_packets"),
    ).first()

    if not result:
        return {
            "downlink": {
                "throughput": None,
                "latency": None,
                "jitter": None,
                "lost_packets": None,
            },
            "uplink": {
                "throughput": None,
                "latency": None,
                "jitter": None,
                "lost_packets": None,
            },
        }

    return {
        "downlink": {
            "throughput": _to_float(result.dl_throughput),
            "latency": _to_float(result.dl_latency),
            "jitter": _to_float(result.dl_jitter),
            "lost_packets": int(result.dl_lost_packets)
            if result.dl_lost_packets is not None
            else None,
        },
        "uplink": {
            "throughput": _to_float(result.ul_throughput),
            "latency": _to_float(result.ul_latency),
            "jitter": _to_float(result.ul_jitter),
            "lost_packets": int(result.ul_lost_packets)
            if result.ul_lost_packets is not None
            else None,
        },
    }
def get_route_anomaly(db: Session, lat: float, lon: float, radius_km: float = 1.2):
    """
    FILTROWANIE DLA TRAS:
    Szuka najbliższego punktu o krytycznie słabym sygnale (RSRP < -100)
    w zadanym promieniu (w kilometrach) od środka trasy technika.
    """
    # Tworzymy punkt środkowy trasy dla bazy przestrzennej
    center_geo = func.ST_SetSRID(func.ST_MakePoint(lon, lat), 4326)
    
    # Przeszukujemy bazę danych bezpośrednio na serwerze
    row = db.query(
        geo_func.ST_Y(geo_func.ST_GeomFromWKB(Measurement.location)).label("latitude"),
        geo_func.ST_X(geo_func.ST_GeomFromWKB(Measurement.location)).label("longitude"),
        Measurement.rsrp
    ).filter(
        Measurement.location.isnot(None),
        Measurement.rsrp < -100,  # Kryterium anomalii sieciowej
        geo_func.ST_Distance(Measurement.location, center_geo) <= (radius_km * 1000)  # Korytarz bezpieczeństwa w metrach
    ).order_by(
        Measurement.rsrp.asc()  # Wybieramy najgorszy sygnał w okolicy
    ).first()

    if not row:
        return None

    return {
        "latitude": _to_float(row.latitude),
        "longitude": _to_float(row.longitude),
        "rsrp": row.rsrp
    }