// PROMASTER Download Page Client Logic
document.addEventListener("DOMContentLoaded", async () => {
  // Elements
  const btnDownload = document.getElementById("btn-download-apk");
  const btnApkSize = document.getElementById("btn-apk-size");
  const navVersion = document.getElementById("nav-version");
  const valVersion = document.getElementById("val-version");
  const valVersionCode = document.getElementById("val-versioncode");
  const valSize = document.getElementById("val-size");
  const valDate = document.getElementById("val-date");
  const valAndroid = document.getElementById("val-android");
  const tableAppName = document.getElementById("table-appname");
  const tablePackage = document.getElementById("table-package");
  const tableVersion = document.getElementById("table-version");
  const navSourceLink = document.getElementById("nav-source-link");
  const btnViewSource = document.getElementById("btn-view-source");

  // Determine repository context from hostname/pathname if on GitHub Pages
  let repoOwner = "MANI2389";
  let repoName = "promaster";

  const host = window.location.hostname;
  if (host.endsWith(".github.io")) {
    repoOwner = host.split(".github.io")[0];
    const pathParts = window.location.pathname.split("/").filter(Boolean);
    if (pathParts.length > 0) {
      repoName = pathParts[0];
    }
  }

  // Update source links
  const githubRepoUrl = `https://github.com/${repoOwner}/${repoName}`;
  if (navSourceLink) navSourceLink.href = githubRepoUrl;
  if (btnViewSource) btnViewSource.href = githubRepoUrl;

  // Load release-info.json
  try {
    const res = await fetch("./release-info.json");
    if (res.ok) {
      const data = await res.json();
      
      // Update Version displays
      const vName = data.versionName || "1.0";
      const vCode = data.versionCode || 1;
      const vSize = data.apkSizeFormatted || "16.5 MB";
      const vDate = data.releaseDate || "Sept 2026";
      const vPackage = data.packageName || "com.example.promaster";
      const apkName = data.apkFileName || `PROMASTER-v${vName}.apk`;

      if (navVersion) navVersion.textContent = `v${vName}`;
      if (valVersion) valVersion.textContent = `v${vName}`;
      if (valVersionCode) valVersionCode.textContent = vCode;
      if (valSize) valSize.textContent = vSize;
      if (valDate) valDate.textContent = vDate;
      if (valAndroid && data.minAndroidVersion) valAndroid.textContent = data.minAndroidVersion;
      if (btnApkSize) btnApkSize.textContent = `v${vName} (${vSize})`;

      if (tableAppName && data.appName) tableAppName.textContent = data.appName;
      if (tablePackage) tablePackage.innerHTML = `<code>${vPackage}</code>`;
      if (tableVersion) tableVersion.textContent = `${vName} (Version Code ${vCode})`;

      // Dynamic Direct Download URL pointing to actual GitHub Release asset
      const releaseDownloadUrl = `${githubRepoUrl}/releases/download/v${vName}/${apkName}`;
      const latestDownloadUrl = `${githubRepoUrl}/releases/latest/download/${apkName}`;
      
      if (btnDownload) {
        btnDownload.href = latestDownloadUrl;
        btnDownload.setAttribute("download", apkName);
      }
    }
  } catch (err) {
    console.info("Loaded with static fallback values:", err);
  }

  // Micro-interaction on download button
  if (btnDownload) {
    btnDownload.addEventListener("click", () => {
      btnDownload.style.transform = "scale(0.98)";
      setTimeout(() => {
        btnDownload.style.transform = "";
      }, 150);
    });
  }
});
