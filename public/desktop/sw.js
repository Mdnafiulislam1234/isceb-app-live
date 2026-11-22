const SW_BUILD_ID = "__ISCEB_SW_BUILD__";
const CACHE_NAME = `isceb-pwa-v1-${SW_BUILD_ID}`;
const SHELL_ASSETS = [
  "./",
  "./index.html",
  "./styles.css",
  "./curriculum-data.js",
  "./desktop-auth.js",
  "./app.js",
  "./manifest.webmanifest",
  "./isceb-logo.webp",
  "./isceb-icon-192.png",
  "./isceb-icon-512.png",
  "./admin-amirul.webp",
  "./admin-nafiul.webp",
  "./member-hasina.webp",
  "./member-masudur.webp",
  "./member-asif.webp",
  "./member-mashiur.webp",
  "./member-shajibul.webp",
  "./member-tonmoy.webp",
  "./hero-supply-chain.jpg"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches
      .open(CACHE_NAME)
      .then((cache) =>
        Promise.all(
          SHELL_ASSETS.map((asset) =>
            cache.add(new Request(asset, { cache: "reload" }))
          )
        )
      )
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((k) => k.startsWith("isceb-") && k !== CACHE_NAME)
            .map((k) => caches.delete(k))
        )
      )
      .then(() => self.clients.claim())
      .then(() =>
        self.clients.matchAll({ type: "window", includeUncontrolled: true })
      )
      .then((clients) => {
        for (const client of clients) {
          client.postMessage({
            type: "ISCEB_PWA_UPDATED",
            buildId: SW_BUILD_ID,
            cacheName: CACHE_NAME
          });
        }
      })
  );
});

self.addEventListener("message", (event) => {
  if (event.data && event.data.type === "SKIP_WAITING") {
    self.skipWaiting();
  }
});

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  if (
    event.request.method !== "GET" ||
    url.pathname.includes("/downloads/") ||
    url.pathname.endsWith("/version.json") ||
    url.pathname.endsWith("/pwa-version.json") ||
    url.origin !== self.location.origin
  ) {
    return;
  }

  const isNavigationOrCode =
    event.request.mode === "navigate" ||
    url.pathname.endsWith(".html") ||
    url.pathname.endsWith(".js") ||
    url.pathname.endsWith(".css") ||
    url.pathname.endsWith(".webmanifest") ||
    url.pathname.endsWith("/");

  event.respondWith(
    fetch(event.request, isNavigationOrCode ? { cache: "no-store" } : undefined)
      .then((res) => {
        if (res && res.ok) {
          const copy = res.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy));
        }
        return res;
      })
      .catch(async () => {
        const cached = await caches.match(event.request);
        if (cached) return cached;
        if (event.request.mode === "navigate") {
          return caches.match("./index.html");
        }
        throw new Error("Offline and not cached");
      })
  );
});
