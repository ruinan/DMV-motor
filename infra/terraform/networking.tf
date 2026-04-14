# ---------------------------------------------------------------
# VPC for private Cloud SQL connectivity
# ---------------------------------------------------------------
resource "google_compute_network" "vpc" {
  name                    = "dmv-motor-vpc"
  auto_create_subnetworks = false
}

resource "google_compute_global_address" "private_ip_range" {
  name          = "dmv-motor-private-ip"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = google_compute_network.vpc.id
}

resource "google_service_networking_connection" "private_vpc_connection" {
  network                 = google_compute_network.vpc.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip_range.name]
}

# Serverless VPC Access connector — lets Cloud Run reach private Cloud SQL IP
resource "google_vpc_access_connector" "connector" {
  name          = "dmv-motor-connector"
  region        = var.region
  network       = google_compute_network.vpc.name
  ip_cidr_range = "10.8.0.0/28"

  depends_on = [google_project_service.apis]
}
