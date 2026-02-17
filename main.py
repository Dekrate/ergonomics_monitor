from testcontainers.postgres import PostgresContainer
import psycopg2


def test_postgres_container():
    with PostgresContainer("postgres:15") as postgres:
        conn = psycopg2.connect(
            host=postgres.get_container_host_ip(),
            port=postgres.get_exposed_port(5432),
            dbname=postgres.dbname,
            user=postgres.username,
            password=postgres.password,
        )

        cur = conn.cursor()
        cur.execute("SELECT 1;")
        assert cur.fetchone()[0] == 1

        cur.close()
        conn.close()
