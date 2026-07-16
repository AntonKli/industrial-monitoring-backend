import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface BackendHealth {
  status: string;
}

export interface Device {
  id: number;
  deviceId: string;
  createdAt: string;
}
export interface Telemetry {
  id: number;
  deviceId: string;
  gatewayTimestamp: number;
  sequenceNumber: number;
  temperatureC: number;
  rpm: number;
  createdAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class MonitoringApi {
  private readonly http = inject(HttpClient);

  getBackendHealth(): Observable<BackendHealth> {
    return this.http.get<BackendHealth>('/actuator/health');
  }

  getDevices(): Observable<Device[]> {
    return this.http.get<Device[]>('/api/devices');
  }
  getLatestTelemetry(): Observable<Telemetry> {
    return this.http.get<Telemetry>('/api/telemetry/latest');
  }
}
