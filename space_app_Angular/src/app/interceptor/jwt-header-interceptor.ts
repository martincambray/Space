import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const jwtHeaderInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.token) {
    return next(req);
  }

  if (req.url.endsWith('/api/auth')) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      'Authorization': `Bearer ${authService.token}`
    }
  });

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403) {
        authService.resetAuth();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
