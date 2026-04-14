output "api_url" {
  description = "Public URL of the Cloud Run API"
  value       = google_cloud_run_v2_service.api.uri
}

output "db_instance_name" {
  description = "Cloud SQL instance connection name"
  value       = google_sql_database_instance.main.connection_name
}

output "db_private_ip" {
  description = "Cloud SQL private IP (reachable from VPC)"
  value       = google_sql_database_instance.main.private_ip_address
  sensitive   = true
}

output "artifact_registry_url" {
  description = "Artifact Registry base URL for Docker images"
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/dmv-motor"
}
