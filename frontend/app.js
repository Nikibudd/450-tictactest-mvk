// Die Adresse der Web-Konsole kommt aus config.js, nicht aus dem Code.
const backendUrl = window.TICTACTEST_CONFIG.backendUrl.replace(/\/$/, "");

const terminal = document.getElementById("terminal");
terminal.src = backendUrl + "/";
document.getElementById("terminal-link").href = backendUrl + "/";

// eine neue Verbindung startet bei der Web-Konsole einen neuen Prozess, also eine neue Partie
document.getElementById("new-game").addEventListener("click", () => {
  terminal.src = backendUrl + "/?t=" + Date.now();
  terminal.focus();
});

async function showVersion(elementId, url, read) {
  const element = document.getElementById(elementId);
  try {
    const response = await fetch(url, { cache: "no-store" });
    if (!response.ok) {
      throw new Error(response.status);
    }
    element.textContent = await read(response);
  } catch (error) {
    element.textContent = "unbekannt";
  }
}

// version.txt liefert die Web-Konsole (nginx), version.json legt der Release ins Frontend-Archiv
showVersion("backend-version", backendUrl + "/version.txt", async (r) => (await r.text()).trim());
showVersion("frontend-version", "version.json", async (r) => (await r.json()).version);
