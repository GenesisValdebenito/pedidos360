import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MsalService } from '@azure/msal-angular';
import { environment } from '../../environments/environment';

@Component({
    selector: 'app-catalog',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div style="padding: 20px; border: 1px solid #ccc; border-radius: 6px; margin-top: 20px;">
      <h2>Catálogo de Productos</h2>

      <div *ngIf="isAdmin" style="margin-bottom: 20px; padding: 12px; background: #f0fff4; border-radius: 4px;">
        <h3>{{ editando ? 'Editar producto' : 'Crear nuevo producto' }}</h3>
        <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
          <label>
            Nombre:
            <input type="text" [(ngModel)]="form.name" style="margin-left: 6px; padding: 4px;" />
          </label>
          <label>
            Precio:
            <input type="number" [(ngModel)]="form.price" style="margin-left: 6px; width: 80px; padding: 4px;" />
          </label>
          <label>
            Stock:
            <input type="number" [(ngModel)]="form.stock" style="margin-left: 6px; width: 80px; padding: 4px;" />
          </label>
          <button (click)="guardarProducto()" style="padding: 6px 14px; background: #107c10; color: white; border: none; border-radius: 4px; cursor: pointer;">
            {{ editando ? 'Guardar cambios' : 'Crear producto' }}
          </button>
          <button *ngIf="editando" (click)="cancelarEdicion()" style="padding: 6px 14px; cursor: pointer;">
            Cancelar
          </button>
        </div>
        <p *ngIf="mensajeForm" style="margin-top: 8px; color: green;">{{ mensajeForm }}</p>
        <p *ngIf="errorForm" style="margin-top: 8px; color: red;">{{ errorForm }}</p>
      </div>

      <div *ngIf="cargando">Cargando productos...</div>
      <div *ngIf="error" style="color: red;">{{ error }}</div>

      <table *ngIf="!cargando && products.length > 0" style="width: 100%; border-collapse: collapse;">
        <thead>
          <tr style="background: #f0f0f0;">
            <th style="border: 1px solid #ddd; padding: 8px;">ID</th>
            <th style="border: 1px solid #ddd; padding: 8px;">Nombre</th>
            <th style="border: 1px solid #ddd; padding: 8px;">Precio</th>
            <th style="border: 1px solid #ddd; padding: 8px;">Stock</th>
            <th *ngIf="isAdmin" style="border: 1px solid #ddd; padding: 8px;">Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let product of products">
            <td style="border: 1px solid #ddd; padding: 8px;">{{ product.id }}</td>
            <td style="border: 1px solid #ddd; padding: 8px;">{{ product.name }}</td>
            <td style="border: 1px solid #ddd; padding: 8px;">{{ product.price }}</td>
            <td style="border: 1px solid #ddd; padding: 8px;">{{ product.stock }}</td>
            <td *ngIf="isAdmin" style="border: 1px solid #ddd; padding: 8px;">
              <button (click)="editarProducto(product)" style="padding: 4px 10px; cursor: pointer;">
                Editar
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <p *ngIf="!cargando && products.length === 0 && !error">No hay productos disponibles.</p>
    </div>
  `
})
export class CatalogComponent implements OnInit {
    products: any[] = []
    cargando = true;
    error = '';
    isAdmin = false;

    editando = false;
    editandoId: number | null = null;
    form = { name: '', price: 0, stock: 0 };
    mensajeForm = '';
    errorForm = '';

    constructor(private http: HttpClient, private msalService: MsalService) { }

    ngOnInit(): void {
        const account = this.msalService.instance.getActiveAccount()
            || this.msalService.instance.getAllAccounts()[0];

        if (account?.idTokenClaims) {
            const claims = account.idTokenClaims as Record<string, any>;
            const roles: string[] = Array.isArray(claims['roles']) ? claims['roles'] : [];
            this.isAdmin = roles.includes('Admin');
        }

        this.cargarProductos();
    }

    cargarProductos(): void {
        this.cargando = true;
        this.http.get<any[]>(`${environment.apiUrl}/api/catalog/products`).subscribe({
            next: (data) => {
                this.products = data;
                this.cargando = false;
            },
            error: (err) => {
                this.error = `Error al cargar productos: ${err.status} ${err.statusText}`;
                this.cargando = false;
            }
        });
    }

    guardarProducto(): void {
        this.mensajeForm = '';
        this.errorForm = '';

        if (this.editando && this.editandoId !== null) {
            this.http.put<any>(`${environment.apiUrl}/api/catalog/products/${this.editandoId}`, this.form).subscribe({
                next: () => {
                    this.mensajeForm = 'Producto actualizado correctamente.';
                    this.cancelarEdicion();
                    this.cargarProductos();
                },
                error: (err) => {
                    this.errorForm = `Error al actualizar: ${err.status} ${err.statusText}`;
                }
            });
        } else {
            this.http.post<any>(`${environment.apiUrl}/api/catalog/products`, this.form).subscribe({
                next: () => {
                    this.mensajeForm = 'Producto creado correctamente.';
                    this.form = { name: '', price: 0, stock: 0 };
                    this.cargarProductos();
                },
                error: (err) => {
                    this.errorForm = `Error al crear producto: ${err.status} ${err.statusText}`;
                }
            });
        }
    }

    editarProducto(product: any): void {
        this.editando = true;
        this.editandoId = product.id;
        this.form = { name: product.name, price: product.price, stock: product.stock };
        this.mensajeForm = '';
        this.errorForm = '';
    }

    cancelarEdicion(): void {
        this.editando = false;
        this.editandoId = null;
        this.form = { name: '', price: 0, stock: 0 };
    }
}