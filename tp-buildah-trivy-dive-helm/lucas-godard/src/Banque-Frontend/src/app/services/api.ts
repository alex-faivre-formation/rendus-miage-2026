import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);

  // L'URL de base est dynamique et gérée par l'utilisateur
  private gatewayUrl = '/api';

  setGatewayUrl(url: string) {
    this.gatewayUrl = url.replace(/\/+$/, '');
  }

  getGatewayUrl(): string {
    return this.gatewayUrl;
  }

  execute(method: string, path: string, body?: any): Observable<any> {
    const url = `${this.gatewayUrl}${path}`;
    let headers = new HttpHeaders({ 'Accept': 'application/json' });

    if (body) {
      headers = headers.set('Content-Type', 'application/json');
    }

    // On retourne la réponse brute (HttpResponse) pour avoir accès au status code
    return this.http.request(method, url, {
      body,
      headers,
      observe: 'response',
      responseType: 'text' // Pour gérer les erreurs ou les JSON vides proprement
    });
  }
}
