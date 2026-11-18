terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    # nur für S3-Buckets, siehe frontend.tf
    awscc = {
      source  = "hashicorp/awscc"
      version = "~> 1.0"
    }
  }
}

# Eigenes Profil statt "default", damit nie versehentlich gegen ein anderes AWS-Konto gearbeitet wird.
provider "aws" {
  region  = var.region
  profile = var.aws_profile
}

provider "awscc" {
  region  = var.region
  profile = var.aws_profile
}
