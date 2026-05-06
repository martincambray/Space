import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RoleComposant } from './role.composant';

describe('RoleComposant', () => {
  let component: RoleComposant;
  let fixture: ComponentFixture<RoleComposant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoleComposant],
    }).compileComponents();

    fixture = TestBed.createComponent(RoleComposant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
