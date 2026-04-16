import { useEffect, useRef, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { storage } from '../services/storage';

type Props = {
  active: 'login' | 'upload' | 'status' | 'download' | 'history';
};

const NavItem = ({ to, label, active }: { to: string; label: string; active: boolean }) => (
  <NavLink to={to} className={`nav-item ${active ? 'active' : ''}`}>
    <span>{label}</span>
  </NavLink>
);

export function Sidebar({ active }: Props) {
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const [confirmLogout, setConfirmLogout] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

  const usuario = storage.getUser();
  const initials = (usuario || 'IP').slice(0, 2).toUpperCase();

  useEffect(() => {
    if (!menuOpen) {
      return;
    }

    const onDocumentClick = (event: MouseEvent) => {
      const target = event.target as Node;
      if (!menuRef.current || menuRef.current.contains(target)) {
        return;
      }
      setMenuOpen(false);
      setConfirmLogout(false);
    };

    const onEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setMenuOpen(false);
        setConfirmLogout(false);
      }
    };

    document.addEventListener('mousedown', onDocumentClick);
    document.addEventListener('keydown', onEscape);
    return () => {
      document.removeEventListener('mousedown', onDocumentClick);
      document.removeEventListener('keydown', onEscape);
    };
  }, [menuOpen]);

  const onLogout = () => {
    storage.clearToken();
    storage.clearUser();
    storage.clearBatchId();
    setMenuOpen(false);
    setConfirmLogout(false);
    navigate('/');
  };

  return (
    <aside className="sidebar">
      <div className="logo">
        <div className="logo-icon" />
        <div>
          <div className="logo-text">ImageProc</div>
          <div className="logo-sub">Sistema Distribuido</div>
        </div>
      </div>

      <div className="nav-section-label">NAVEGACION</div>
      <NavItem to="/upload" label="Nueva solicitud" active={active === 'upload'} />
      <NavItem to="/status" label="Estado" active={active === 'status'} />
      <NavItem to="/download" label="Descargas" active={active === 'download'} />
      <NavItem to="/history" label="Historial" active={active === 'history'} />

      <div className="sidebar-footer" ref={menuRef}>
        <button
          className="user-pill user-pill-button"
          type="button"
          onClick={() => setMenuOpen((prev) => !prev)}
          aria-expanded={menuOpen}
          aria-haspopup="menu"
        >
          <div className="avatar">{initials}</div>
          <div>
            <div className="user-name">{usuario || 'Sin sesion'}</div>
            <div className="user-role">Cliente web</div>
          </div>
        </button>

        {menuOpen ? (
          <div className="user-menu" role="menu">
            {!confirmLogout ? (
              <button
                className="user-menu-item danger"
                type="button"
                onClick={() => setConfirmLogout(true)}
                role="menuitem"
              >
                Cerrar sesion
              </button>
            ) : (
              <div className="logout-confirm">
                <div className="logout-confirm-text">Confirmar cierre de sesion?</div>
                <div className="logout-confirm-actions">
                  <button className="user-menu-item danger" type="button" onClick={onLogout} role="menuitem">
                    Confirmar
                  </button>
                  <button className="user-menu-item" type="button" onClick={() => setConfirmLogout(false)} role="menuitem">
                    Cancelar
                  </button>
                </div>
              </div>
            )}
          </div>
        ) : null}
      </div>
    </aside>
  );
}
