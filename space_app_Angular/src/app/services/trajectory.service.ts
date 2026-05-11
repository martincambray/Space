import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TrajectoryPointsModel } from '../models/trajectory-points.model';

@Injectable({
  providedIn: 'root',
})
export class TrajectoryService {
  private http = inject(HttpClient);

  /**
   * Retourne N points équidistants de la trajectoire de la mission.
   * L'orbite est calculée à la volée si absente du cache backend.
   *
   * @param missionId identifiant de la mission
   * @param n         nombre de points (défaut 500)
   */
  getPoints(missionId: number, n = 500): Observable<TrajectoryPointsModel> {
    return this.http.get<TrajectoryPointsModel>(
      `/api/trajectory/${missionId}/points`,
      { params: { n: n.toString() } }
    );
  }

  /**
   * Force le recalcul complet de la trajectoire côté backend puis retourne N points.
   * À appeler après une action perturbatrice ou la première fois pour une mission PLANNED.
   */
  computeAndGetPoints(missionId: number, n = 500): Observable<TrajectoryPointsModel> {
    return new Observable(observer => {
      this.http.post<void>(`/api/trajectory/${missionId}`, {}).subscribe({
        next: () => this.getPoints(missionId, n).subscribe(observer),
        error: err => observer.error(err),
      });
    });
  }
}
