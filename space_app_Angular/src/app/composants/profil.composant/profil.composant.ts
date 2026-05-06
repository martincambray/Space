import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UtilisateurService, CreateUtilisateurRequest, UpdateUtilisateurRequest } from '../../services/utilisateur.service';
import { UtilisateurModel } from '../../models/utilisateur.model';

@Component({
  selector: 'app-profil',
  imports: [ReactiveFormsModule],
  templateUrl: './profil.composant.html',
  styleUrl: './profil.composant.css',
})
export class ProfilComposant implements OnInit {
  private utilisateurService = inject(UtilisateurService);
  private formBuilder        = inject(FormBuilder);
  private router             = inject(Router);

  protected user    = signal<UtilisateurModel | null>(null);
  protected users   = signal<UtilisateurModel[]>([]);
  protected isAdmin = computed(() => this.user()?.role === 'ADMIN');

  protected success = signal(false);
  protected error   = signal('');

  // ── Changer mot de passe (soi-même) ──
  protected form!:         FormGroup;
  protected passwordCtrl!: FormControl;
  protected confirmCtrl!:  FormControl;

  // ── Modale gestion utilisateurs (create/edit) ──
  protected showUserModal  = signal(false);
  protected editingUserId  = signal<number | null>(null);
  protected userModalError = signal('');

  protected userForm!:   FormGroup;
  protected uMailCtrl!:  FormControl;
  protected uPassCtrl!:  FormControl;
  protected uLastCtrl!:  FormControl;
  protected uFirstCtrl!: FormControl;
  protected uRoleCtrl!:  FormControl;

  ngOnInit(): void {
    this.utilisateurService.findMe().subscribe({
      next: u => {
        this.user.set(u);
        if (u.role === 'ADMIN') this.loadUsers();
      },
      error: () => this.router.navigate(['/login'])
    });

    this.passwordCtrl = this.formBuilder.control('', [Validators.required, Validators.minLength(8)]);
    this.confirmCtrl  = this.formBuilder.control('', [Validators.required]);
    this.form = this.formBuilder.group(
      { password: this.passwordCtrl, confirm: this.confirmCtrl },
      { validators: this.passwordsMatch }
    );

    this.uMailCtrl  = this.formBuilder.control('', [Validators.required, Validators.email]);
    this.uPassCtrl  = this.formBuilder.control('');          // optionnel à l'édition
    this.uLastCtrl  = this.formBuilder.control('', Validators.required);
    this.uFirstCtrl = this.formBuilder.control('', Validators.required);
    this.uRoleCtrl  = this.formBuilder.control('OPERATEUR', Validators.required);
    this.userForm = this.formBuilder.group({
      mail:      this.uMailCtrl,
      password:  this.uPassCtrl,
      lastname:  this.uLastCtrl,
      firstname: this.uFirstCtrl,
      role:      this.uRoleCtrl,
    });
  }

  private loadUsers(): void {
    this.utilisateurService.findAll().subscribe({ next: list => this.users.set(list) });
  }

  private passwordsMatch(g: FormGroup) {
    return g.get('password')?.value === g.get('confirm')?.value ? null : { mismatch: true };
  }

  protected submit(): void {
    if (this.form.invalid) return;
    this.success.set(false);
    this.error.set('');
    this.utilisateurService.updatePassword(this.passwordCtrl.value).subscribe({
      next: () => { this.success.set(true); this.form.reset(); },
      error: () => this.error.set('Erreur lors de la mise à jour du mot de passe.')
    });
  }

  protected openCreateUser(): void {
    this.editingUserId.set(null);
    this.userForm.reset({ role: 'OPERATEUR' });
    // En mode création, le mot de passe est obligatoire
    this.uPassCtrl.setValidators([Validators.required, Validators.minLength(8)]);
    this.uPassCtrl.updateValueAndValidity();
    this.userModalError.set('');
    this.showUserModal.set(true);
  }

  protected openEditUser(u: UtilisateurModel): void {
    this.editingUserId.set(u.id);
    this.userForm.patchValue({
      mail:      u.mail,
      lastname:  u.lastname,
      firstname: u.firstname,
      role:      u.role,
      password:  '',
    });
    // En mode édition, le mot de passe est optionnel
    this.uPassCtrl.clearValidators();
    this.uPassCtrl.updateValueAndValidity();
    this.userModalError.set('');
    this.showUserModal.set(true);
  }

  protected closeUserModal(): void {
    this.showUserModal.set(false);
  }

  protected submitUser(): void {
    if (this.userForm.invalid) return;
    this.userModalError.set('');
    const raw = this.userForm.getRawValue();
    const id  = this.editingUserId();

    if (id === null) {
      // Création
      const request: CreateUtilisateurRequest = {
        mail:      raw.mail.trim(),
        password:  raw.password,
        lastname:  raw.lastname.trim(),
        firstname: raw.firstname.trim(),
        role:      raw.role,
      };
      this.utilisateurService.create(request).subscribe({
        next: () => { this.loadUsers(); this.showUserModal.set(false); },
        error: () => this.userModalError.set('Erreur lors de la création.')
      });
    } else {
      // Modification
      const request: UpdateUtilisateurRequest = {
        mail:      raw.mail?.trim() || undefined,
        lastname:  raw.lastname?.trim() || undefined,
        firstname: raw.firstname?.trim() || undefined,
        role:      raw.role || undefined,
        password:  raw.password?.trim() || undefined,
      };
      this.utilisateurService.update(id, request).subscribe({
        next: () => { this.loadUsers(); this.showUserModal.set(false); },
        error: () => this.userModalError.set('Erreur lors de la modification.')
      });
    }
  }

  protected confirmDeleteUser(id: number): void {
    if (id === this.user()?.id) {
      alert('Vous ne pouvez pas supprimer votre propre compte.');
      return;
    }
    if (!confirm('Supprimer cet utilisateur ?')) return;
    this.utilisateurService.delete(id).subscribe({
      next: () => this.users.update(list => list.filter(u => u.id !== id)),
      error: () => alert('Erreur lors de la suppression.')
    });
  }

  protected isEditingSelf(id: number): boolean {
    return this.user()?.id === id;
  }

  protected retour(): void {
    this.router.navigate(['/menu']);
  }
}
