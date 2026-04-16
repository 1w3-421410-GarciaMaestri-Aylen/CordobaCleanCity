import type { LatLngBoundsExpression, LatLngExpression } from "leaflet";
import { CORDOBA_CAPITAL_BOUNDS, CORDOBA_CAPITAL_CENTER } from "./location";

export const CORDOBA_MAP_CENTER: LatLngExpression = [CORDOBA_CAPITAL_CENTER.lat, CORDOBA_CAPITAL_CENTER.lng];

export const CORDOBA_MAP_BOUNDS: LatLngBoundsExpression = [
  [CORDOBA_CAPITAL_BOUNDS.south, CORDOBA_CAPITAL_BOUNDS.west],
  [CORDOBA_CAPITAL_BOUNDS.north, CORDOBA_CAPITAL_BOUNDS.east]
];

export const CORDOBA_DEFAULT_ZOOM = 13;
export const CORDOBA_SELECTED_POINT_ZOOM = 15;
export const CORDOBA_MIN_ZOOM = 12;
export const CORDOBA_MAX_ZOOM = 18;
