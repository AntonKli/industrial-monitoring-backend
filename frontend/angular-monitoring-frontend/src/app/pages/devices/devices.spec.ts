import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import {
  MonitoringApi,
  MonitoringEvent
} from '../../services/monitoring-api';
import { Devices } from './devices';

describe('Devices', () => {
  let component: Devices;
  let fixture: ComponentFixture<Devices>;
  let api: {
    getDevices: ReturnType<typeof vi.fn>;
    getEvents: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    api = {
      getDevices: vi.fn().mockReturnValue(of([])),
      getEvents: vi.fn().mockReturnValue(of([]))
    };

    await TestBed.configureTestingModule({
      imports: [Devices],
      providers: [{ provide: MonitoringApi, useValue: api }]
    }).compileComponents();

    fixture = TestBed.createComponent(Devices);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should use the first five events from the backend ordered list', () => {
    api.getEvents.mockReturnValue(of([
      event(6),
      event(5),
      event(4),
      event(3),
      event(2),
      event(1)
    ]));

    fixture.detectChanges();

    const recentEvents = (component as unknown as {
      recentEvents(): MonitoringEvent[];
    }).recentEvents();
    expect(recentEvents.map(item => item.id)).toEqual([6, 5, 4, 3, 2]);
  });

  function event(id: number): MonitoringEvent {
    return {
      id,
      deviceId: 'edge01',
      gatewayTimestamp: id * 1000,
      sequenceNumber: id,
      sessionId: '550e8400-e29b-41d4-a716-446655440000',
      sessionGeneration: 7,
      eventType: `EVENT_${id}`,
      createdAt: '2026-08-20T10:00:00Z'
    };
  }
});
