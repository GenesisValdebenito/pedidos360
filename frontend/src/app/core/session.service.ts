import { Injectable } from '@angular/core';
import { AccountInfo } from '@azure/msal-browser';
import { MsalService } from '@azure/msal-angular';

/**
 * Acceso centralizado a la cuenta y a los roles que vienen en el ID token.
 *
 * Los roles los asigna Microsoft Entra ID (claim "roles"). El backend sigue
 * validando el token y el scope; esto solo decide qué mostrar en pantalla.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {

  constructor(private readonly msalService: MsalService) {}

  get cuenta(): AccountInfo | null {
    const activa = this.msalService.instance.getActiveAccount();
    if (activa) {
      return activa;
    }
    const cuentas = this.msalService.instance.getAllAccounts();
    return cuentas.length > 0 ? cuentas[0] : null;
  }

  get roles(): string[] {
    const claims = this.cuenta?.idTokenClaims as Record<string, unknown> | undefined;
    const roles = claims ? claims['roles'] : undefined;
    return Array.isArray(roles) ? (roles as string[]) : [];
  }

  get nombre(): string {
    return this.cuenta?.name || this.usuario || '';
  }

  get usuario(): string {
    return this.cuenta?.username || '';
  }

  get esAdmin(): boolean {
    return this.roles.includes('Admin');
  }

  get esOperador(): boolean {
    return this.roles.includes('Operador');
  }

  get esCliente(): boolean {
    return this.roles.includes('Cliente');
  }

  /** Admin y Operador pueden ver reportería y cambiar estados de pedidos. */
  get esStaff(): boolean {
    return this.esAdmin || this.esOperador;
  }

  get rolPrincipal(): string {
    if (this.esAdmin) {
      return 'Admin';
    }
    if (this.esOperador) {
      return 'Operador';
    }
    if (this.esCliente) {
      return 'Cliente';
    }
    return 'Sin rol';
  }

  get rolBadgeClass(): string {
    if (this.rolPrincipal === 'Sin rol') {
      return 'badge-plain';
    }
    return `badge-${this.rolPrincipal.toLowerCase()}`;
  }

  get iniciales(): string {
    const texto = this.nombre || this.usuario;
    if (!texto) {
      return '?';
    }
    const partes = texto.replace(/[^\p{L}\p{N} ]/gu, ' ').trim().split(/\s+/);
    if (partes.length === 1) {
      return partes[0].slice(0, 2).toUpperCase();
    }
    return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase();
  }
}
