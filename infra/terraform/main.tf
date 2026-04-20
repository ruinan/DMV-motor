provider "google" {
  project = var.project_id
  region  = var.region
}

# ---------------------------------------------------------------
# Enable required GCP APIs
# ---------------------------------------------------------------
locals {
  services = [
    "run.googleapis.com",               # Cloud Run
    "sqladmin.googleapis.com",          # Cloud SQL
    "secretmanager.googleapis.com",     # Secret Manager
    "artifactregistry.googleapis.com",  # Docker images
    "servicenetworking.googleapis.com", # Private Service Access for Cloud SQL
    "compute.googleapis.com",           # VPC / subnet / Direct VPC egress
    "iamcredentials.googleapis.com",    # WIF token minting
    "sts.googleapis.com",               # WIF STS exchange
    "cloudresourcemanager.googleapis.com",
    "iam.googleapis.com",
  ]
}

resource "google_project_service" "apis" {
  for_each = toset(local.services)
  service  = each.value

  disable_on_destroy = false
}
