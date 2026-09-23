import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="padding: 20px; border: 1px solid #ccc; border-radius: 6px; margin-top: 20px;">
      <h2>Dashboard (Ruta Protegida)</h2>
      <div *ngIf="account; else noAccount">
        <p><strong>Nombre del usuario:</strong> {{ account.name }}</p>
        <p><strong>Username / Email:</strong> {{ account.username }}</p>
        
        <h3>Roles del usuario (account.idTokenClaims.roles):</h3>
        <ul *ngIf="roles.length > 0; else noRoles">
          <li *ngFor="let role of roles">{{ role }}</li>
        </ul>
        <ng-template #noRoles>
          <p>No se encontraron roles asignados en idTokenClaims.</p>
        </ng-template>

        <details style="margin-top: 15px;">
          <summary>Ver todos los ID Token Claims</summary>
          <pre style="background: #f4f4f4; padding: 10px; border-radius: 4px;">{{ account.idTokenClaims | json }}</pre>
        </details>
      </div>
      <ng-template #noAccount>
        <p>No hay información de cuenta activa disponible.</p>
      </ng-template>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  account: AccountInfo | null = null;
  roles: string[] = [];

  constructor(private msalService: MsalService) {}

  ngOnInit(): void {
    const activeAccount = this.msalService.instance.getActiveAccount();
    const accounts = this.msalService.instance.getAllAccounts();
    this.account = activeAccount || (accounts.length > 0 ? accounts[0] : null);

    if (this.account && this.account.idTokenClaims) {
      const claims = this.account.idTokenClaims as Record<string, any>;
      if (Array.isArray(claims['roles'])) {
        this.roles = claims['roles'];
      }
    }
  }
}
