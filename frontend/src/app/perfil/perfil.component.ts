import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SessionService } from '../core/session.service';

interface ClaimFila {
  clave: string;
  valor: string;
}

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './perfil.component.html',
  styleUrl: './perfil.component.css'
})
export class PerfilComponent {

  constructor(public session: SessionService) {}

  get nombre(): string {
    return this.session.nombre || 'Usuario sin nombre';
  }

  get correo(): string {
    return this.session.usuario || '—';
  }

  get tenantId(): string {
    return this.claim('tid');
  }

  get objetoId(): string {
    return this.claim('oid');
  }

  get emisor(): string {
    return this.claim('iss');
  }

  get claims(): ClaimFila[] {
    const claims = this.session.cuenta?.idTokenClaims as Record<string, unknown> | undefined;
    if (!claims) {
      return [];
    }
    return Object.entries(claims)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([clave, valor]) => ({ clave, valor: this.formatear(valor) }));
  }

  private claim(nombre: string): string {
    const claims = this.session.cuenta?.idTokenClaims as Record<string, unknown> | undefined;
    const valor = claims ? claims[nombre] : undefined;
    return valor === undefined || valor === null ? '—' : String(valor);
  }

  private formatear(valor: unknown): string {
    if (valor === null || valor === undefined) {
      return '—';
    }
    if (Array.isArray(valor)) {
      return valor.join(', ');
    }
    if (typeof valor === 'object') {
      return JSON.stringify(valor);
    }
    return String(valor);
  }
}
