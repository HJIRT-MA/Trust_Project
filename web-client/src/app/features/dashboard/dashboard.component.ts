import { Component } from '@angular/core';
import { DocumentManagerComponent } from '../../dashboard/document-manager/document-manager.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DocumentManagerComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {

}
