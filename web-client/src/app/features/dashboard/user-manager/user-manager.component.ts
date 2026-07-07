import { Component, OnInit, inject, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

export interface User {
  id: string;
  username: string;
  email: string;
  roles: string[];
}

@Component({
  selector: 'app-user-manager',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatSelectModule,
    MatSnackBarModule,
    MatIconModule,
    FormsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './user-manager.component.html',
  styleUrls: ['./user-manager.component.scss']
})
export class UserManagerComponent implements OnInit {
  private http = inject(HttpClient);
  private snackBar = inject(MatSnackBar);

  displayedColumns: string[] = ['username', 'email', 'roles', 'actions'];
  dataSource = new MatTableDataSource<User>([]);
  availableRoles = ['admin', 'analyst', 'viewer'];
  loading = true;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.loading = true;
    this.http.get<User[]>('http://localhost:8082/api/users').subscribe({
      next: (users) => {
        this.dataSource.data = users;
        this.dataSource.paginator = this.paginator;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load users', err);
        this.snackBar.open('Erreur de chargement des utilisateurs', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  updateRoles(user: User) {
    this.http.post(`http://localhost:8082/api/users/${user.id}/roles`, { roles: user.roles }).subscribe({
      next: () => {
        this.snackBar.open('Rôles mis à jour avec succès', 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        console.error('Failed to update roles', err);
        this.snackBar.open('Erreur lors de la mise à jour des rôles', 'Fermer', { duration: 3000 });
      }
    });
  }
}
