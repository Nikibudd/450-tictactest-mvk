# Fester Image-Name statt "most_recent": ein neues AMI würde sonst die Instanz ersetzen
data "aws_ami" "amazon_linux" {
  owners = ["amazon"]

  filter {
    name   = "name"
    values = [var.ami_name]
  }
}

# Im Learner Lab sind eigene IAM-Rollen verboten; die Instanz nutzt das vorhandene Profil (LabRole)
data "aws_iam_instance_profile" "lab" {
  name = "LabInstanceProfile"
}

resource "aws_key_pair" "tictactest" {
  key_name   = "tictactest"
  public_key = file(pathexpand(var.public_key_path))
}

resource "aws_security_group" "tictactest" {
  name        = "tictactest-web"
  description = "TicTacTest: Web-Konsole fuer alle, SSH nur fuer Admins"

  ingress {
    description = "HTTP: nginx vor der Web-Konsole und /version.txt"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH fuer Fehlersuche, nur von der eigenen IP"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.ssh_allowed_cidr]
  }

  egress {
    description = "Pakete, ttyd-Download, S3 und SSM"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "tictactest" {
  ami                         = data.aws_ami.amazon_linux.id
  instance_type               = var.instance_type
  key_name                    = aws_key_pair.tictactest.key_name
  vpc_security_group_ids      = [aws_security_group.tictactest.id]
  iam_instance_profile        = data.aws_iam_instance_profile.lab.name
  associate_public_ip_address = true

  # Laufzeitumgebung und Dienst; welche Version läuft, bestimmt erst das Deployment
  user_data = templatefile("${path.module}/user-data.sh.tftpl", {
    corretto_version = var.corretto_version
    nginx_version    = var.nginx_version
    ttyd_version     = var.ttyd_version
    ttyd_sha256      = var.ttyd_sha256
    service_unit     = file("${path.module}/../deploy/tictactest.service")
    nginx_conf       = file("${path.module}/../deploy/tictactest.nginx.conf")
  })
  # user_data läuft nur beim ersten Start; eine geänderte Einrichtung braucht deshalb eine neue Instanz
  user_data_replace_on_change = true

  metadata_options {
    http_tokens = "required"
  }

  tags = {
    Name = "tictactest"
  }
}

# Feste Adresse, die das Stoppen der Instanz am Ende einer Learner-Lab-Session überlebt
resource "aws_eip" "tictactest" {
  instance = aws_instance.tictactest.id
  domain   = "vpc"

  tags = {
    Name = "tictactest"
  }
}
