import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({
  name: 'jsonHighlighter',
  standalone: true // Rendu autonome pour correspondre à ton architecture
})
export class JsonHighlighterPipe implements PipeTransform {
  private sanitizer = inject(DomSanitizer);

  transform(value: string): SafeHtml {
    if (!value) return '';

    // L'expression régulière magique de ton fichier d'origine
    let highlighted = value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/("(\\u[a-fA-F0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(\.\d+)?([eE][+-]?\d+)?)/g,
        match => {
          let cls = 'n'; // Classe par défaut (nombre)
          if (/^"/.test(match)) {
            cls = /:$/.test(match) ? 'k' : 's'; // k = key, s = string
          } else if (/true|false|null/.test(match)) {
            cls = 'b'; // b = boolean/null
          }
          return `<span class="${cls}">${match}</span>`;
        }
      );

    // On autorise Angular à injecter ce HTML de façon sécurisée
    return this.sanitizer.bypassSecurityTrustHtml(highlighted);
  }
}
