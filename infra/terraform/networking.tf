# ---------------------------------------------------------------
# VPC for private Cloud SQL connectivity + Cloud Run Direct VPC egress
# No VPC Connector — Cloud Run v2 Direct VPC egress is free
# ---------------------------------------------------------------
resource "google_compute_network" "vpc" {
  name                    = "dmv-motor-vpc"
  auto_create_subnetworks = false
}

# Subnet used by Cloud Run Direct VPC egress
resource "google_compute_subnetwork" "cloudrun" {
  name                     = "dmv-motor-cloudrun-subnet"
  network                  = google_compute_network.vpc.id
  region                   = var.region
  ip_cidr_range            = "10.100.0.0/24"
  private_ip_google_access = true
}

# Reserved IP range for private service access (Cloud SQL private IP)
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
