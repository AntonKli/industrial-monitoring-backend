import { TestBed } from '@angular/core/testing';

import { MonitoringApi } from './monitoring-api';

describe('MonitoringApi', () => {
  let service: MonitoringApi;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MonitoringApi);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
