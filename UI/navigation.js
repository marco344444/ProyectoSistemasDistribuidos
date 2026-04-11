(function () {
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
})();
