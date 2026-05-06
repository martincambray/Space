import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TrajectoryLogsComposant } from './trajectory-logs.composant';

describe('TrajectoryLogsComposant', () => {
  let component: TrajectoryLogsComposant;
  let fixture: ComponentFixture<TrajectoryLogsComposant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TrajectoryLogsComposant],
    }).compileComponents();

    fixture = TestBed.createComponent(TrajectoryLogsComposant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
