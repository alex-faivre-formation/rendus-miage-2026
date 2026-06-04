import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../services/api';
import { ConsoleStateService } from '../../services/console-state';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './header.html'
})
export class HeaderComponent {
  private api = inject(ApiService);
  // On met le state en 'public' pour y accéder directement dans le HTML
  public state = inject(ConsoleStateService);

  // La valeur par défaut liée à l'input
  gatewayUrl = '/api';

  async testConnection() {
    // 1. Mise à jour de l'URL globale dans le service
    this.api.setGatewayUrl(this.gatewayUrl);

    // 2. Mise à jour visuelle (En attente)
    this.state.gatewayStatus.set('wait');
    this.state.gatewayMessage.set('test...');

    const t0 = performance.now();

    try {
      // Tentative 1 : L'actuator health
      const res = await firstValueFrom(this.api.execute('GET', '/clients/'));
      const ms = Math.round(performance.now() - t0);

      this.state.setResponse({
        method: 'GET', url: `${this.gatewayUrl}/actuator/health`,
        status: res.status, ms, bodyText: res.body || '', isErr: !res.ok
      });
      this.state.gatewayStatus.set(res.ok ? 'ok' : 'err');
      this.state.gatewayMessage.set(res.ok ? 'gateway en ligne' : `réponse ${res.status}`);

    } catch (err: any) {
      // Tentative 2 : Fallback sur la liste des clients (comme dans ton script d'origine)
      try {
        const fallbackRes = await firstValueFrom(this.api.execute('GET', '/comptes/'));
        this.state.setResponse({
          method: 'GET', url: `${this.gatewayUrl}/api/clients/`,
          status: fallbackRes.status, ms: null, bodyText: fallbackRes.body || '', isErr: !fallbackRes.ok
        });
        this.state.gatewayStatus.set(fallbackRes.ok ? 'ok' : 'err');
        this.state.gatewayMessage.set(fallbackRes.ok ? 'gateway en ligne' : `réponse ${fallbackRes.status}`);

      } catch (err2: any) {
        // Échec total : réseau injoignable ou CORS
        this.state.gatewayStatus.set('err');
        this.state.gatewayMessage.set('injoignable');
        this.state.setResponse({
          method: 'GET', url: `${this.gatewayUrl}/actuator/health`,
          status: 'NETWORK', ms: null,
          bodyText: `// Gateway injoignable : ${err2.message}\n// Vérifie que les conteneurs K8s tournent et le CORS.`,
          isErr: true
        });
      }
    }
  }
}
