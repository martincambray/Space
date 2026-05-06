export interface SpaceObject {
  id: string;
  name: string;
  color: string;
  radius: number;
  x: number;
  y: number;
}

export interface SimulationFrame {
  timestamp: number;
  position: [number, number];
}