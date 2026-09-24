import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { DashboardComponent } from './dashboard/dashboard.component';
import { OrdersComponent } from './orders/orders.component';
import { CatalogComponent } from './catalog/catalog.component';

export const routes: Routes = [
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [MsalGuard]
  },
  {
    path: 'orders',
    component: OrdersComponent,
    canActivate: [MsalGuard]
  },
  {
    path: 'catalog',
    component: CatalogComponent,
    canActivate: [MsalGuard]
  }
];