import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UtilisateurModel } from '../models/utilisateur.model';

@Injectable({
  providedIn: 'root',
})
export class UtilisateurService {
  private http: HttpClient = inject(HttpClient);

  public findAll(): Observable<UtilisateurModel[]> {
    return this.http.get<UtilisateurModel[]>('/api/utilisateur');
  }

  public findMe(): Observable<UtilisateurModel> {
    return this.http.get<UtilisateurModel>('/api/utilisateur/me');
  }

  public updatePassword(password: string): Observable<void> {
    return this.http.patch<void>('/api/utilisateur/me', { password });
  }

  public create(request: CreateUtilisateurRequest): Observable<UtilisateurModel> {
    return this.http.post<UtilisateurModel>('/api/utilisateur', request);
  }

  public update(id: number, request: UpdateUtilisateurRequest): Observable<void> {
    return this.http.put<void>(`/api/utilisateur/${id}`, request);
  }

  public delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/utilisateur/${id}`);
  }
}

export interface CreateUtilisateurRequest {
  mail: string;
  password: string;
  lastname: string;
  firstname: string;
  role: string;
}

export interface UpdateUtilisateurRequest {
  mail?: string;
  password?: string;
  lastname?: string;
  firstname?: string;
  role?: string;
}
