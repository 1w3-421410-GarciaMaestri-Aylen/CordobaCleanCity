export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
let accessTokenProvider: (() => string | null) | null = null;
let unauthorizedHandler: (() => void) | null = null;

export function configureHttpClient(options: {
  getAccessToken?: () => string | null;
  onUnauthorized?: () => void;
}): void {
  accessTokenProvider = options.getAccessToken ?? null;
  unauthorizedHandler = options.onUnauthorized ?? null;
}

function buildUrl(path: string): string {
  return `${API_BASE_URL}${path}`;
}

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string; error?: string };
    return body.message || body.error || `${response.status} ${response.statusText}`;
  } catch {
    return `${response.status} ${response.statusText}`;
  }
}

export async function httpRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers ?? {});
  const body = init?.body;
  const token = accessTokenProvider?.() ?? null;

  if (!(body instanceof FormData) && body != null && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(buildUrl(path), {
    ...init,
    headers
  });

  if (response.status === 401 && token && unauthorizedHandler) {
    unauthorizedHandler();
  }

  if (!response.ok) {
    const message = await readErrorMessage(response);
    throw new ApiError(message, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
