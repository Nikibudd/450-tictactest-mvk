variable "aws_profile" {
  description = "AWS-CLI-Profil mit den Learner-Lab-Credentials (~/.aws/credentials)"
  type        = string
  default     = "learnerlab"
}

variable "region" {
  description = "AWS-Region; das Learner Lab erlaubt nur us-east-1 und us-west-2"
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "EC2-Instanztyp"
  type        = string
  default     = "t2.micro"
}

variable "ami_name" {
  description = "Exakter Name des Amazon-Linux-2023-Images. Fixiert zusammen mit dem Paket-Repository die installierten Versionen."
  type        = string
  default     = "al2023-ami-2023.12.20261117.0-kernel-6.18-x86_64"
}

variable "corretto_version" {
  description = "Version des Pakets java-25-amazon-corretto-headless (epoch:version-release)"
  type        = string
  default     = "1:25.0.5.1.2-1.amzn2023.1"
}

variable "nginx_version" {
  description = "Version des Pakets nginx (epoch:version-release)"
  type        = string
  default     = "1:1.30.6-1.amzn2023.0.1"
}

variable "ttyd_version" {
  description = "ttyd-Release auf GitHub"
  type        = string
  default     = "1.7.7"
}

variable "ttyd_sha256" {
  description = "SHA-256 von ttyd.x86_64 aus dem ttyd-Release (SHA256SUMS)"
  type        = string
  default     = "8a217c968aba172e0dbf3f34447218dc015bc4d5e59bf51db2f2cd12b7be4f55"
}

variable "public_key_path" {
  description = "Pfad zum öffentlichen SSH-Schlüssel (liegt ausserhalb des Repos)"
  type        = string
  default     = "~/.ssh/tictactest_ec2.pub"
}

variable "ssh_allowed_cidr" {
  description = "IP-Bereich, der per SSH auf Port 22 zugreifen darf, z. B. die eigene IP als x.x.x.x/32"
  type        = string
}
