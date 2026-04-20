variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "region" {
  description = "GCP region"
  type        = string
  default     = "us-west1"
}

variable "db_tier" {
  description = "Cloud SQL machine tier"
  type        = string
  default     = "db-f1-micro" # cheapest; upgrade when needed
}

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "dmv_motor"
}

variable "db_user" {
  description = "PostgreSQL application user"
  type        = string
  default     = "dmv_motor"
}

variable "image_tag" {
  description = "Docker image tag to deploy. Use 'bootstrap' on first apply to use a placeholder hello-container (since Artifact Registry has no image yet). After first CI/CD deploy, lifecycle.ignore_changes keeps TF from reverting the image."
  type        = string
  default     = "bootstrap"
}

variable "github_repo" {
  description = "GitHub repo in 'owner/name' form, used to scope Workload Identity Federation"
  type        = string
}

variable "api_min_instances" {
  description = "Minimum Cloud Run instances (0 = scale to zero)"
  type        = number
  default     = 0
}

variable "api_max_instances" {
  description = "Maximum Cloud Run instances"
  type        = number
  default     = 3
}
