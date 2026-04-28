from sqlalchemy import text


def test_health_endpoint(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_database_connection(test_db):
    result = test_db.execute(text("SELECT 1"))
    assert result.scalar() == 1


def test_measurements_table_exists(test_db):
    result = test_db.execute(
        text("""
        SELECT EXISTS (
            SELECT FROM information_schema.tables
            WHERE table_name = 'measurements'
        )
    """)
    )
    assert result.scalar() is True


def test_postgis_extension(test_db):
    result = test_db.execute(
        text("""
        SELECT EXISTS (
            SELECT FROM pg_extension WHERE extname = 'postgis'
        )
    """)
    )
    assert result.scalar() is True
