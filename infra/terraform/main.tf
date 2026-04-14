provider "google" {
  project = var.project_id
  region  = var.region
}

# ---------------------------------------------------------------
# Enable required GCP APIs
# ---------------------------------------------------------------
locals {
  services = [
    "run.googleapis.com",            # Cloud Run
    "sqladmin.googleapis.com",       # Cloud SQL
    "secretmanager.googleapis.com",  # Secret Manager
    "artifactregistry.googleapis.com",
    "vpcaccess.googleapis.com",      # Serverless VPC Access
    "cloudresourcemanager.googleapis.com",
    "iam.googleapis.com",
  ]
}

resource "google_project_service" "apis" {
  for_each = toset(local.services)
  service  = each.value

  disable_on_destroy = false
}
