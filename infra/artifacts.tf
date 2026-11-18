# Privater Bucket für die Jars aus den Releases. Die Instanz liest ihn über das Instance Profile.
# Die Jars lassen sich jederzeit wieder aus dem GitHub-Release kopieren.
resource "awscc_s3_bucket" "artifacts" {
  public_access_block_configuration = {
    block_public_acls       = true
    ignore_public_acls      = true
    block_public_policy     = true
    restrict_public_buckets = true
  }
}
