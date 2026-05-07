export interface CelestialBodyModel {
  id: number;
  name: string;
  mass: number | null;
  radius: number | null;
  orbitalRadius: number | null;
  refCoordX: number | null;
  refCoordY: number | null;
  image: string | null;
}
