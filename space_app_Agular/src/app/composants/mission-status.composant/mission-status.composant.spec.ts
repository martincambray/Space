import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MissionStatusComposant } from './mission-status.composant';

describe('MissionStatusComposant', () => {
  let component: MissionStatusComposant;
  let fixture: ComponentFixture<MissionStatusComposant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MissionStatusComposant],
    }).compileComponents();

    fixture = TestBed.createComponent(MissionStatusComposant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
