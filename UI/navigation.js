(function () {
  function getSessionData() {
    return {
      nombreSesion: (localStorage.getItem('nombreSesion') || '').trim(),
      usuarioSesion: (localStorage.getItem('usuarioSesion') || '').trim()
    };
  }

  function clearSessionData() {
    localStorage.removeItem('tokenSesion');
    localStorage.removeItem('usuarioSesion');
    localStorage.removeItem('nombreSesion');
    localStorage.removeItem('idLoteActual');
    window.dispatchEvent(new CustomEvent('session-changed'));
  }

  const routeByLabel = [
    { match: ['acceso'], target: '01_login.html' },
    { match: ['nueva solicitud'], target: '02_upload.html' },
    { match: ['estado'], target: '03_status.html' },
    { match: ['descargas'], target: '04_download.html' },
    { match: ['historial'], target: '05_history.html' }
  ];

  function detectTarget(text) {
    const normalized = (text || '').toLowerCase().trim();
    for (const route of routeByLabel) {
      for (const key of route.match) {
        if (normalized.includes(key)) {
          return route.target;
        }
      }
    }
    return null;
  }

  document.querySelectorAll('.nav-item').forEach((item) => {
    const target = detectTarget(item.textContent);
    if (!target) {
      return;
    }
    item.style.cursor = 'pointer';
    item.addEventListener('click', () => {
      window.location.href = target;
    });
  });

  const backBtn = document.querySelector('.back-btn');
  if (backBtn) {
    backBtn.style.cursor = 'pointer';
    backBtn.addEventListener('click', () => {
      if (window.history.length > 1) {
        window.history.back();
        return;
      }
      window.location.href = '02_upload.html';
    });
  }

  const newBtn = document.querySelector('.btn-new');
  if (newBtn) {
    newBtn.addEventListener('click', () => {
      window.location.href = '02_upload.html';
    });
  }

  const cancelBtn = document.querySelector('.btn-secondary');
  if (cancelBtn && cancelBtn.textContent.toLowerCase().includes('cancelar')) {
    cancelBtn.addEventListener('click', () => {
      window.location.href = '05_history.html';
    });
  }

  const userNameEl = document.querySelector('.user-name');
  const userRoleEl = document.querySelector('.user-role');
  const avatarEl = document.querySelector('.avatar');
  const userPill = document.querySelector('.user-pill');
  const sidebarFooter = document.querySelector('.sidebar-footer');

  function closeUserMenu(menu) {
    if (menu) {
      menu.classList.remove('open');
    }
  }

  if (userNameEl && userRoleEl && avatarEl && userPill && sidebarFooter) {
    let userMenu = sidebarFooter.querySelector('.user-menu');
    if (!userMenu) {
      userMenu = document.createElement('div');
      userMenu.className = 'user-menu';
      userMenu.innerHTML =
        '<div class="user-menu-label">Sesion iniciada</div>' +
        '<button class="user-menu-btn logout" type="button">Cerrar sesion</button>';
      sidebarFooter.appendChild(userMenu);

      userMenu.querySelector('.logout').addEventListener('click', () => {
        clearSessionData();
        closeUserMenu(userMenu);
        window.location.href = '01_login.html';
      });

      document.addEventListener('click', (event) => {
        if (!sidebarFooter.contains(event.target)) {
          closeUserMenu(userMenu);
        }
      });

      userPill.addEventListener('click', (event) => {
        const session = getSessionData();
        if (!session.nombreSesion && !session.usuarioSesion) {
          return;
        }
        event.stopPropagation();
        userMenu.classList.toggle('open');
      });
    }

    function renderSidebarSession() {
      const session = getSessionData();
      const nombreSesion = session.nombreSesion;
      const usuarioSesion = session.usuarioSesion;

      if (nombreSesion || usuarioSesion) {
        const nombreMostrado = nombreSesion || usuarioSesion;
        const partes = nombreMostrado.split(/\s+/).filter(Boolean);
        const iniciales = partes.length >= 2
          ? (partes[0][0] + partes[1][0]).toUpperCase()
          : nombreMostrado.slice(0, 2).toUpperCase();

        userNameEl.textContent = nombreMostrado;
        userRoleEl.textContent = usuarioSesion || 'Sesion activa';
        avatarEl.textContent = iniciales || 'IP';
        userPill.classList.add('logged-in');
      } else {
        userNameEl.textContent = 'Sin sesión';
        userRoleEl.textContent = 'Inicia para continuar';
        avatarEl.textContent = 'IP';
        userPill.classList.remove('logged-in');
        closeUserMenu(userMenu);
      }
    }

    renderSidebarSession();
    window.addEventListener('session-changed', renderSidebarSession);
  }
})();
