# Copyright (c) CARIAD SE, A VOLKSWAGEN GROUP COMPANY

terraform {
  required_providers {
    keycloak = {
      source  = "mrparkers/keycloak"
      version = ">= 4.2.0"
    }
  }

  required_version = ">= 1.4.0"
}

