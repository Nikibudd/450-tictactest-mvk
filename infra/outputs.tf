output "frontend_url" {
  description = "Hier wird gespielt"
  value       = awscc_s3_bucket.frontend.website_url
}

output "backend_url" {
  description = "Web-Konsole (nginx + ttyd)"
  value       = "http://${aws_eip.tictactest.public_dns}"
}

output "version_url" {
  description = "Laufende Backend-Version"
  value       = "http://${aws_eip.tictactest.public_dns}/version.txt"
}

output "public_ip" {
  value = aws_eip.tictactest.public_ip
}

output "region" {
  value = var.region
}

output "instance_id" {
  value = aws_instance.tictactest.id
}

output "frontend_bucket" {
  value = awscc_s3_bucket.frontend.bucket_name
}

output "artifact_bucket" {
  value = awscc_s3_bucket.artifacts.bucket_name
}

output "ssh_command" {
  value = "ssh -i ~/.ssh/tictactest_ec2 ec2-user@${aws_eip.tictactest.public_dns}"
}
