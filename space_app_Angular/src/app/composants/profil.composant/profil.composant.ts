import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UtilisateurService } from '../../services/utilisateur.service';
import { UtilisateurModel } from '../../models/utilisateur.model';

@Component({
  selector: 'app-profil',
  imports: [ReactiveFormsModule],
  templateUrl: './profil.composant.html',
  styleUrl: './profil.composant.css',
})
export class ProfilComposant implements OnInit {
  private utilisateurService = inject(UtilisateurService);
  private formBuilder = inject(FormBuilder);
  private router = inject(Router);

  protected user = signal<UtilisateurModel | null>(null);
  protected success = signal(false);
  protected error = signal('');

  protected form!: FormGroup;
  protected passwordCtrl!: FormControl;
  protected confirmCtrl!: FormControl;

  ngOnInit(): void {
    this.utilisateurService.findMe().subscribe({
      next: u => this.user.set(u),
      error: () => this.router.navigate(['/login'])
    });

    this.passwordCtrl = this.formBuilder.control('', [Validators.required, Validators.minLength(8)]);
    this.confirmCtrl  = this.formBuilder.control('', [Validators.required]);
    this.form = this.formBuilder.group(
      { password: this.passwordCtrl, confirm: this.confirmCtrl },
      { validators: this.passwordsMatch }
    );
  }

  private passwordsMatch(g: FormGroup) {
    return g.get('password')?.value === g.get('confirm')?.value ? null : { mismatch: true };
  }

  protected submit(): void {
    if (this.form.invalid) return;
    this.success.set(false);
    this.error.set('');
    this.utilisateurService.updatePassword(this.passwordCtrl.value).subscribe({
      next: () => {
        this.success.set(true);
        this.form.reset();
      },
      error: () => this.error.set('Erreur lors de la mise à jour du mot de passe.')
    });
  }

  protected retour(): void {
    this.router.navigate(['/menu']);
  }
}
