import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { MonitoringApi } from './monitoring-api';

describe('MonitoringApi', () => {
  let service: MonitoringApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(MonitoringApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should request per-device latest observed telemetry and health', () => {
    service.getLatestObservedTelemetry('edge 01').subscribe();
    service.getLatestObservedHealth('edge 01').subscribe();

    http.expectOne('/api/telemetry/device/edge%2001/latest-observed').flush({});
    http.expectOne('/api/health/device/edge%2001/latest-observed').flush({});
  });

  it('should expose explicit latest received endpoints', () => {
    service.getLatestReceivedTelemetry().subscribe();
    service.getLatestReceivedTelemetryForDevice('edge01').subscribe();
    service.getLatestReceivedHealth().subscribe();
    service.getLatestReceivedHealthForDevice('edge01').subscribe();

    http.expectOne('/api/telemetry/latest-received').flush({});
    http.expectOne('/api/telemetry/device/edge01/latest-received').flush({});
    http.expectOne('/api/health/latest-received').flush({});
    http.expectOne('/api/health/device/edge01/latest-received').flush({});
  });
});
