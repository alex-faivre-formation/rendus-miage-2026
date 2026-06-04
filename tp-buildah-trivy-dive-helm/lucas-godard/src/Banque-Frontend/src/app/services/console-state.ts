import { Injectable, signal } from '@angular/core';

export interface LogEntry {
  method: string;
  path: string;
  status: string | number;
  ok: boolean;
  body?: any;
}

export interface CurrentResponse {
  method: string;
  url: string;
  status: string | number;
  ms: number | null;
  bodyText: string;
  isErr: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ConsoleStateService {
  // État de la connexion Gateway
  gatewayStatus = signal<'non testé' | 'ok' | 'err' | 'wait'>('non testé');
  gatewayMessage = signal<string>('non testé');

  // Logs et réponse actuelle
  logs = signal<LogEntry[]>([]);
  currentResponse = signal<CurrentResponse | null>(null);

  pushLog(entry: LogEntry) {
    this.logs.update(current => {
      const newLogs = [entry, ...current];
      return newLogs.slice(0, 12); // On garde les 12 derniers
    });
  }

  setResponse(resp: CurrentResponse) {
    this.currentResponse.set(resp);
  }

  clearConsole() {
    this.logs.set([]);
    this.currentResponse.set(null);
  }
}
