# ---------------------------------------------------------------
# Artifact Registry — Docker repository
# ---------------------------------------------------------------
resource "google_artifact_registry_repository" "api" {
  location      = var.region
  repository_id = "dmv-motor"
  format        = "DOCKER"
  description   = "DMV Motor API Docker images"

  depends_on = [google_project_service.apis]
}

locals {
  image_base = "${var.region}-docker.pkg.dev/${var.project_id}/dmv-motor/api"
}
