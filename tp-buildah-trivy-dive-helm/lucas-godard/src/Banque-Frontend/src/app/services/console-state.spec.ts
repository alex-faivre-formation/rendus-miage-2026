import { TestBed } from '@angular/core/testing';

import { ConsoleStateService } from './console-state';

describe('ConsoleStateService', () => {
  let service: ConsoleStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ConsoleStateService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
