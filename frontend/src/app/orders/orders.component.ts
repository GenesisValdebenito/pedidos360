import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MsalService } from '@azure/msal-angular';
import { environment } from '../../environments/environment';

@Component({
    selector: 'app-orders',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div style="padding: 20px; border: 1px solid #ccc; border-radius: 6px; margin-top: 20px;">
      <h2>Pedidos</h2>

      <div *ngIf="isCliente" style="margin-bottom: 20px; padding: 12px; background: #f0f4ff; border-radius: 4px;">
        <h3>Crear nuevo pedido</h3>
        <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
          <label>
            Producto ID:
            <input type="number" [(ngModel)]="nuevoProductoId" style="margin-left: 6px; width: 80px; padding: 4px;" />
          </label>
          <label>
            Cantidad:
            <input type="number" [(ngModel)]="nuevaCantidad" style="margin-left: 6px; width: 80px; padding: 4px;" />
          </label>
          <button (click)="crearPedido()" style="padding: 6px 14px; background: #0078d4; color: white; border: none; border-radius: 4px; cursor: pointer;">
            Crear pedido
          </button>
        </div>
        <p *ngIf="mensajeCrear" style="margin-top: 8px; color: green;">{{ mensajeCrear }}</p>
        <p *ngIf="errorCrear" style="margin-top: 8px; color: red;">{{ errorCrear }}</p>
      </div>

      <div *ngIf="cargando">Cargando pedidos...</div>
      <div *ngIf="error" style="color: red;">{{ error }}</div>

      <table *ngIf="!cargando && orders.length > 0" style="width: 100%; border-collapse: collapse;">
        <thead>
          <tr style="background: #f0f0f0;">
            <th style="border: 1px solid #ddd; padding: 8px;">ID</th>
            <th style="border: 1px solid #ddd; padding: 8px;">Cliente</th>
            <th style="border: 1px solid #ddd; padding: 8px;">Producto ID</th>
            <th style="border: 1px solid #ddd; padding: 8px;">Cantidad</th>
            <th style="border: 1px solid #ddd; padding: 8px;">Estado</th>
            <th *ngIf="isAdminOOperador" style="border: 1px solid #ddd; padding: 8px;">Cambiar estado</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let order of orders">
            <td style="border: 1px solid #ddd; padding: 8px;">{{ order.id }}</td>
            <td style="border: 1px solid #ddd; padding: 8px;">{{ order.cliente }}</td>
            <td style="border: 1px solid #ddd; padding: 8px;">{{ order.productoId }}</td>
            <td style="border: 1px solid #ddd; padding: 8px;">{{ order.cantidad }}</td>
            <td style="border: 1px solid #ddd; padding: 8px;">{{ order.estado }}</td>
            <td *ngIf="isAdminOOperador" style="border: 1px solid #ddd; padding: 8px;">
              <select [(ngModel)]="order.nuevoEstado" style="padding: 4px;">
                <option value="CREADO">CREADO</option>
                <option value="ACEPTADO">ACEPTADO</option>
                <option value="EN_PREPARACION">EN_PREPARACION</option>
                <option value="DESPACHADO">DESPACHADO</option>
                <option value="ENTREGADO">ENTREGADO</option>
                <option value="CANCELADO">CANCELADO</option>
              </select>
              <button (click)="cambiarEstado(order)" style="margin-left: 6px; padding: 4px 10px; cursor: pointer;">
                Actualizar
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <p *ngIf="!cargando && orders.length === 0 && !error">No hay pedidos disponibles.</p>
    </div>
  `
})
export class OrdersComponent implements OnInit {
    orders: any[] = [];
    cargando = true;
    error = '';
    isAdminOOperador = false;
    isCliente = false;

    nuevoProductoId: number = 1;
    nuevaCantidad: number = 1;
    mensajeCrear = '';
    errorCrear = '';

    constructor(private http: HttpClient, private msalService: MsalService) { }

    ngOnInit(): void {
        const account = this.msalService.instance.getActiveAccount()
            || this.msalService.instance.getAllAccounts()[0];

        if (account?.idTokenClaims) {
            const claims = account.idTokenClaims as Record<string, any>;
            const roles: string[] = Array.isArray(claims['roles']) ? claims['roles'] : [];
            this.isAdminOOperador = roles.includes('Admin') || roles.includes('Operador');
            this.isCliente = roles.includes('Cliente');
        }

        this.cargarPedidos();
    }

    cargarPedidos(): void {
        this.cargando = true;
        this.http.get<any[]>(`${environment.apiUrl}/api/orders`).subscribe({
            next: (data) => {
                this.orders = data.map(o => ({ ...o, nuevoEstado: o.estado }));
                this.cargando = false;
            },
            error: (err) => {
                this.error = `Error al cargar pedidos: ${err.status} ${err.statusText}`;
                this.cargando = false;
            }
        });
    }

    crearPedido(): void {
        this.mensajeCrear = '';
        this.errorCrear = '';
        const body = { productoId: this.nuevoProductoId, cantidad: this.nuevaCantidad };
        this.http.post<any>(`${environment.apiUrl}/api/orders`, body).subscribe({
            next: () => {
                this.mensajeCrear = 'Pedido creado correctamente.';
                this.cargarPedidos();
            },
            error: (err) => {
                this.errorCrear = `Error al crear pedido: ${err.status} ${err.statusText}`;
            }
        });
    }

    cambiarEstado(order: any): void {
        this.http.put<any>(`${environment.apiUrl}/api/orders/${order.id}/status`, { estado: order.nuevoEstado }).subscribe({
            next: () => {
                order.estado = order.nuevoEstado;
            },
            error: (err) => {
                alert(`Error al cambiar estado: ${err.status} ${err.statusText}`);
            }
        });
    }
}