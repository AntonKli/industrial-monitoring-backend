import { Component, inject, OnInit, signal } from '@angular/core';
import {
  Device,
  MonitoringApi
} from '../../services/monitoring-api';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-devices',
  imports: [DatePipe],
  templateUrl: './devices.html',
  styleUrl: './devices.scss'
})
export class Devices implements OnInit {

  private readonly monitoringApi = inject(MonitoringApi);

  protected readonly devices = signal<Device[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');

  ngOnInit(): void {
    this.monitoringApi.getDevices().subscribe({
      next: (devices: Device[]) => {
        this.devices.set(devices);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Devices could not be loaded.');
        this.isLoading.set(false);
      }
    });
  }
}
