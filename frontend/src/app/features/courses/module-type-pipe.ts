import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'moduleType',
  standalone: true
})
export class ModuleTypePipe implements PipeTransform {

  transform(items: any[], type: string) {
    if (!items) return [];
    return items.filter((i) => i.type === type);
  }

}
