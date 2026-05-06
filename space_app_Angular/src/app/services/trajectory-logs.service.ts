import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TrajectoryLogsModel } from '../models/trajectory-logs.model';

@Injectable({
  providedIn: 'root',
})
export class TrajectoryLogsService {
  private http: HttpClient = inject(HttpClient);

  public findByMission(missionId: number): Observable<TrajectoryLogsModel[]> {
    return this.http.get<TrajectoryLogsModel[]>(`/api/trajectory-log/mission/${missionId}`);
  }
}
