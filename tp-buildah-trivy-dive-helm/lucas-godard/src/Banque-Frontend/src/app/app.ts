import {Component, inject} from '@angular/core';
import { OperationCardComponent, OperationConfig } from './components/operation-card/operation-card';
import { HeaderComponent } from './components/header/header';
import { ConsoleComponent } from './components/console/console';
import { ApiService } from './services/api';
import {ConsoleStateService} from './services/console-state';
import {firstValueFrom} from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [HeaderComponent, OperationCardComponent, ConsoleComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent {

  private consoleState = inject(ConsoleStateService);
  private api = inject(ApiService);

  clientOperations: OperationConfig[] = [
    { label: 'Lister tous les clients', method: 'GET', path: '/clients/' },
    {
      label: 'Récupérer un client', method: 'GET', path: '/clients/{id}',
      inputs: [{ id: 'id', placeholder: 'id', value: 1 }]
    },
    {
      label: 'Créer un client', method: 'POST', path: '/clients/',
      inputs: [
        { id: 'id', placeholder: 'id', value: 1 },
        { id: 'nom', placeholder: 'nom', value: 'Dupond', isWide: true },
        { id: 'prenom', placeholder: 'prénom', value: 'Jean', isWide: true }
      ]
    }
  ];

  compteOperations: OperationConfig[] = [
    { label: 'Lister tous les comptes', method: 'GET', path: '/comptes/all' },
    {
      label: "Comptes d'un client", method: 'GET', path: '/comptes/?client={id}',
      inputs: [{ id: 'id', placeholder: 'client', value: 1 }]
    },
    {
      label: 'Récupérer un compte', method: 'GET', path: '/comptes/{id}',
      inputs: [{ id: 'id', placeholder: 'id', value: 1 }]
    },
    {
      label: 'Créer un compte', method: 'POST', path: '/comptes/',
      inputs: [
        { id: 'id', placeholder: 'id', value: 1 },
        { id: 'solde', placeholder: 'solde', value: 1000 },
        { id: 'idclient', placeholder: 'id client', value: 1 }
      ]
    }
  ];

  compositeOperations: OperationConfig[] = [
    {
      label: 'Client + ses comptes', method: 'GET', path: '/clientscomptes/{id}',
      inputs: [{ id: 'id', placeholder: 'id client', value: 1 }]
    },
    {
      label: 'Ouvrir un compte pour un client', method: 'POST', path: '/clientscomptes/{id}',
      inputs: [
        { id: 'id', placeholder: 'id compte', value: 2 },
        { id: 'solde', placeholder: 'solde', value: 500 },
        { id: 'idclient', placeholder: 'id client', value: 1 }
      ]
    }
  ];

  clearConsole() {
    this.consoleState.clearConsole();
  }

  async seedData() {
    this.consoleState.setResponse({
      method: 'SYS', url: 'Génération du jeu de données...', status: 'wait', ms: null, bodyText: 'Patientez...', isErr: false
    });

    try {
      // Étape 1 : Créer le client
      await firstValueFrom(this.api.execute('POST', '/clients/', { id: 1, nom: 'Dupond', prenom: 'Jean' }));
      await this.delay(250);

      // Étape 2 : Créer son compte (Service Composite)
      await firstValueFrom(this.api.execute('POST', '/clientscomptes/1', { id: 1, solde: 1000, idclient: 1 }));
      await this.delay(250);

      // Étape 3 : Récupérer le résultat
      const finalRes = await firstValueFrom(this.api.execute('GET', '/clientscomptes/1'));

      // Affichage du succès final dans la console
      this.consoleState.setResponse({
        method: 'GET', url: '/clientscomptes/1', status: 200, ms: null, bodyText: finalRes.body || 'Terminé', isErr: false
      });
      this.consoleState.pushLog({ method: 'SEED', path: 'Jeu de données généré', status: 'OK', ok: true });

    } catch (e: any) {
      this.consoleState.setResponse({
        method: 'ERR', url: 'Erreur lors du Seed', status: e.status || 500, ms: null, bodyText: e.message, isErr: true
      });
    }
  }

  // Petite fonction utilitaire pour remplacer le setTimeout natif
  private delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
