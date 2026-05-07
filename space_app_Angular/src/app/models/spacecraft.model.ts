export interface SpacecraftModel {
  id: number;
  name: string;
  description: string;
  type: string;
  batteryMax: number;
  fuelCapacity: number;
  available: boolean;
}
