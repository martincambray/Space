import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SpacecraftModel } from '../models/spacecraft.model';

@Injectable({
  providedIn: 'root',
})
export class SpacecraftService {
  private http: HttpClient = inject(HttpClient);

  public findAll(): Observable<SpacecraftModel[]> {
    return this.http.get<SpacecraftModel[]>('/api/spacecraft');
  }

  public create(request: CreateSpacecraftRequest): Observable<SpacecraftModel> {
    return this.http.post<SpacecraftModel>('/api/spacecraft', request);
  }

  public update(id: number, request: CreateSpacecraftRequest): Observable<SpacecraftModel> {
    return this.http.put<SpacecraftModel>(`/api/spacecraft/${id}`, request);
  }

  public delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/spacecraft/${id}`);
  }
}

export interface CreateSpacecraftRequest {
  name: string;
  description: string;
  batteryMax: number;
  fuelCapacity: number;
}
