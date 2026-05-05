import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UtilisateurComposant } from './utilisateur.composant';

describe('UtilisateurComposant', () => {
  let component: UtilisateurComposant;
  let fixture: ComponentFixture<UtilisateurComposant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UtilisateurComposant],
    }).compileComponents();

    fixture = TestBed.createComponent(UtilisateurComposant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
