// Konfiguration pro Umgebung. Lokal zeigt sie auf die Web-Konsole auf localhost.
// In der Cloud schreibt OpenTofu diese Datei in den Frontend-Bucket; das Frontend-Deployment lädt sie nie hoch.
window.TICTACTEST_CONFIG = {
  backendUrl: "http://localhost:8080",
};
