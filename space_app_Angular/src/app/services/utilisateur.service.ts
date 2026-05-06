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
}
