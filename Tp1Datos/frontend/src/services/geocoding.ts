import { CORDOBA_CAPITAL_BOUNDS, isInsideCordobaCapital, type GeoPoint } from "../constants/location";

const NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org";
const SEARCH_LIMIT = 10;

interface NominatimAddress {
  city?: string;
  town?: string;
  village?: string;
  municipality?: string;
  county?: string;
  city_district?: string;
  state?: string;
  country_code?: string;
}

interface NominatimSearchResult {
  lat: string;
  lon: string;
  display_name: string;
  address?: NominatimAddress;
}

interface NominatimReverseResult {
  lat: string;
  lon: string;
  display_name: string;
  address?: NominatimAddress;
}

export interface GeocodingSuggestion {
  label: string;
  point: GeoPoint;
}

function normalizeText(value: string | undefined): string {
  return (value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

function belongsToCordobaCapital(address?: NominatimAddress): boolean {
  if (!address) {
    return false;
  }

  const country = normalizeText(address.country_code);
  if (country && country !== "ar") {
    return false;
  }

  const state = normalizeText(address.state);
  if (state && !state.includes("cordoba")) {
    return false;
  }

  const districtCandidates = [
    address.city,
    address.town,
    address.village,
    address.municipality,
    address.county,
    address.city_district
  ].map(normalizeText);

  return districtCandidates.some((item) => item.includes("cordoba"));
}

function parsePoint(lat: string, lon: string): GeoPoint | null {
  const parsedLat = Number(lat);
  const parsedLng = Number(lon);
  if (!Number.isFinite(parsedLat) || !Number.isFinite(parsedLng)) {
    return null;
  }
  return { lat: parsedLat, lng: parsedLng };
}

function isValidCordobaResult(point: GeoPoint, address?: NominatimAddress): boolean {
  return isInsideCordobaCapital(point) && belongsToCordobaCapital(address);
}

function toSuggestion(result: NominatimSearchResult): GeocodingSuggestion | null {
  const point = parsePoint(result.lat, result.lon);
  if (!point || !isValidCordobaResult(point, result.address)) {
    return null;
  }
  return {
    label: result.display_name,
    point
  };
}

function buildCordobaViewBox(): string {
  return [
    CORDOBA_CAPITAL_BOUNDS.west,
    CORDOBA_CAPITAL_BOUNDS.north,
    CORDOBA_CAPITAL_BOUNDS.east,
    CORDOBA_CAPITAL_BOUNDS.south
  ].join(",");
}

function dedupeSuggestions(suggestions: GeocodingSuggestion[]): GeocodingSuggestion[] {
  const seen = new Set<string>();
  const unique: GeocodingSuggestion[] = [];

  for (const suggestion of suggestions) {
    const key = `${suggestion.point.lat.toFixed(6)}:${suggestion.point.lng.toFixed(6)}`;
    if (!seen.has(key)) {
      seen.add(key);
      unique.push(suggestion);
    }
  }

  return unique;
}

export async function searchCordobaAddressSuggestions(
  query: string,
  signal?: AbortSignal
): Promise<GeocodingSuggestion[]> {
  const trimmed = query.trim();
  if (trimmed.length < 3) {
    return [];
  }

  const params = new URLSearchParams({
    q: `${trimmed}, Cordoba, Cordoba, Argentina`,
    format: "jsonv2",
    addressdetails: "1",
    limit: String(SEARCH_LIMIT),
    countrycodes: "ar",
    bounded: "1",
    viewbox: buildCordobaViewBox()
  });

  const response = await fetch(`${NOMINATIM_BASE_URL}/search?${params.toString()}`, {
    headers: {
      "Accept-Language": "es"
    },
    signal
  });

  if (!response.ok) {
    throw new Error("No se pudo buscar la direccion. Intenta de nuevo.");
  }

  const data = (await response.json()) as NominatimSearchResult[];
  return dedupeSuggestions(data.map(toSuggestion).filter((item): item is GeocodingSuggestion => item != null));
}

export async function reverseGeocodeCordoba(point: GeoPoint, signal?: AbortSignal): Promise<string | null> {
  if (!isInsideCordobaCapital(point)) {
    return null;
  }

  const params = new URLSearchParams({
    lat: String(point.lat),
    lon: String(point.lng),
    format: "jsonv2",
    addressdetails: "1",
    zoom: "18"
  });

  const response = await fetch(`${NOMINATIM_BASE_URL}/reverse?${params.toString()}`, {
    headers: {
      "Accept-Language": "es"
    },
    signal
  });

  if (!response.ok) {
    throw new Error("No se pudo resolver la direccion para la ubicacion elegida.");
  }

  const data = (await response.json()) as NominatimReverseResult;
  const parsedPoint = parsePoint(data.lat, data.lon);
  if (!parsedPoint || !isValidCordobaResult(parsedPoint, data.address)) {
    return null;
  }

  return data.display_name;
}
