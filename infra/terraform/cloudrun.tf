# ---------------------------------------------------------------
# Cloud Run — API service
# ---------------------------------------------------------------
resource "google_cloud_run_v2_service" "api" {
  name     = "dmv-motor-api"
  location = var.region

  template {
    service_account = google_service_account.api.email

    scaling {
      min_instance_count = var.api_min_instances
      max_instance_count = var.api_max_instances
    }

    # Direct VPC egress (free; replaces paid VPC Connector)
    vpc_access {
      network_interfaces {
        network    = google_compute_network.vpc.id
        subnetwork = google_compute_subnetwork.cloudrun.id
      }
      egress = "PRIVATE_RANGES_ONLY"
    }

    containers {
      # First apply uses placeholder; CI/CD deploys real image afterwards.
      # lifecycle.ignore_changes below prevents TF from reverting it.
      image = var.image_tag == "bootstrap" ? "us-docker.pkg.dev/cloudrun/container/hello" : "${local.image_base}:${var.image_tag}"

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
        cpu_idle = true # only allocate CPU during requests (scale-to-zero friendly)
      }

      ports {
        container_port = 8080
      }

      # Spring Boot config via env vars
      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }

      env {
        name = "SPRING_DATASOURCE_URL"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_url.secret_id
            version = "latest"
          }
        }
      }

      env {
        name  = "SPRING_DATASOURCE_USERNAME"
        value = var.db_user
      }

      env {
        name = "SPRING_DATASOURCE_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_password.secret_id
            version = "latest"
          }
        }
      }

      # JVM tuning for constrained memory
      env {
        name  = "JAVA_TOOL_OPTIONS"
        value = "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
      }

      startup_probe {
        http_get {
          path = "/actuator/health"
        }
        initial_delay_seconds = 10
        period_seconds        = 5
        failure_threshold     = 12 # 60s startup budget
      }

      liveness_probe {
        http_get {
          path = "/actuator/health/liveness"
        }
        period_seconds    = 30
        failure_threshold = 3
      }
    }
  }

  depends_on = [
    google_project_service.apis,
    google_secret_manager_secret_version.db_url,
    google_secret_manager_secret_version.db_password,
    google_compute_subnetwork.cloudrun,
  ]

  lifecycle {
    # CI/CD (gcloud run deploy --image) manages the image tag.
    # Without this, every `terraform apply` would drift the image back.
    ignore_changes = [
      template[0].containers[0].image,
      client,
      client_version,
    ]
  }
}
