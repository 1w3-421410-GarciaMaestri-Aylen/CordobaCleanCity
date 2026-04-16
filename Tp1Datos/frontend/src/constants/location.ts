export interface GeoPoint {
  lat: number;
  lng: number;
}

export interface GeoBounds {
  south: number;
  west: number;
  north: number;
  east: number;
}

export const CORDOBA_CAPITAL_CENTER: GeoPoint = {
  lat: -31.420083,
  lng: -64.188776
};

export const CORDOBA_CAPITAL_BOUNDS: GeoBounds = {
  south: -31.56,
  west: -64.33,
  north: -31.30,
  east: -64.03
};

export function isInsideCordobaCapital(point: GeoPoint): boolean {
  return (
    point.lat >= CORDOBA_CAPITAL_BOUNDS.south &&
    point.lat <= CORDOBA_CAPITAL_BOUNDS.north &&
    point.lng >= CORDOBA_CAPITAL_BOUNDS.west &&
    point.lng <= CORDOBA_CAPITAL_BOUNDS.east
  );
}
