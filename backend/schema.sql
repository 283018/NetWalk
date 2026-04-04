CREATE TABLE measurements (
    id BIGSERIAL PRIMARY KEY,
    session_id UUID NOT NULL,
    imsi TEXT NOT NULL,
    imei TEXT,

    timestamp TIMESTAMPTZ NOT NULL,

    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,

    rsrp INTEGER,
    sinr INTEGER,
    network_type TEXT,
    cell_id TEXT,

    battery_level INTEGER,
    processor_temp DOUBLE PRECISION,
    os_version TEXT,

    throughput_mbps DOUBLE PRECISION,
    test_start_time TIMESTAMPTZ,
    test_end_time TIMESTAMPTZ
);

SELECT create_hypertable('measurements', 'timestamp');