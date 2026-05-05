import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MissionTypeComposant } from './mission-type.composant';

describe('MissionTypeComposant', () => {
  let component: MissionTypeComposant;
  let fixture: ComponentFixture<MissionTypeComposant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MissionTypeComposant],
    }).compileComponents();

    fixture = TestBed.createComponent(MissionTypeComposant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
