const CACHE_NAME = "isceb-desktop-v1-6";
const SHELL_ASSETS = [
  "./",
  "./index.html",
  "./styles.css",
  "./curriculum-data.js",
  "./app.js",
  "./manifest.webmanifest",
  "./isceb-logo.webp",
  "./isceb-icon-192.png",
  "./isceb-icon-512.png",
  "./admin-amirul.webp",
  "./admin-nafiul.webp",
  "./hero-supply-chain.jpg"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(SHELL_ASSETS)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  if (
    event.request.method !== "GET" ||
    url.pathname.includes("/downloads/") ||
    url.pathname.endsWith("/version.json") ||
    url.origin !== self.location.origin
  ) {
    return;
  }
  event.respondWith(
    fetch(event.request)
      .then((res) => {
        if (res && res.ok) {
          const copy = res.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy));
        }
        return res;
      })
      .catch(() => caches.match(event.request))
  );
});
