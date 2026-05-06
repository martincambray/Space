import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CelestialBodyModel } from '../models/celestial-body.model';

@Injectable({
  providedIn: 'root',
})
export class CelestialBodyService {
  private http: HttpClient = inject(HttpClient);

  public findAll(): Observable<CelestialBodyModel[]> {
    return this.http.get<CelestialBodyModel[]>('/api/celestial-body');
  }

  public create(request: CreateCelestialBodyRequest): Observable<CelestialBodyModel> {
    return this.http.post<CelestialBodyModel>('/api/celestial-body', request);
  }

  public update(id: number, request: CreateCelestialBodyRequest): Observable<CelestialBodyModel> {
    return this.http.put<CelestialBodyModel>(`/api/celestial-body/${id}`, request);
  }

  public delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/celestial-body/${id}`);
  }
}

export interface CreateCelestialBodyRequest {
  name: string;
  mass: number | null;
  radius: number | null;
  orbitalRadius: number | null;
  refCoordX: number | null;
  refCoordY: number | null;
}
