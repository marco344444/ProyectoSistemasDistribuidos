import { NavLink } from 'react-router-dom';
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
  const usuario = storage.getUser();
  const initials = (usuario || 'IP').slice(0, 2).toUpperCase();

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

      <div className="sidebar-footer">
        <div className="user-pill">
          <div className="avatar">{initials}</div>
          <div>
            <div className="user-name">{usuario || 'Sin sesion'}</div>
            <div className="user-role">Cliente web</div>
          </div>
        </div>
      </div>
    </aside>
  );
}
