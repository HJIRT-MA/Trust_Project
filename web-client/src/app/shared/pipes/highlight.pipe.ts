import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'highlight',
  standalone: true
})
export class HighlightPipe implements PipeTransform {

  transform(text: string, search: string): string {
    if (!text || !search) {
      return text;
    }

    // Extract significant words from the search string (more than 3 chars)
    const words = search
      .split(/\W+/)
      .filter(word => word.length > 3)
      .map(word => word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')); // Escape regex chars

    if (words.length === 0) {
      return text;
    }

    const pattern = new RegExp(`(${words.join('|')})`, 'gi');
    return text.replace(pattern, match => `<span class="bg-indigo-500/40 text-white px-0.5 rounded">${match}</span>`);
  }

}
