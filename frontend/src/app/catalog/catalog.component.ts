import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { SessionService } from '../core/session.service';

interface Producto {
  id: number;
  name: string;
  price: number;
  stock: number;
}

/**
 * Catálogo de productos.
 *
 * Admin puede crear y editar; todos los usuarios autenticados pueden verlo.
 * La lista se refresca cada 8 segundos para que los cambios de otro usuario
 * aparezcan sin recargar, y se avisa cuando se detecta una modificación.
 */
@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.css'
})
export class CatalogComponent implements OnInit, OnDestroy {

  productos: Producto[] = [];
  cargando = true;
  error = '';
  ultimaActualizacion: Date | null = null;
  filtro = '';
  cambioDetectado = false;

  esAdmin = false;

  editando = false;
  editandoId: number | null = null;
  form = { name: '', price: 0, stock: 0 };
  mensajeForm = '';
  errorForm = '';
  guardando = false;

  private firmaAnterior = '';
  private temporizador?: ReturnType<typeof setInterval>;
  private temporizadorAviso?: ReturnType<typeof setTimeout>;
  private readonly intervaloMs = 8000;
  private readonly alEnfocar = () => this.cargarProductos(true);

  constructor(
    private readonly http: HttpClient,
    private readonly session: SessionService
  ) {}

  ngOnInit(): void {
    this.esAdmin = this.session.esAdmin;
    this.cargarProductos();
    this.temporizador = setInterval(() => this.cargarProductos(true), this.intervaloMs);
    window.addEventListener('focus', this.alEnfocar);
  }

  ngOnDestroy(): void {
    if (this.temporizador) {
      clearInterval(this.temporizador);
    }
    if (this.temporizadorAviso) {
      clearTimeout(this.temporizadorAviso);
    }
    window.removeEventListener('focus', this.alEnfocar);
  }

  get productosFiltrados(): Producto[] {
    const texto = this.filtro.trim().toLowerCase();
    if (!texto) {
      return this.productos;
    }
    return this.productos.filter((producto) =>
      producto.name.toLowerCase().includes(texto)
      || String(producto.id).includes(texto)
    );
  }

  cargarProductos(silencioso = false): void {
    if (!silencioso) {
      this.cargando = true;
    }
    this.error = '';

    this.http.get<Producto[]>(`${environment.apiUrl}/api/catalog/products`).subscribe({
      next: (datos) => {
        const firma = JSON.stringify(datos);
        if (this.firmaAnterior && firma !== this.firmaAnterior) {
          this.mostrarAvisoCambio();
        }
        this.firmaAnterior = firma;
        this.productos = datos;
        this.ultimaActualizacion = new Date();
        this.cargando = false;
      },
      error: (err) => {
        this.error = err.status === 503
          ? 'El catálogo no está disponible en este momento. Intenta nuevamente más tarde.'
          : 'No se pudieron cargar los productos. Revisa la conexión con EC2.';
        this.cargando = false;
      }
    });
  }

  guardarProducto(): void {
    if (this.guardando) {
      return;
    }

    this.mensajeForm = '';
    this.errorForm = '';

    const nombre = (this.form.name || '').trim();
    if (!nombre) {
      this.errorForm = 'El nombre del producto es obligatorio.';
      return;
    }
    if (this.form.price <= 0) {
      this.errorForm = 'El precio debe ser mayor que cero.';
      return;
    }
    if (this.form.stock < 0) {
      this.errorForm = 'El stock no puede ser negativo.';
      return;
    }

    this.guardando = true;
    const body = { name: nombre, price: this.form.price, stock: this.form.stock };

    if (this.editando && this.editandoId !== null) {
      this.http.put<Producto>(`${environment.apiUrl}/api/catalog/products/${this.editandoId}`, body)
        .subscribe({
          next: () => {
            this.mensajeForm = 'Producto actualizado correctamente.';
            this.guardando = false;
            this.cancelarEdicion();
            this.cargarProductos(true);
          },
          error: (err) => {
            this.errorForm = `No se pudo actualizar el producto (${err.status || 'sin conexión'}).`;
            this.guardando = false;
          }
        });
    } else {
      this.http.post<Producto>(`${environment.apiUrl}/api/catalog/products`, body)
        .subscribe({
          next: (producto) => {
            this.mensajeForm = `Producto "${producto.name}" creado correctamente.`;
            this.form = { name: '', price: 0, stock: 0 };
            this.guardando = false;
            this.cargarProductos(true);
          },
          error: (err) => {
            this.errorForm = `No se pudo crear el producto (${err.status || 'sin conexión'}).`;
            this.guardando = false;
          }
        });
    }
  }

  editarProducto(producto: Producto): void {
    this.editando = true;
    this.editandoId = producto.id;
    this.form = { name: producto.name, price: producto.price, stock: producto.stock };
    this.mensajeForm = '';
    this.errorForm = '';
  }

  cancelarEdicion(): void {
    this.editando = false;
    this.editandoId = null;
    this.form = { name: '', price: 0, stock: 0 };
  }

  cerrarAviso(): void {
    this.cambioDetectado = false;
  }

  claseStock(stock: number): string {
    if (stock <= 5) {
      return 'stock stock-bajo';
    }
    if (stock <= 15) {
      return 'stock stock-medio';
    }
    return 'stock stock-ok';
  }

  formatearPrecio(valor: number): string {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      maximumFractionDigits: 0
    }).format(valor ?? 0);
  }

  private mostrarAvisoCambio(): void {
    this.cambioDetectado = true;
    if (this.temporizadorAviso) {
      clearTimeout(this.temporizadorAviso);
    }
    this.temporizadorAviso = setTimeout(() => this.cambioDetectado = false, 6000);
  }
}
