import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface AuthRequestDto {
  mail: string;
  password: string;
}

export interface TokenResponseDto {
  token: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http: HttpClient = inject(HttpClient);
  private _token: string = sessionStorage.getItem('token') ?? '';

  public get token(): string {
    return this._token;
  }

  public set token(value: string) {
    this._token = value;
    sessionStorage.setItem('token', value);
  }

  public auth(request: AuthRequestDto): Observable<TokenResponseDto> {
    return this.http.post<TokenResponseDto>('/api/auth', request);
  }

  public isLogged(): boolean {
    return !!this._token;
  }

  public resetAuth(): void {
    this._token = '';
    sessionStorage.removeItem('token');
  }
}
