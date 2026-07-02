import { Component, inject, computed } from '@angular/core';
import { ConsoleStateService } from '../../services/console-state';
import {CommonModule} from '@angular/common';
import {JsonHighlighterPipe} from '../../pipes/json-highlighter-pipe';

@Component({
  selector: 'app-console',
  standalone: true,
  imports: [CommonModule, JsonHighlighterPipe],
  templateUrl: './console.html'

})
export class ConsoleComponent {
  state = inject(ConsoleStateService);

  // Récupération réactive des données via les Signals
  response = this.state.currentResponse;
  logs = this.state.logs;

  // Méthode simple pour formater le JSON
  formattedJson = computed(() => {
    const res = this.response();
    if (!res || !res.bodyText) return '';
    try {
      return JSON.stringify(JSON.parse(res.bodyText), null, 2);
    } catch {
      return res.bodyText; // Si ce n'est pas du JSON valide, on renvoie brut
    }
  });
}
