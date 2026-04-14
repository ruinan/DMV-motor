terraform {
  required_version = ">= 1.7"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }

  # Remote state: GCS bucket (created manually once, then checked in)
  # bootstrap: gcloud storage buckets create gs://dmv-motor-tfstate --location=us-west1
  backend "gcs" {
    bucket = "dmv-motor-tfstate"
    prefix = "prod"
  }
}
