import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OperationCardComponent } from './operation-card';

describe('OperationCardComponent', () => {
  let component: OperationCardComponent;
  let fixture: ComponentFixture<OperationCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OperationCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(OperationCardComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
