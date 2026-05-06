import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.composant.html',
  styleUrl: './login.composant.css',
})
export class LoginComposant implements OnInit {
  private authService: AuthService = inject(AuthService);
  private formBuilder: FormBuilder = inject(FormBuilder);
  private router: Router = inject(Router);

  protected loginError = signal(false);
  protected loginForm!: FormGroup;
  protected mailCtrl!: FormControl;
  protected passwordCtrl!: FormControl;

  ngOnInit(): void {
    this.mailCtrl = this.formBuilder.control('', [Validators.required, Validators.email]);
    this.passwordCtrl = this.formBuilder.control('', [Validators.required, Validators.minLength(6)]);

    this.loginForm = this.formBuilder.group({
      mail: this.mailCtrl,
      password: this.passwordCtrl
    });
  }

  public connecter(): void {
    this.loginError.set(false);
    this.authService.auth(this.loginForm.getRawValue()).subscribe({
      next: resp => {
        this.authService.token = resp.token;
        this.router.navigate(['/dashboard']);
      },
      error: () => this.loginError.set(true)
    });
  }
}
