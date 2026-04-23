import { useAuthController } from '../controllers/useAuthController';

export function LoginView() {
  const controller = useAuthController();

  return (
    <main className="main login-main">
      <div className="grid-bg" />
      <div className="auth-container">
        <div className="auth-info">
          <div className="auth-badge">
            <span className="auth-badge-dot" />
            Procesamiento activo
          </div>
          <h1 className="auth-headline">
            Procesa imagenes
            <br />a <span>escala masiva</span>
          </h1>
          <p className="auth-desc">
            Plataforma para procesar lotes de imagenes de forma rapida y segura.
          </p>
        </div>

        <div className="auth-card">
          <div className="card-title">Bienvenido</div>
          <div className="card-sub">Gestiona tus lotes y resultados desde un solo lugar</div>

          <div className="tabs">
            <button className={`tab ${controller.tab === 'login' ? 'active' : ''}`} onClick={() => controller.setTab('login')} type="button">
              Iniciar sesion
            </button>
            <button className={`tab ${controller.tab === 'register' ? 'active' : ''}`} onClick={() => controller.setTab('register')} type="button">
              Registrarse
            </button>
          </div>

          {controller.tab === 'login' ? (
            <form onSubmit={controller.onLogin}>
              <div className="field-label">Correo</div>
              <input
                className="field"
                type="email"
                value={controller.loginEmail}
                onChange={(e) => controller.setLoginEmail(e.target.value)}
                required
              />
              <div className="field-label">Contrasena</div>
              <input
                className="field"
                type="password"
                value={controller.loginPassword}
                onChange={(e) => controller.setLoginPassword(e.target.value)}
                required
              />
              <button className="btn-auth" type="submit" disabled={controller.loading}>
                {controller.loading ? 'Ingresando...' : 'Iniciar sesion'}
              </button>
            </form>
          ) : (
            <form onSubmit={controller.onRegister}>
              <div className="field-label">Nombres</div>
              <input className="field" value={controller.nombres} onChange={(e) => controller.setNombres(e.target.value)} required />
              <div className="field-label">Apellidos</div>
              <input className="field" value={controller.apellidos} onChange={(e) => controller.setApellidos(e.target.value)} required />
              <div className="field-label">Cedula</div>
              <input className="field" value={controller.cedula} onChange={(e) => controller.setCedula(e.target.value)} required />
              <div className="field-label">Correo</div>
              <input className="field" type="email" value={controller.correo} onChange={(e) => controller.setCorreo(e.target.value)} required />
              <div className="field-label">Contrasena</div>
              <input className="field" type="password" value={controller.password} onChange={(e) => controller.setPassword(e.target.value)} required />
              <button className="btn-auth" type="submit" disabled={controller.loading}>
                {controller.loading ? 'Creando...' : 'Crear cuenta'}
              </button>
            </form>
          )}

          <div className={`form-msg ${controller.isError ? 'error' : 'success'}`}>{controller.message}</div>
        </div>
      </div>
    </main>
  );
}
