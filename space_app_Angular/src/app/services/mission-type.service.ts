import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MissionTypeModel } from '../models/mission-type.model';

@Injectable({
  providedIn: 'root',
})
export class MissionTypeService {
  private http: HttpClient = inject(HttpClient);

  public findAll(): Observable<MissionTypeModel[]> {
    return this.http.get<MissionTypeModel[]>('/api/mission-type');
  }

  public create(request: CreateMissionTypeRequest): Observable<MissionTypeModel> {
    return this.http.post<MissionTypeModel>('/api/mission-type', request);
  }

  public update(id: number, request: CreateMissionTypeRequest): Observable<MissionTypeModel> {
    return this.http.put<MissionTypeModel>(`/api/mission-type/${id}`, request);
  }

  public delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/mission-type/${id}`);
  }
}

export interface CreateMissionTypeRequest {
  name: string;
  description: string;
}
