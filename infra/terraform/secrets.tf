# ---------------------------------------------------------------
# Secret Manager — DB password & JDBC URL
# ---------------------------------------------------------------
resource "google_secret_manager_secret" "db_password" {
  secret_id = "dmv-motor-db-password"
  replication {
    auto {}
  }
  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = random_password.db_password.result
}

resource "google_secret_manager_secret" "db_url" {
  secret_id = "dmv-motor-db-url"
  replication {
    auto {}
  }
  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "db_url" {
  secret = google_secret_manager_secret.db_url.id
  # Cloud Run + VPC connector connects to Cloud SQL via private IP
  secret_data = "jdbc:postgresql://${google_sql_database_instance.main.private_ip_address}/${var.db_name}"
}
