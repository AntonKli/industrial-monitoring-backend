import { DatePipe } from '@angular/common';
import { HttpResponse } from '@angular/common/http';
import {
  Component,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  Device,
  MonitoringApi,
  MonitoringEvent
} from '../../services/monitoring-api';

@Component({
  selector: 'app-devices',
  imports: [
    DatePipe,
    FormsModule
  ],
  templateUrl: './devices.html',
  styleUrl: './devices.scss'
})
export class Devices implements OnInit {
  private readonly monitoringApi = inject(MonitoringApi);

  protected readonly devices = signal<Device[]>([]);
  protected readonly recentEvents =
    signal<MonitoringEvent[]>([]);

  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');

  protected readonly fromDate = signal('');
  protected readonly toDate = signal('');
  protected readonly isExporting = signal(false);
  protected readonly exportMessage = signal('');
  protected readonly exportError = signal('');

  ngOnInit(): void {
    this.monitoringApi.getDevices().subscribe({
      next: (devices: Device[]) => {
        this.devices.set(devices);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(
          'Devices could not be loaded.'
        );
        this.isLoading.set(false);
      }
    });

    this.monitoringApi.getEvents().subscribe({
      next: (events: MonitoringEvent[]) => {
        const latestEvents =
          events.slice(-5).reverse();

        this.recentEvents.set(latestEvents);
      },
      error: () => {
        this.errorMessage.set(
          'Events could not be loaded.'
        );
      }
    });
  }

  protected createRangeExport(): void {
    this.exportMessage.set('');
    this.exportError.set('');

    const selectedFromDate = this.fromDate();
    const selectedToDate = this.toDate();

    if (!selectedFromDate || !selectedToDate) {
      this.exportError.set(
        'Please select a start date and an end date.'
      );
      return;
    }

    if (selectedFromDate > selectedToDate) {
      this.exportError.set(
        'The end date must be equal to or later than the start date.'
      );
      return;
    }

    const toDateExclusive =
      this.addOneDay(selectedToDate);

    this.isExporting.set(true);

    this.monitoringApi
      .downloadRangeExport(
        selectedFromDate,
        toDateExclusive
      )
      .subscribe({
        next: (response: HttpResponse<Blob>) => {
          this.isExporting.set(false);

          const zipFile = response.body;

          if (!zipFile) {
            this.exportError.set(
              'The server returned an empty ZIP file.'
            );
            return;
          }

          const contentDisposition =
            response.headers.get(
              'Content-Disposition'
            );

          const fileName =
            this.getDownloadFileName(
              contentDisposition,
              selectedFromDate,
              selectedToDate
            );

          this.downloadFile(
            zipFile,
            fileName
          );

          this.exportMessage.set(
            `ZIP export "${fileName}" is ready for download.`
          );
        },
        error: () => {
          this.isExporting.set(false);

          this.exportError.set(
            'The ZIP export could not be downloaded.'
          );
        }
      });
  }

  private addOneDay(dateValue: string): string {
    const date =
      new Date(`${dateValue}T00:00:00Z`);

    date.setUTCDate(
      date.getUTCDate() + 1
    );

    return date.toISOString().slice(0, 10);
  }

  private getDownloadFileName(
    contentDisposition: string | null,
    fromDate: string,
    toDate: string
  ): string {
    if (contentDisposition) {
      const fileNameMatch =
        contentDisposition.match(
          /filename="([^"]+)"/
        );

      if (fileNameMatch?.[1]) {
        return fileNameMatch[1];
      }
    }

    return (
      `monitoring-export-${fromDate}` +
      `_to_${toDate}.zip`
    );
  }

  private downloadFile(
    file: Blob,
    fileName: string
  ): void {
    const downloadUrl =
      URL.createObjectURL(file);

    const downloadLink =
      document.createElement('a');

    downloadLink.href = downloadUrl;
    downloadLink.download = fileName;

    document.body.appendChild(downloadLink);
    downloadLink.click();
    downloadLink.remove();

    URL.revokeObjectURL(downloadUrl);
  }
}
