import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { SessionService } from '../core/session.service';

interface Kpi {
  pedidosCreados: number;
  unidadesSolicitadas: number;
  pedidosCancelados: number;
  pedidosEntregados: number;
  pedidosPorEstado: Record<string, number>;
  tasaCancelacionPorcentaje: number;
  promedioUnidadesPorPedido: number;
  ultimaActualizacion: string;
}

interface EventoAuditoria {
  pedidoId: number;
  estado: string;
  cliente: string;
  productoId: number;
  cantidad: number;
  timestamp: string;
  recibidoEn: string;
}

interface StockActual {
  productoId: number;
  nombre: string;
  stock: number;
  precio: number;
  actualizadoEn: string;
}

interface OffsetParticion {
  topico: string;
  particion: number;
  offsetActual: number;
  offsetFinal: number;
  lag: number;
}

interface GrupoKafka {
  grupo: string;
  estado: string;
  miembros: number;
  offsets: OffsetParticion[];
}

interface Pedido {
  id: number;
  cliente: string;
  estado: string;
  productoId: number;
  cantidad: number;
}

interface FilaEstado {
  estado: string;
  total: number;
  porcentaje: number;
}

/**
 * Dashboard con dos vistas:
 * - Admin/Operador: KPIs, auditoría y stock calculados desde Kafka.
 * - Cliente: resumen de sus propios pedidos y estado actual.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {

  esStaff = false;
  esAdmin = false;
  esCliente = false;

  kpis: Kpi | null = null;
  auditoria: EventoAuditoria[] = [];
  stock: StockActual[] = [];
  grupos: GrupoKafka[] = [];
  pedidos: Pedido[] = [];

  cargando = true;
  error = '';
  ultimaActualizacion: Date | null = null;

  private temporizador?: ReturnType<typeof setInterval>;
  private readonly intervaloMs = 8000;
  private readonly alEnfocar = () => this.cargar(true);

  constructor(
    private readonly http: HttpClient,
    private readonly session: SessionService
  ) {}

  ngOnInit(): void {
    this.esStaff = this.session.esStaff;
    this.esAdmin = this.session.esAdmin;
    this.esCliente = this.session.esCliente;

    this.cargar();
    this.temporizador = setInterval(() => this.cargar(true), this.intervaloMs);
    window.addEventListener('focus', this.alEnfocar);
  }

  ngOnDestroy(): void {
    if (this.temporizador) {
      clearInterval(this.temporizador);
    }
    window.removeEventListener('focus', this.alEnfocar);
  }

  cargar(silencioso = false): void {
    if (!silencioso) {
      this.cargando = true;
    }
    this.error = '';

    if (this.esStaff) {
      this.cargarReportes();
    } else {
      this.cargarMisPedidos();
    }
  }

  private cargarReportes(): void {
    const grupos$ = this.esAdmin
      ? this.http.get<GrupoKafka[]>(`${environment.apiUrl}/api/kafka/grupos`)
      : of([] as GrupoKafka[]);

    forkJoin({
      kpis: this.http.get<Kpi>(`${environment.apiUrl}/api/reportes/kpis`),
      auditoria: this.http.get<EventoAuditoria[]>(`${environment.apiUrl}/api/reportes/auditoria?limite=25`),
      stock: this.http.get<StockActual[]>(`${environment.apiUrl}/api/reportes/stock`),
      grupos: grupos$
    }).subscribe({
      next: (datos) => {
        this.kpis = datos.kpis;
        this.auditoria = datos.auditoria;
        this.stock = datos.stock;
        this.grupos = datos.grupos;
        this.ultimaActualizacion = new Date();
        this.cargando = false;
      },
      error: (err) => {
        this.error = this.mensajeError(err);
        this.cargando = false;
      }
    });
  }

  private cargarMisPedidos(): void {
    this.http.get<Pedido[]>(`${environment.apiUrl}/api/orders`).subscribe({
      next: (datos) => {
        this.pedidos = datos;
        this.ultimaActualizacion = new Date();
        this.cargando = false;
      },
      error: (err) => {
        this.error = this.mensajeError(err);
        this.cargando = false;
      }
    });
  }

  private mensajeError(err: { status?: number }): string {
    if (err.status === 403) {
      return 'Tu cuenta no tiene permisos para ver esta información.';
    }
    if (err.status === 0) {
      return 'No se pudo conectar con el backend. Revisa que EC2 y el proxy estén activos.';
    }
    return `No se pudo cargar la información (${err.status || 'sin conexión'}).`;
  }

  get estados(): FilaEstado[] {
    const orden = ['CREADO', 'ACEPTADO', 'EN_PREPARACION', 'DESPACHADO', 'ENTREGADO', 'CANCELADO'];
    const mapa = this.kpis?.pedidosPorEstado ?? {};
    const filas = orden.map((estado) => ({ estado, total: mapa[estado] ?? 0 }));
    const maximo = Math.max(1, ...filas.map((fila) => fila.total));
    return filas.map((fila) => ({
      ...fila,
      porcentaje: Math.round((fila.total / maximo) * 100)
    }));
  }

  get misPedidosRecientes(): Pedido[] {
    return [...this.pedidos].sort((a, b) => b.id - a.id).slice(0, 8);
  }

  get pedidosEnCurso(): number {
    return this.pedidos.filter((p) =>
      ['ACEPTADO', 'EN_PREPARACION', 'DESPACHADO'].includes(p.estado)
    ).length;
  }

  get pedidosPendientes(): number {
    return this.pedidos.filter((p) => p.estado === 'CREADO').length;
  }

  get pedidosEntregadosCliente(): number {
    return this.pedidos.filter((p) => p.estado === 'ENTREGADO').length;
  }

  get pedidosCanceladosCliente(): number {
    return this.pedidos.filter((p) => p.estado === 'CANCELADO').length;
  }

  totalLag(grupo: GrupoKafka): number {
    return grupo.offsets.reduce((total, offset) => total + offset.lag, 0);
  }

  etiquetaEstado(estado: string): string {
    return (estado || '').replace(/_/g, ' ');
  }

  claseEstado(estado: string): string {
    return `badge-${(estado || '').toLowerCase()}`;
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
}
