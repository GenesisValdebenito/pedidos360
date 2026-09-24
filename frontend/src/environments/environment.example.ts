export const environment = {
  production: false,
  clientId: 'YOUR_MICROSOFT_APP_CLIENT_ID',
  authority: 'https://login.microsoftonline.com/YOUR_TENANT_ID',
  redirectUri: 'http://localhost:4200',
  apiScope: 'api://YOUR_API_APP_ID/Pedidos.Read',
  apiUrl: 'https://YOUR_API_GATEWAY_URL.execute-api.us-east-1.amazonaws.com'
};
