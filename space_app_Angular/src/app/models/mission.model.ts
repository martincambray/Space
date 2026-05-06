import { MissionStatus } from './mission-status.model';

export interface MissionModel {
  id: number;
  name: string;
  status: MissionStatus;
  operatorMail: string;
  spacecraftName: string;
  typeName: string;
  departureBodyName: string;
  arrivalBodyName: string;
  departureDate: string;
  arrivalDate: string | null;
  orbitalTime: number | null;
  payload: string;
  createdAt: string;
}
