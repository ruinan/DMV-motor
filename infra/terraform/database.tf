# ---------------------------------------------------------------
# Cloud SQL — PostgreSQL 16
# ---------------------------------------------------------------
resource "google_sql_database_instance" "main" {
  name             = "dmv-motor-pg"
  database_version = "POSTGRES_16"
  region           = var.region

  settings {
    tier              = var.db_tier
    availability_type = "ZONAL" # single-zone; change to REGIONAL for HA

    backup_configuration {
      enabled            = true
      start_time         = "03:00" # 3 AM UTC = 7 PM PST
      binary_log_enabled = false   # not applicable for Postgres
      transaction_log_retention_days = 7
      backup_retention_settings {
        retained_backups = 7
      }
    }

    ip_configuration {
      ipv4_enabled    = false        # private IP only
      private_network = google_compute_network.vpc.id
    }

    insights_config {
      query_insights_enabled = true
    }

    database_flags {
      name  = "max_connections"
      value = "100"
    }
  }

  deletion_protection = true

  depends_on = [
    google_service_networking_connection.private_vpc_connection,
    google_project_service.apis,
  ]
}

resource "google_sql_database" "app" {
  name     = var.db_name
  instance = google_sql_database_instance.main.name
}

resource "google_sql_user" "app" {
  name     = var.db_user
  instance = google_sql_database_instance.main.name
  password = random_password.db_password.result
}

resource "random_password" "db_password" {
  length  = 32
  special = false # avoid shell-quoting issues
}
