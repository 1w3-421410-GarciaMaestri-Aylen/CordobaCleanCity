const BACKEND_BASE_URL = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_BACKEND_URL || "").replace(/\/api$/, "");

export function resolveReportImageUrl(imageUrl: string): string {
  if (!imageUrl) {
    return imageUrl;
  }

  if (/^https?:\/\//i.test(imageUrl) || imageUrl.startsWith("data:")) {
    return imageUrl;
  }

  const normalizedPath = imageUrl.replace(/\\/g, "/");
  const uploadsPublicPath = extractUploadsPublicPath(normalizedPath);
  if (uploadsPublicPath) {
    return BACKEND_BASE_URL ? `${BACKEND_BASE_URL}${uploadsPublicPath}` : uploadsPublicPath;
  }

  if (isAbsoluteFileSystemPath(normalizedPath)) {
    const fileName = normalizedPath.split("/").pop();
    if (!fileName) {
      return normalizedPath;
    }
    const publicPath = `/uploads/${fileName}`;
    return BACKEND_BASE_URL ? `${BACKEND_BASE_URL}${publicPath}` : publicPath;
  }

  if (normalizedPath.startsWith("/")) {
    return BACKEND_BASE_URL ? `${BACKEND_BASE_URL}${normalizedPath}` : normalizedPath;
  }

  return normalizedPath;
}

function extractUploadsPublicPath(normalizedPath: string): string | null {
  if (normalizedPath.startsWith("/uploads/")) {
    return normalizedPath;
  }

  if (normalizedPath.startsWith("uploads/")) {
    return `/${normalizedPath}`;
  }

  const marker = "/uploads/";
  const markerIndex = normalizedPath.lastIndexOf(marker);
  if (markerIndex >= 0) {
    return normalizedPath.slice(markerIndex);
  }

  return null;
}

function isAbsoluteFileSystemPath(normalizedPath: string): boolean {
  return /^[A-Za-z]:\//.test(normalizedPath) || normalizedPath.startsWith("//");
}

export function buildReportImageFallbackDataUri(label = "Imagen no disponible"): string {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="640" height="480" viewBox="0 0 640 480">
      <defs>
        <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#eef7f8" />
          <stop offset="100%" stop-color="#dce9ee" />
        </linearGradient>
      </defs>
      <rect width="640" height="480" fill="url(#bg)" />
      <rect x="174" y="118" width="292" height="196" rx="20" fill="#ffffff" fill-opacity="0.84" stroke="#bdd3db" stroke-width="4" />
      <circle cx="250" cy="186" r="24" fill="#8bb1bf" />
      <path d="M210 278 L280 216 L336 262 L390 228 L430 278 Z" fill="#6f96a4" />
      <text x="320" y="364" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif" font-size="28" font-weight="700" fill="#365766">${escapeXml(
        label
      )}</text>
    </svg>
  `;

  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

function escapeXml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}
