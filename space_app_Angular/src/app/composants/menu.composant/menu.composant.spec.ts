import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MenuComposant } from './menu.composant';

describe('MenuComposant', () => {
  let component: MenuComposant;
  let fixture: ComponentFixture<MenuComposant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MenuComposant]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MenuComposant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
