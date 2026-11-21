/**
 * Plantilla de configuración del frontend.
 *
 * Cómo usarla (entrega reproducible sin secretos):
 *   1. Copiar este archivo a src/environments/environment.ts
 *   2. Reemplazar los placeholders YOUR_* por los valores reales.
 *
 * environment.ts está excluido del repositorio (.gitignore), por lo que
 * nunca se publican credenciales ni URLs internas.
 */
import { environmentBase } from './environment.shared';

export const environment = {
  ...environmentBase,
  // URL base del API Gateway (SIN el sufijo /api: el interceptor y los
  // componentes ya agregan /api en cada llamado).
  apiUrl: environmentBase.gatewayUrl
};
