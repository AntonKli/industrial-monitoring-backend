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

export interface DeviceHealth {
  id: number;
  deviceId: string;
  gatewayTimestamp: number;
  sequenceNumber: number;
  state: number;
  mqttConnected: boolean;
  pubLastOk: boolean;
  bufferFill: number;
  bufferDrops: number;
  diagPubOk: number;
  diagPubFail: number;
  diagReconnects: number;
  diagLastError: number;
  diagUptimeS: number;
  createdAt: string;
}
export interface MonitoringEvent {
  id: number;
  deviceId: string;
  gatewayTimestamp: number;
  sequenceNumber: number;
  eventType: string;
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

  getLatestHealth(): Observable<DeviceHealth> {
    return this.http.get<DeviceHealth>('/api/health/latest');
  }
  getEvents(): Observable<MonitoringEvent[]> {
    return this.http.get<MonitoringEvent[]>('/api/events');
  }
}
