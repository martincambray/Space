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
}
