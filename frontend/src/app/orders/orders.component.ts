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

interface Pedido {
  id: number;
  cliente: string;
  estado: string;
  productoId: number;
  cantidad: number;
  nuevoEstado?: string;
}

interface Toast {
  id: number;
  texto: string;
  tipo: 'info' | 'success' | 'error';
}

/**
 * Página de pedidos.
 *
 * - Cliente: crea pedidos desde el catálogo y puede cancelar mientras estén CREADO.
 * - Admin/Operador: ven todos los pedidos y avanzan el estado.
 *
 * Se refresca cada 8 segundos para que los cambios de otros usuarios se vean
 * sin recargar la página. El botón de crear se bloquea mientras hay una
 * petición en curso para evitar pedidos duplicados.
 */
@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.css'
})
export class OrdersComponent implements OnInit, OnDestroy {

  pedidos: Pedido[] = [];
  productos: Producto[] = [];

  esStaff = false;
  esCliente = false;

  cargando = true;
  error = '';
  ultimaActualizacion: Date | null = null;

  filtro = '';

  nuevoProductoId: number | null = null;
  nuevaCantidad = 1;
  creando = false;
  mensajeCrear = '';
  errorCrear = '';

  cambiandoId: number | null = null;
  cancelandoId: number | null = null;

  toasts: Toast[] = [];

  private readonly estadosPrevios = new Map<number, string>();
  private temporizador?: ReturnType<typeof setInterval>;
  private readonly intervaloMs = 8000;
  private readonly alEnfocar = () => this.refrescar(true);
  private readonly temporizadoresToast: Array<ReturnType<typeof setTimeout>> = [];

  constructor(
    private readonly http: HttpClient,
    private readonly session: SessionService
  ) {}

  ngOnInit(): void {
    this.esStaff = this.session.esStaff;
    this.esCliente = this.session.esCliente;

    this.refrescar();
    this.temporizador = setInterval(() => this.refrescar(true), this.intervaloMs);
    window.addEventListener('focus', this.alEnfocar);
  }

  ngOnDestroy(): void {
    if (this.temporizador) {
      clearInterval(this.temporizador);
    }
    window.removeEventListener('focus', this.alEnfocar);
    this.temporizadoresToast.forEach((t) => clearTimeout(t));
  }

  get productosFiltrados(): Producto[] {
    return this.productos;
  }

  get pedidosFiltrados(): Pedido[] {
    const texto = this.filtro.trim().toLowerCase();
    if (!texto) {
      return this.pedidos;
    }
    return this.pedidos.filter((pedido) => {
      return String(pedido.id).includes(texto)
        || pedido.estado.toLowerCase().includes(texto)
        || (pedido.cliente || '').toLowerCase().includes(texto)
        || this.nombreProducto(pedido.productoId).toLowerCase().includes(texto);
    });
  }

  get productoSel(): Producto | null {
    if (this.nuevoProductoId === null) {
      return null;
    }
    return this.productos.find((p) => p.id === Number(this.nuevoProductoId)) || null;
  }

  get totalEstimado(): number {
    const producto = this.productoSel;
    return producto ? producto.price * (this.nuevaCantidad || 0) : 0;
  }

  get puedeCrear(): boolean {
    if (this.creando || !this.nuevoProductoId || !this.nuevaCantidad || this.nuevaCantidad < 1) {
      return false;
    }
    const producto = this.productoSel;
    if (producto && producto.stock < this.nuevaCantidad) {
      return false;
    }
    return true;
  }

  refrescar(silencioso = false): void {
    if (!silencioso) {
      this.cargando = true;
    }
    this.error = '';
    this.cargarProductos();
    this.cargarPedidos(silencioso);
  }

  cargarProductos(): void {
    this.http.get<Producto[]>(`${environment.apiUrl}/api/catalog/products`).subscribe({
      next: (datos) => {
        this.productos = datos;
        if (this.nuevoProductoId === null && datos.length > 0) {
          this.nuevoProductoId = datos[0].id;
        }
        if (this.nuevoProductoId !== null && !datos.some((p) => p.id === this.nuevoProductoId)) {
          this.nuevoProductoId = datos.length > 0 ? datos[0].id : null;
        }
      },
      error: () => {
        // El catálogo es secundario: si falla, los pedidos igual se muestran.
      }
    });
  }

  cargarPedidos(silencioso = false): void {
    this.http.get<Pedido[]>(`${environment.apiUrl}/api/orders`).subscribe({
      next: (datos) => {
        this.notificarCambios(datos);
        this.pedidos = datos.map((pedido) => {
          const anterior = this.pedidos.find((p) => p.id === pedido.id);
          const opciones = this.siguientesEstados(pedido.estado);
          const seleccion = anterior?.nuevoEstado;
          return {
            ...pedido,
            nuevoEstado: seleccion && opciones.includes(seleccion)
              ? seleccion
              : (opciones[0] || pedido.estado)
          };
        });
        this.estadosPrevios.clear();
        datos.forEach((pedido) => this.estadosPrevios.set(pedido.id, pedido.estado));
        this.ultimaActualizacion = new Date();
        this.cargando = false;
      },
      error: (err) => {
        this.error = this.mensajeError(err, 'cargar los pedidos');
        this.cargando = false;
      }
    });
  }

