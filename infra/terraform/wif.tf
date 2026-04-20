# ---------------------------------------------------------------
# Workload Identity Federation — GitHub Actions → GCP
# Lets GitHub Actions authenticate to GCP without a long-lived JSON key.
# Attribute condition locks the provider to a single GitHub repo so that
# other repos under the same GitHub account cannot impersonate us.
# ---------------------------------------------------------------
resource "google_iam_workload_identity_pool" "github" {
  workload_identity_pool_id = "github-actions"
  display_name              = "GitHub Actions"
  description               = "OIDC federation pool for GitHub Actions"

  depends_on = [google_project_service.apis]
}

resource "google_iam_workload_identity_pool_provider" "github" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.github.workload_identity_pool_id
  workload_identity_pool_provider_id = "github"
  display_name                       = "GitHub"

  attribute_mapping = {
    "google.subject"       = "assertion.sub"
    "attribute.actor"      = "assertion.actor"
    "attribute.repository" = "assertion.repository"
  }

  # Hard lock: only OIDC tokens from this specific repo can use this provider.
  attribute_condition = "assertion.repository == \"${var.github_repo}\""

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }
}

# Service account assumed by GitHub Actions to deploy
resource "google_service_account" "deployer" {
  account_id   = "dmv-motor-deployer"
  display_name = "DMV Motor GitHub Actions Deployer"
}

# GitHub Actions from the locked repo can impersonate the deployer SA
resource "google_service_account_iam_member" "github_wif_binding" {
  service_account_id = google_service_account.deployer.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.github.name}/attribute.repository/${var.github_repo}"
}

# --- Deployer permissions (least privilege for CI/CD) ---
resource "google_project_iam_member" "deployer_run_admin" {
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${google_service_account.deployer.email}"
}

resource "google_project_iam_member" "deployer_artifact_writer" {
  project = var.project_id
  role    = "roles/artifactregistry.writer"
  member  = "serviceAccount:${google_service_account.deployer.email}"
}

# Required so that `gcloud run deploy` can attach the api runtime SA
resource "google_service_account_iam_member" "deployer_actas_api_sa" {
  service_account_id = google_service_account.api.name
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${google_service_account.deployer.email}"
}
