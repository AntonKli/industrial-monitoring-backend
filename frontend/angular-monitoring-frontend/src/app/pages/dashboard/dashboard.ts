import { Component, inject, OnInit, signal } from '@angular/core';
import {
  BackendHealth,
  Device,
  MonitoringApi,
  Telemetry
} from '../../services/monitoring-api';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {

  private readonly monitoringApi = inject(MonitoringApi);

  protected readonly backendStatus = signal('LOADING');
  protected readonly errorMessage = signal('');
  protected readonly deviceCount = signal(0);
  protected readonly latestTelemetry = signal<Telemetry | null>(null);

  ngOnInit(): void {
    this.monitoringApi.getBackendHealth().subscribe({
      next: (health: BackendHealth) => {
        this.backendStatus.set(health.status);
      },
      error: () => {
        this.backendStatus.set('DOWN');
        this.errorMessage.set('Backend could not be reached.');
      }
    });
    this.monitoringApi.getDevices().subscribe({
      next: (devices: Device[]) => {
        this.deviceCount.set(devices.length);
      },
      error: () => {
        this.errorMessage.set('Device data could not be loaded.');
      }
    });
    this.monitoringApi.getLatestTelemetry().subscribe({
      next: (telemetry: Telemetry) => {
        this.latestTelemetry.set(telemetry);
      },
      error: () => {
        this.errorMessage.set('Telemetry data could not be loaded.');
      }
    });
  }
}