  crearPedido(): void {
    // Doble protección: el botón se deshabilita y este guard detiene dobles clics.
    if (this.creando || !this.puedeCrear) {
      return;
    }

    this.mensajeCrear = '';
    this.errorCrear = '';
    const producto = this.productoSel;
    if (producto && producto.stock < this.nuevaCantidad) {
      this.errorCrear = `Solo hay ${producto.stock} unidades disponibles de ${producto.name}.`;
      return;
    }

    this.creando = true;
    const body = {
      productoId: Number(this.nuevoProductoId),
      cantidad: this.nuevaCantidad
    };

    this.http.post<Pedido>(`${environment.apiUrl}/api/orders`, body).subscribe({
      next: (pedido) => {
        this.mensajeCrear = `Pedido #${pedido.id} creado correctamente.`;
        this.nuevaCantidad = 1;
        const opciones = this.siguientesEstados(pedido.estado);
        this.pedidos = [{
          ...pedido,
          nuevoEstado: opciones[0] || pedido.estado
        }, ...this.pedidos];
        this.estadosPrevios.set(pedido.id, pedido.estado);
        this.creando = false;
        this.refrescar(true);
      },
      error: (err) => {
        this.errorCrear = this.mensajeError(err, 'crear el pedido');
        this.creando = false;
      }
    });
  }

  cambiarEstado(pedido: Pedido): void {
    if (this.cambiandoId !== null || !pedido.nuevoEstado) {
      return;
    }
    const destino = pedido.nuevoEstado;
    this.cambiandoId = pedido.id;

    this.http.put<Pedido>(`${environment.apiUrl}/api/orders/${pedido.id}/status`, { estado: destino })
      .subscribe({
        next: () => {
          pedido.estado = destino;
          const opciones = this.siguientesEstados(destino);
          pedido.nuevoEstado = opciones[0] || destino;
          this.cambiandoId = null;
          this.refrescar(true);
        },
        error: (err) => {
          this.cambiandoId = null;
          const mensaje = err.status === 400
            ? 'Transición no permitida o stock insuficiente. Revisa el estado del pedido.'
            : this.mensajeError(err, 'cambiar el estado');
          this.agregarToast(mensaje, 'error');
        }
      });
  }

  cancelar(pedido: Pedido): void {
    if (this.cancelandoId !== null) {
      return;
    }
    if (!window.confirm(`¿Cancelar el pedido #${pedido.id}? Esta acción no se puede deshacer.`)) {
      return;
    }

    this.cancelandoId = pedido.id;
    this.http.put<Pedido>(`${environment.apiUrl}/api/orders/${pedido.id}/cancelar`, {})
      .subscribe({
        next: () => {
          pedido.estado = 'CANCELADO';
          pedido.nuevoEstado = 'CANCELADO';
          this.cancelandoId = null;
          this.refrescar(true);
        },
        error: (err) => {
          this.cancelandoId = null;
          let mensaje = this.mensajeError(err, 'cancelar el pedido');
          if (err.status === 400) {
            mensaje = 'Solo puedes cancelar un pedido mientras está en estado CREADO.';
          }
          if (err.status === 404) {
            mensaje = 'El pedido no existe o no pertenece a tu cuenta.';
          }
          this.agregarToast(mensaje, 'error');
        }
      });
  }

  siguientesEstados(estado: string): string[] {
    switch (estado) {
      case 'CREADO':
        return ['ACEPTADO', 'CANCELADO'];
      case 'ACEPTADO':
        return ['EN_PREPARACION'];
      case 'EN_PREPARACION':
        return ['DESPACHADO'];
      case 'DESPACHADO':
        return ['ENTREGADO'];
      default:
        return [];
    }
  }

  nombreProducto(productoId: number): string {
    const producto = this.productos.find((p) => p.id === productoId);
    return producto ? producto.name : `Producto #${productoId}`;
  }

  etiquetaEstado(estado: string): string {
    return (estado || '').replace(/_/g, ' ');
  }

  claseEstado(estado: string): string {
    return `badge-${(estado || '').toLowerCase()}`;
  }

  formatearPrecio(valor: number): string {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      maximumFractionDigits: 0
    }).format(valor ?? 0);
  }

  cerrarToast(id: number): void {
    this.toasts = this.toasts.filter((t) => t.id !== id);
  }

  private notificarCambios(datos: Pedido[]): void {
    if (this.estadosPrevios.size === 0) {
      return;
    }

    datos.forEach((pedido) => {
      const anterior = this.estadosPrevios.get(pedido.id);
      if (anterior && anterior !== pedido.estado) {
        const texto = this.esCliente
          ? `Tu pedido #${pedido.id} cambió de ${this.etiquetaEstado(anterior)} a ${this.etiquetaEstado(pedido.estado)}.`
          : `Pedido #${pedido.id}: ${this.etiquetaEstado(anterior)} → ${this.etiquetaEstado(pedido.estado)}.`;
        this.agregarToast(texto, pedido.estado === 'CANCELADO' ? 'error' : 'info');
      } else if (!anterior && this.esStaff) {
        this.agregarToast(`Nuevo pedido #${pedido.id} de ${pedido.cliente}.`, 'success');
      }
    });
  }

  private agregarToast(texto: string, tipo: Toast['tipo']): void {
    const id = Date.now() + Math.random();
    this.toasts = [...this.toasts, { id, texto, tipo }];
    const temporizador = setTimeout(() => this.cerrarToast(id), 8000);
    this.temporizadoresToast.push(temporizador);
  }

  private mensajeError(err: { status?: number; error?: unknown }, accion: string): string {
    if (err.status === 0) {
      return 'No se pudo conectar con el backend. Revisa que EC2 y el proxy estén activos.';
    }
    if (err.status === 401) {
      return 'Tu sesión expiró. Vuelve a iniciar sesión.';
    }
    if (err.status === 403) {
      return 'No tienes permisos para realizar esta acción.';
    }
    if (err.status === 503) {
      return `El servicio no está disponible para ${accion}. Intenta nuevamente en unos segundos.`;
    }
    return `No se pudo ${accion} (${err.status || 'sin conexión'}).`;
  }
}
