import { TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { MsalBroadcastService, MsalService, MSAL_GUARD_CONFIG } from '@azure/msal-angular';
import { EventMessage } from '@azure/msal-browser';

import { AppComponent } from './app.component';
import { environment } from '../environments/environment';

describe('AppComponent', () => {
  const msalServiceStub = {
    handleRedirectObservable: () => of(null),
    loginRedirect: jasmine.createSpy('loginRedirect'),
    logoutRedirect: jasmine.createSpy('logoutRedirect'),
    instance: {
      getAllAccounts: () => [],
      getActiveAccount: () => null,
      setActiveAccount: jasmine.createSpy('setActiveAccount')
    }
  };

  const msalBroadcastServiceStub = {
    msalSubject$: new Subject<EventMessage>()
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        {
          provide: MSAL_GUARD_CONFIG,
          useValue: {
            interactionType: 'redirect',
            authRequest: { scopes: [environment.apiScope] }
          }
        },
        { provide: MsalService, useValue: msalServiceStub },
        { provide: MsalBroadcastService, useValue: msalBroadcastServiceStub }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should initialize without logged user', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.loginDisplay).toBeFalse();
    expect(fixture.componentInstance.userName).toBe('');
  });

  it('should call loginRedirect with the API scope', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    fixture.componentInstance.login();

    expect(msalServiceStub.loginRedirect).toHaveBeenCalledWith({
      scopes: [environment.apiScope]
    });
  });

  it('should call logoutRedirect with the configured redirect URI', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    fixture.componentInstance.logout();

    expect(msalServiceStub.logoutRedirect).toHaveBeenCalledWith({
      postLogoutRedirectUri: environment.redirectUri
    });
  });
});
