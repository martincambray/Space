import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SpacecraftComposant } from './spacecraft.composant';

describe('SpacecraftComposant', () => {
  let component: SpacecraftComposant;
  let fixture: ComponentFixture<SpacecraftComposant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SpacecraftComposant],
    }).compileComponents();

    fixture = TestBed.createComponent(SpacecraftComposant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
