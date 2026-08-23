import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  BackendHealth,
  Device,
  DeviceHealth,
  MonitoringApi,
  Telemetry
} from '../../services/monitoring-api';

type LatestDataState = 'idle' | 'loading' | 'data' | 'empty' | 'error';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {

  private readonly monitoringApi = inject(MonitoringApi);

  protected readonly backendStatus = signal('LOADING');
  protected readonly backendErrorMessage = signal('');
  protected readonly deviceErrorMessage = signal('');
  protected readonly deviceCount = signal(0);
  protected readonly devices = signal<Device[]>([]);
  protected readonly selectedDeviceId = signal('');
  protected readonly latestTelemetry = signal<Telemetry | null>(null);
  protected readonly latestHealth = signal<DeviceHealth | null>(null);
  protected readonly telemetryState = signal<LatestDataState>('idle');
  protected readonly healthState = signal<LatestDataState>('idle');
  protected readonly telemetryErrorMessage = signal('');
  protected readonly healthErrorMessage = signal('');

  private latestRequestId = 0;

  ngOnInit(): void {
    this.monitoringApi.getBackendHealth().subscribe({
      next: (health: BackendHealth) => {
        this.backendStatus.set(health.status);
      },
      error: () => {
        this.backendStatus.set('DOWN');
        this.backendErrorMessage.set('Backend could not be reached.');
      }
    });
    this.monitoringApi.getDevices().subscribe({
      next: (devices: Device[]) => {
        this.devices.set(devices);
        this.deviceCount.set(devices.length);

        if (devices.length === 1) {
          this.selectDevice(devices[0].deviceId);
        }
      },
      error: () => {
        this.deviceErrorMessage.set('Device data could not be loaded.');
      }
    });
  }

  protected selectDevice(deviceId: string): void {
    const requestId = ++this.latestRequestId;

    this.selectedDeviceId.set(deviceId);
    this.latestTelemetry.set(null);
    this.latestHealth.set(null);
    this.telemetryErrorMessage.set('');
    this.healthErrorMessage.set('');
    this.telemetryState.set(deviceId ? 'loading' : 'idle');
    this.healthState.set(deviceId ? 'loading' : 'idle');

    if (!deviceId) {
      return;
    }

    this.monitoringApi.getLatestObservedTelemetry(deviceId).subscribe({
      next: (telemetry: Telemetry) => {
        if (!this.isCurrentRequest(requestId, deviceId)) {
          return;
        }

        this.latestTelemetry.set(telemetry);
        this.telemetryErrorMessage.set('');
        this.telemetryState.set('data');
      },
      error: (error: HttpErrorResponse) => {
        if (!this.isCurrentRequest(requestId, deviceId)) {
          return;
        }

        if (error.status === 404) {
          this.telemetryState.set('empty');
          return;
        }

        this.telemetryErrorMessage.set('Telemetry data could not be loaded.');
        this.telemetryState.set('error');
      }
    });

    this.monitoringApi.getLatestObservedHealth(deviceId).subscribe({
      next: (health: DeviceHealth) => {
        if (!this.isCurrentRequest(requestId, deviceId)) {
          return;
        }

        this.latestHealth.set(health);
        this.healthErrorMessage.set('');
        this.healthState.set('data');
      },
      error: (error: HttpErrorResponse) => {
        if (!this.isCurrentRequest(requestId, deviceId)) {
          return;
        }

        if (error.status === 404) {
          this.healthState.set('empty');
          return;
        }

        this.healthErrorMessage.set('Device health data could not be loaded.');
        this.healthState.set('error');
      }
    });
  }

  private isCurrentRequest(requestId: number, deviceId: string): boolean {
    return requestId === this.latestRequestId && deviceId === this.selectedDeviceId();
  }
}
