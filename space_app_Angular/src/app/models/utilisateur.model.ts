export type Role = 'ADMIN' | 'OPERATEUR';

export interface UtilisateurModel {
  id: number;
  mail: string;
  lastname: string;
  firstname: string;
  role: Role;
}
