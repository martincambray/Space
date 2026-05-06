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
}
