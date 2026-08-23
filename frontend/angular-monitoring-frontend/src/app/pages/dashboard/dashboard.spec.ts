import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';

import {
  Device,
  DeviceHealth,
  MonitoringApi,
  Telemetry
} from '../../services/monitoring-api';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let fixture: ComponentFixture<Dashboard>;
  let component: Dashboard;
  let api: {
    getBackendHealth: ReturnType<typeof vi.fn>;
    getDevices: ReturnType<typeof vi.fn>;
    getLatestObservedTelemetry: ReturnType<typeof vi.fn>;
    getLatestObservedHealth: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    api = {
      getBackendHealth: vi.fn().mockReturnValue(of({ status: 'UP' })),
      getDevices: vi.fn(),
      getLatestObservedTelemetry: vi.fn(),
      getLatestObservedHealth: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [{ provide: MonitoringApi, useValue: api }]
    }).compileComponents();
  });

  it('should automatically select the only device for both observed requests', () => {
    api.getDevices.mockReturnValue(of([device('edge01', 1)]));
    api.getLatestObservedTelemetry.mockReturnValue(of(telemetry('edge01', 7, 42)));
    api.getLatestObservedHealth.mockReturnValue(of(health('edge01', 7, 40)));

    createComponent();

    expect(api.getLatestObservedTelemetry).toHaveBeenCalledWith('edge01');
    expect(api.getLatestObservedHealth).toHaveBeenCalledWith('edge01');
    expect(fixture.nativeElement.textContent).toContain('Latest Observed Telemetry');
  });

  it('should wait for a selection when multiple devices exist and load both for that device', () => {
    api.getDevices.mockReturnValue(of([
      device('edge01', 1),
      device('edge02', 2)
    ]));
    api.getLatestObservedTelemetry.mockReturnValue(of(telemetry('edge02', 8, 5)));
    api.getLatestObservedHealth.mockReturnValue(of(health('edge02', 8, 4)));

    createComponent();
    expect(api.getLatestObservedTelemetry).not.toHaveBeenCalled();
    expect(api.getLatestObservedHealth).not.toHaveBeenCalled();

    selectDevice('edge02');

    expect(api.getLatestObservedTelemetry).toHaveBeenCalledWith('edge02');
    expect(api.getLatestObservedHealth).toHaveBeenCalledWith('edge02');
  });

  it('should show an empty state when no protocol-v3 observed records exist', () => {
    const notFound = new HttpErrorResponse({ status: 404 });
    api.getDevices.mockReturnValue(of([device('edge01', 1)]));
    api.getLatestObservedTelemetry.mockReturnValue(throwError(() => notFound));
    api.getLatestObservedHealth.mockReturnValue(throwError(() => notFound));

    createComponent();

    expect(fixture.nativeElement.textContent)
      .toContain('No protocol-v3 telemetry has been observed for edge01.');
    expect(fixture.nativeElement.textContent)
      .toContain('No protocol-v3 health data has been observed for edge01.');
    expect(fixture.nativeElement.textContent)
      .not.toContain('Telemetry data could not be loaded.');
    expect(fixture.nativeElement.textContent)
      .not.toContain('Device health data could not be loaded.');
  });

  it('should show only a telemetry error while successful health remains visible', () => {
    const serverError = new HttpErrorResponse({ status: 500 });
    api.getDevices.mockReturnValue(of([device('edge01', 1)]));
    api.getLatestObservedTelemetry.mockReturnValue(throwError(() => serverError));
    api.getLatestObservedHealth.mockReturnValue(of(health('edge01', 7, 40)));

    createComponent();

    expect(fixture.nativeElement.textContent)
      .toContain('Telemetry data could not be loaded.');
    expect(fixture.nativeElement.textContent)
      .not.toContain('No protocol-v3 telemetry has been observed');
    expect(fixture.nativeElement.textContent)
      .toContain('Latest Observed Device Health');
  });

  it('should show only a health error while successful telemetry remains visible', () => {
    const serverError = new HttpErrorResponse({ status: 500 });
    api.getDevices.mockReturnValue(of([device('edge01', 1)]));
    api.getLatestObservedTelemetry.mockReturnValue(of(telemetry('edge01', 7, 42)));
    api.getLatestObservedHealth.mockReturnValue(throwError(() => serverError));

    createComponent();

    expect(fixture.nativeElement.textContent)
      .toContain('Device health data could not be loaded.');
    expect(fixture.nativeElement.textContent)
      .not.toContain('No protocol-v3 health data has been observed');
    expect(fixture.nativeElement.textContent)
      .toContain('Latest Observed Telemetry');
  });

  it('should reset data, empty and error states across device changes', () => {
    const notFound = new HttpErrorResponse({ status: 404 });
    const serverError = new HttpErrorResponse({ status: 500 });
    const telemetryResponses = new Map<string, Subject<Telemetry>>([
      ['edge02', new Subject<Telemetry>()],
      ['edge03', new Subject<Telemetry>()]
    ]);
    const healthResponses = new Map<string, Subject<DeviceHealth>>([
      ['edge02', new Subject<DeviceHealth>()],
      ['edge03', new Subject<DeviceHealth>()]
    ]);
    api.getDevices.mockReturnValue(of([
      device('edge01', 1),
      device('edge02', 2),
      device('edge03', 3)
    ]));
    api.getLatestObservedTelemetry.mockImplementation(
      (deviceId: string): Observable<Telemetry> => deviceId === 'edge01'
        ? throwError(() => notFound)
        : telemetryResponses.get(deviceId)!
    );
    api.getLatestObservedHealth.mockImplementation(
      (deviceId: string): Observable<DeviceHealth> => deviceId === 'edge01'
        ? of(health('edge01', 7, 40))
        : healthResponses.get(deviceId)!
    );

    createComponent();
    selectDevice('edge01');
    expect(fixture.nativeElement.textContent)
      .toContain('No protocol-v3 telemetry has been observed for edge01.');
    expect(fixture.nativeElement.textContent)
      .toContain('Session generation 7, sequence 40');

    selectDevice('edge02');
    expect(fixture.nativeElement.textContent)
      .not.toContain('No protocol-v3 telemetry has been observed for edge01.');
    expect(fixture.nativeElement.textContent)
      .not.toContain('Session generation 7, sequence 40');
    expect(fixture.nativeElement.textContent).toContain('Loading telemetry data...');
    expect(fixture.nativeElement.textContent).toContain('Loading device health data...');

    telemetryResponses.get('edge02')!.error(serverError);
    healthResponses.get('edge02')!.next(health('edge02', 8, 4));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent)
      .toContain('Telemetry data could not be loaded.');
    expect(fixture.nativeElement.textContent)
      .toContain('Session generation 8, sequence 4');

    selectDevice('edge03');
    expect(fixture.nativeElement.textContent)
      .not.toContain('Telemetry data could not be loaded.');
    expect(fixture.nativeElement.textContent)
      .not.toContain('Session generation 8, sequence 4');
    expect(fixture.nativeElement.textContent).toContain('Loading telemetry data...');
    expect(fixture.nativeElement.textContent).toContain('Loading device health data...');

    telemetryResponses.get('edge03')!.next(telemetry('edge03', 9, 2));
    healthResponses.get('edge03')!.next(health('edge03', 9, 1));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Deviceedge03');
  });

  it('should ignore late responses from a previously selected device', () => {
    const telemetryResponses = new Map<string, Subject<Telemetry>>([
      ['edge01', new Subject<Telemetry>()],
      ['edge02', new Subject<Telemetry>()]
    ]);
    const healthResponses = new Map<string, Subject<DeviceHealth>>([
      ['edge01', new Subject<DeviceHealth>()],
      ['edge02', new Subject<DeviceHealth>()]
    ]);
    api.getDevices.mockReturnValue(of([
      device('edge01', 1),
      device('edge02', 2)
    ]));
    api.getLatestObservedTelemetry.mockImplementation(
      (deviceId: string): Observable<Telemetry> => telemetryResponses.get(deviceId)!
    );
    api.getLatestObservedHealth.mockImplementation(
      (deviceId: string): Observable<DeviceHealth> => healthResponses.get(deviceId)!
    );

    createComponent();
    selectDevice('edge01');
    selectDevice('edge02');

    telemetryResponses.get('edge02')!.next(telemetry('edge02', 8, 5));
    healthResponses.get('edge02')!.next(health('edge02', 8, 4));
    telemetryResponses.get('edge01')!.next(telemetry('edge01', 7, 100));
    healthResponses.get('edge01')!.next(health('edge01', 7, 99));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Deviceedge02');
    expect(fixture.nativeElement.textContent).not.toContain('Deviceedge01');
    const healthDetails = fixture.nativeElement.querySelector('.health-details') as HTMLElement;
    expect(healthDetails.textContent).toContain('Session generation 8, sequence 4');
    expect(healthDetails.textContent).not.toContain('Session generation 7, sequence 99');
  });

  it('should ignore late errors from a previously selected device', () => {
    const serverError = new HttpErrorResponse({ status: 500 });
    const telemetryResponses = new Map<string, Subject<Telemetry>>([
      ['edge01', new Subject<Telemetry>()],
      ['edge02', new Subject<Telemetry>()]
    ]);
    const healthResponses = new Map<string, Subject<DeviceHealth>>([
      ['edge01', new Subject<DeviceHealth>()],
      ['edge02', new Subject<DeviceHealth>()]
    ]);
    api.getDevices.mockReturnValue(of([
      device('edge01', 1),
      device('edge02', 2)
    ]));
    api.getLatestObservedTelemetry.mockImplementation(
      (deviceId: string): Observable<Telemetry> => telemetryResponses.get(deviceId)!
    );
    api.getLatestObservedHealth.mockImplementation(
      (deviceId: string): Observable<DeviceHealth> => healthResponses.get(deviceId)!
    );

    createComponent();
    selectDevice('edge01');
    selectDevice('edge02');

    telemetryResponses.get('edge02')!.next(telemetry('edge02', 8, 5));
    healthResponses.get('edge02')!.next(health('edge02', 8, 4));
    telemetryResponses.get('edge01')!.error(serverError);
    healthResponses.get('edge01')!.error(serverError);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Deviceedge02');
    expect(fixture.nativeElement.textContent)
      .not.toContain('Telemetry data could not be loaded.');
    expect(fixture.nativeElement.textContent)
      .not.toContain('Device health data could not be loaded.');
  });

  function createComponent(): void {
    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function selectDevice(deviceId: string): void {
    const selectable = component as unknown as {
      selectDevice(selectedDeviceId: string): void;
    };
    selectable.selectDevice(deviceId);
    fixture.detectChanges();
  }

  function device(deviceId: string, id: number): Device {
    return { id, deviceId, createdAt: '2026-08-20T10:00:00Z' };
  }

  function telemetry(deviceId: string, generation: number, sequence: number): Telemetry {
    return {
      id: sequence,
      deviceId,
      gatewayTimestamp: sequence * 1000,
      sequenceNumber: sequence,
      sessionId: '550e8400-e29b-41d4-a716-446655440000',
      sessionGeneration: generation,
      temperatureC: 30.2,
      rpm: 1600,
      createdAt: '2026-08-20T10:00:00Z'
    };
  }

  function health(deviceId: string, generation: number, sequence: number): DeviceHealth {
    return {
      id: sequence,
      deviceId,
      gatewayTimestamp: sequence * 1000,
      sequenceNumber: sequence,
      sessionId: '550e8400-e29b-41d4-a716-446655440000',
      sessionGeneration: generation,
      state: 1,
      mqttConnected: true,
      pubLastOk: true,
      bufferFill: 0,
      bufferDrops: 0,
      diagPubOk: sequence,
      diagPubFail: 0,
      diagReconnects: 0,
      diagLastError: 0,
      diagUptimeS: sequence,
      createdAt: '2026-08-20T10:00:00Z'
    };
  }
});
