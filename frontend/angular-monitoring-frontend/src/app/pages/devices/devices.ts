import { Component, inject, OnInit, signal } from '@angular/core';
import {
  Device,
  MonitoringApi,
  MonitoringEvent
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
  protected readonly recentEvents = signal<MonitoringEvent[]>([]);

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
    this.monitoringApi.getEvents().subscribe({
      next: (events: MonitoringEvent[]) => {
        const latestEvents = events.slice(-5).reverse();
        this.recentEvents.set(latestEvents);
      },
      error: () => {
        this.errorMessage.set('Events could not be loaded.');
      }
    });
  }
}
