export interface TrajectoryLogsModel {
  id: number;
  bodyName: string;
  computedAt: string;
  altitude: number;
  initialSpeed: number;
  mass: number;
  orbitalSpeed: number;
  orbitalPeriod: number;
  orbitalRadius: number;
  points: number[][];
}
