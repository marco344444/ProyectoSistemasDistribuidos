(function () {
  const routeByLabel = [
    { match: ['acceso', 'iniciar sesión', 'login'], target: '01_login.html' },
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
      closeSidebar();
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

  /* ── Mobile sidebar toggle ── */
  const sidebar = document.querySelector('.sidebar');
  const topbar  = document.querySelector('.topbar');

  function closeSidebar() {
    if (sidebar) {
      sidebar.classList.remove('open');
    }
    if (overlay) {
      overlay.classList.remove('visible');
    }
  }

  let overlay = null;

  if (sidebar && topbar) {
    /* Create overlay */
    overlay = document.createElement('div');
    overlay.className = 'sidebar-overlay';
    document.body.appendChild(overlay);

    overlay.addEventListener('click', closeSidebar);

    /* Create hamburger button */
    const hamburger = document.createElement('button');
    hamburger.className = 'hamburger-btn';
    hamburger.setAttribute('aria-label', 'Abrir menú');
    hamburger.innerHTML =
      '<svg viewBox="0 0 18 18" fill="none">' +
      '<path d="M2 4.5h14M2 9h14M2 13.5h14" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>' +
      '</svg>';

    hamburger.addEventListener('click', () => {
      const isOpen = sidebar.classList.toggle('open');
      if (isOpen) {
        overlay.classList.add('visible');
      } else {
        overlay.classList.remove('visible');
      }
    });

    /* Insert hamburger as first child of topbar */
    topbar.insertBefore(hamburger, topbar.firstChild);
  }

  /* Update user pill from localStorage */
  const userName = localStorage.getItem('usuarioSesion');
  const userNameEl = document.querySelector('.user-name');
  const avatarEl   = document.querySelector('.avatar');
  if (userName && userNameEl) {
    userNameEl.textContent = userName;
    if (avatarEl) {
      avatarEl.textContent = userName.slice(0, 2).toUpperCase();
    }
  }
})();
