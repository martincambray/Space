import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MissionComposant } from './mission.composant';

describe('MissionComposant', () => {
  let component: MissionComposant;
  let fixture: ComponentFixture<MissionComposant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MissionComposant],
    }).compileComponents();

    fixture = TestBed.createComponent(MissionComposant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
