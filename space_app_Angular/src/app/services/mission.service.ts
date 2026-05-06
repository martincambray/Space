import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MissionModel } from '../models/mission.model';

@Injectable({
  providedIn: 'root',
})
export class MissionService {
  private http: HttpClient = inject(HttpClient);

  public findAll(): Observable<MissionModel[]> {
    return this.http.get<MissionModel[]>('/api/mission');
  }

  public findById(id: number): Observable<MissionModel> {
    return this.http.get<MissionModel>(`/api/mission/${id}`);
  }

  public create(request: CreateMissionRequest): Observable<MissionModel> {
    return this.http.post<MissionModel>('/api/mission', request);
  }

  public updateStatus(id: number, status: string): Observable<MissionModel> {
    return this.http.patch<MissionModel>(`/api/mission/${id}/status`, { status });
  }

  public delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/mission/${id}`);
  }
}

export interface CreateMissionRequest {
  name: string;
  spacecraftId: number;
  typeId: number;
  departureBodyId: number;
  arrivalBodyId: number;
  departureDate: string;
}
