import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api';
import { ConsoleStateService } from '../../services/console-state';

export interface OperationConfig {
  label: string;
  method: 'GET' | 'POST';
  path: string;
  hasInputs?: boolean;
  inputs?: { id: string; placeholder: string; value: any; isWide?: boolean }[];
}

@Component({
  selector: 'app-operation-card',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './operation-card.html'
})
export class OperationCardComponent {
  private api = inject(ApiService);
  private consoleState = inject(ConsoleStateService);

  @Input({ required: true }) title!: string;
  @Input({ required: true }) type!: string;
  @Input({ required: true }) index!: string;
  @Input({ required: true }) operations: OperationConfig[] = [];

  // Gestion des modèles de formulaires locaux pour stocker la saisie de l'utilisateur
  inputModels: { [key: string]: any } = {};

  executeOperation(op: OperationConfig) {
    let finalPath = op.path;
    let body: any = undefined;

    // 1. Extraction dynamique des paramètres d'URL ou construction du Body
    if (op.inputs) {
      if (op.method === 'GET') {
        // Remplacement des placeholders type {id} ou construction de query params
        op.inputs.forEach(input => {
          const val = this.inputModels[input.id] ?? input.value;
          if (finalPath.includes(`{${input.id}}`)) {
            finalPath = finalPath.replace(`{${input.id}}`, val);
          } else if (finalPath.includes('=')) {
            // Gestion simplifiée pour le paramètre ?client={id}
            finalPath = finalPath.replace(`{id}`, val);
          }
        });
      } else if (op.method === 'POST') {
        // Pour un POST, on rassemble tous les champs de saisie dans un objet JSON payload
        body = {};
        op.inputs.forEach(input => {
          const val = this.inputModels[input.id] ?? input.value;
          // Conversion automatique en nombre si la valeur initiale était un nombre
          body[input.id] = typeof input.value === 'number' ? Number(val) : val;
        });
      }
    }

    // 2. Exécution de la requête via le service centralisé
    const t0 = performance.now();
    this.api.execute(op.method, finalPath, body).subscribe({
      next: (response) => {
        const ms = Math.round(performance.now() - t0);
        this.handleSuccess(op.method, finalPath, response, ms, body);
      },
      error: (err) => {
        const ms = Math.round(performance.now() - t0);
        this.handleError(op.method, finalPath, err, ms);
      }
    });
  }

  private handleSuccess(method: string, path: string, response: any, ms: number, bodySent: any) {
    const fullUrl = `${this.api.getGatewayUrl()}${path}`;

    this.consoleState.setResponse({
      method,
      url: fullUrl,
      status: response.status,
      ms,
      bodyText: response.body || '',
      isErr: !response.ok
    });

    this.consoleState.pushLog({
      method,
      path,
      status: response.status,
      ok: response.ok,
      body: bodySent
    });
  }

  private handleError(method: string, path: string, error: any, ms: number) {
    const fullUrl = `${this.api.getGatewayUrl()}${path}`;
    const errorMsg = `// Échec réseau : ${error.message}\n// Vérifie le statut de la Gateway ou le CORS.`;

    this.consoleState.setResponse({
      method,
      url: fullUrl,
      status: error.status || 'NETWORK',
      ms,
      bodyText: errorMsg,
      isErr: true
    });

    this.consoleState.pushLog({
      method,
      path,
      status: error.status || 'NET',
      ok: false
    });
  }
}
