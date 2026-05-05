import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CelestialBodyComposant } from './celestial-body.composant';

describe('CelestialBodyComposant', () => {
  let component: CelestialBodyComposant;
  let fixture: ComponentFixture<CelestialBodyComposant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CelestialBodyComposant],
    }).compileComponents();

    fixture = TestBed.createComponent(CelestialBodyComposant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
