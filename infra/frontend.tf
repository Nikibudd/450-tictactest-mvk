# Die Buckets entstehen über awscc (Cloud Control API) statt über aws_s3_bucket: Der aws-Provider liest bei
# jedem Bucket die Object-Lock-Konfiguration, und genau das verbietet eine Policy im Learner Lab (AccessDenied).
# Policy, Website-Konfiguration und Objekte verwaltet weiterhin der aws-Provider.

# Öffentlicher Bucket als Static Website. Hier liegt nur, was ohnehin jeder Browser sieht.
resource "awscc_s3_bucket" "frontend" {
  public_access_block_configuration = {
    block_public_acls       = true
    ignore_public_acls      = true
    block_public_policy     = false
    restrict_public_buckets = false
  }

  website_configuration = {
    index_document = "index.html"
  }
}

resource "aws_s3_bucket_policy" "frontend_public_read" {
  bucket = awscc_s3_bucket.frontend.bucket_name
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "PublicRead"
      Effect    = "Allow"
      Principal = "*"
      Action    = "s3:GetObject"
      Resource  = "${awscc_s3_bucket.frontend.arn}/*"
    }]
  })
}

# Nur die Infrastruktur kennt die Adresse der Instanz (Hostname der Elastic IP, nicht die IP: manche Netze blockieren reine IPs). Das Frontend-Deployment lässt diese Datei aus.
resource "aws_s3_object" "frontend_config" {
  bucket        = awscc_s3_bucket.frontend.bucket_name
  key           = "config.js"
  content_type  = "application/javascript"
  cache_control = "no-cache"
  content = templatefile("${path.module}/frontend-config.js.tftpl", {
    backend_url = "http://${aws_eip.tictactest.public_dns}"
  })
}

# Ersatz für force_destroy: leert die Buckets bei tofu destroy, sonst kann S3 sie nicht löschen
resource "terraform_data" "empty_buckets_on_destroy" {
  input = {
    buckets = [awscc_s3_bucket.frontend.bucket_name, awscc_s3_bucket.artifacts.bucket_name]
    profile = var.aws_profile
    region  = var.region
  }

  provisioner "local-exec" {
    when    = destroy
    command = "for b in ${join(" ", self.input.buckets)}; do aws s3 rm \"s3://$b\" --recursive --profile ${self.input.profile} --region ${self.input.region}; done"
  }
}
