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
}
