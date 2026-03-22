CREATE TABLE measurements (

    id BIGSERIAL,
    session_id UUID NOT NULL,
    imsi TEXT NOT NULL,                 -- ID karty SIM
    imei TEXT,                          -- ID urządzenia


    timestamp TIMESTAMPTZ NOT NULL,     -- czas z GPSa


    location_lat DOUBLE PRECISION,      -- szerokość geograficzna
    location_lon DOUBLE PRECISION,      -- długość geograficzna


    rsrp INTEGER,                       -- siła sygnału
    sinr INTEGER,                       -- jakość sygnału
    network_type TEXT,
    cell_id TEXT,                       -- ID nadajnika


    battery_level INTEGER,
    processor_temp DOUBLE PRECISION,
    os_version TEXT,


    throughput_mbps DOUBLE PRECISION,   -- test prędkości
    test_start_time TIMESTAMPTZ,
    test_end_time TIMESTAMPTZ,

    PRIMARY KEY (id, timestamp)
);


SELECT create_hypertable('measurements', 'timestamp');