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
}
