/**
 * Valores base compartidos entre las variantes de environment.
 *
 * Este archivo contiene SOLO placeholders: nunca valores reales de tenant,
 * client id ni URLs de API Gateway. Los valores reales viven en
 * environment.ts (local, no versionado) y se generan copiando
 * environment.example.ts.
 */
export const environmentBase = {
  production: false,
  clientId: 'YOUR_CLIENT_ID',
  authority: 'https://login.microsoftonline.com/YOUR_TENANT_ID',
  redirectUri: 'http://localhost:4200',
  apiScope: 'api://YOUR_API_CLIENT_ID/Pedidos.Read',
  gatewayUrl: 'https://YOUR_API_GATEWAY_URL'
};
