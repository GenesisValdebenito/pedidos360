import { Component, OnInit, OnDestroy, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import {
  MsalService,
  MsalBroadcastService,
  MSAL_GUARD_CONFIG,
  MsalGuardConfiguration
} from '@azure/msal-angular';
import {
  EventMessage,
  EventType,
  AuthenticationResult,
  RedirectRequest
} from '@azure/msal-browser';
import { environment } from '../environments/environment';
import { SessionService } from './core/session.service';

type Tema = 'claro' | 'oscuro';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'pedidos360-frontend';
  loginDisplay = false;
  userName = '';
  tema: Tema = 'claro';

  private readonly _destroying$ = new Subject<void>();

  constructor(
    @Inject(MSAL_GUARD_CONFIG) private msalGuardConfig: MsalGuardConfiguration,
    private authService: MsalService,
    private msalBroadcastService: MsalBroadcastService,
    public session: SessionService
  ) {
    this.inicializarTema();
  }

  ngOnInit(): void {
    // Procesa el retorno del redirect de login de forma asíncrona
    this.authService.handleRedirectObservable().subscribe();

    this.setLoginDisplay();

    this.msalBroadcastService.msalSubject$
      .pipe(
        filter(
          (msg: EventMessage) =>
            msg.eventType === EventType.LOGIN_SUCCESS ||
            msg.eventType === EventType.ACQUIRE_TOKEN_SUCCESS ||
            msg.eventType === EventType.ACTIVE_ACCOUNT_CHANGED
        ),
        takeUntil(this._destroying$)
      )
      .subscribe((result: EventMessage) => {
        if (result.payload) {
          const payload = result.payload as AuthenticationResult;
          if (payload && payload.account) {
            this.authService.instance.setActiveAccount(payload.account);
          }
        }
        this.setLoginDisplay();
      });

    this.msalBroadcastService.msalSubject$
      .pipe(
        filter((msg: EventMessage) => msg.eventType === EventType.LOGOUT_SUCCESS),
        takeUntil(this._destroying$)
      )
      .subscribe(() => {
        this.setLoginDisplay();
      });
  }

  setLoginDisplay(): void {
    const accounts = this.authService.instance.getAllAccounts();
    this.loginDisplay = accounts.length > 0;
    if (this.loginDisplay) {
      const activeAccount = this.authService.instance.getActiveAccount() || accounts[0];
      if (!this.authService.instance.getActiveAccount()) {
        this.authService.instance.setActiveAccount(activeAccount);
      }
      this.userName = activeAccount.name || activeAccount.username || '';
    } else {
      this.userName = '';
    }
  }

  get esTemaOscuro(): boolean {
    return this.tema === 'oscuro';
  }

  alternarTema(): void {
    this.tema = this.tema === 'claro' ? 'oscuro' : 'claro';
    localStorage.setItem('pedidos360-tema', this.tema);
    this.aplicarTema();
  }

  private inicializarTema(): void {
    const guardado = localStorage.getItem('pedidos360-tema');
    if (guardado === 'claro' || guardado === 'oscuro') {
      this.tema = guardado;
    } else {
      const prefiereOscuro = window.matchMedia
        && window.matchMedia('(prefers-color-scheme: dark)').matches;
      this.tema = prefiereOscuro ? 'oscuro' : 'claro';
    }
    this.aplicarTema();
  }

  private aplicarTema(): void {
    document.documentElement.setAttribute('data-theme', this.tema);
  }

  login(): void {
    const authRequest: RedirectRequest = {
      scopes: [environment.apiScope]
    };
    this.authService.loginRedirect(authRequest);
  }

  logout(): void {
    this.authService.logoutRedirect({
      postLogoutRedirectUri: environment.redirectUri
    });
  }

  ngOnDestroy(): void {
    this._destroying$.next(undefined);
    this._destroying$.complete();
  }
}
