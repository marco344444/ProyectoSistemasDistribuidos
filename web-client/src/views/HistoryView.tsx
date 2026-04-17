import { useNavigate } from 'react-router-dom';
import { Sidebar } from '../components/Sidebar';
import { Topbar } from '../components/Topbar';
import { useHistoryController } from '../controllers/useHistoryController';
import { storage } from '../services/storage';

function formatDate(value: string): string {
  const numeric = Number(value);
  const date = Number.isNaN(numeric) ? new Date(value) : new Date(numeric);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  const fecha = date.toLocaleDateString('es-CO');
  const hora = date.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  return `${fecha} ${hora}`;
}

export function HistoryView() {
  const navigate = useNavigate();
  const controller = useHistoryController();

  return (
    <div className="layout">
      <Sidebar active="history" />
      <div className="content">
        <Topbar
          title="Historial de solicitudes"
          subtitle={
            controller.replica?.consistente
              ? `Replica: OK (${controller.replica.totalReplicaciones} syncs)`
              : 'Replica: verificando...'
          }
          actions={<button className="btn-primary" onClick={() => navigate('/upload')}>+ Nueva solicitud</button>}
        />

        <div className="body history-body">
          <div className="stats-row">
            <div className="stat"><div className="stat-label">TOTAL LOTES</div><div className="stat-val">{controller.historial?.totalLotes ?? 0}</div></div>
            <div className="stat"><div className="stat-label">COMPLETADOS</div><div className="stat-val green">{controller.historial?.completados ?? 0}</div></div>
            <div className="stat"><div className="stat-label">EN PROGRESO</div><div className="stat-val amber">{controller.historial?.enProgreso ?? 0}</div></div>
            <div className="stat"><div className="stat-label">IMGS PROCESADAS</div><div className="stat-val accent">{controller.historial?.totalImagenes ?? 0}</div></div>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>LOTE</th>
                  <th>FECHA</th>
                  <th>IMAGENES</th>
                  <th>ESTADO</th>
                  <th>DURACION</th>
                  <th>NODOS</th>
                  <th>ACCIONES</th>
                </tr>
              </thead>
              <tbody>
                {(controller.historial?.lotes || []).map((item) => (
                  <tr key={item.idLote}>
                    <td className="lote-id">{item.idLote}</td>
                    <td className="mono">{formatDate(item.fecha)}</td>
                    <td>{item.imagenes} imgs</td>
                    <td><span className={`chip ${item.estado === 'COMPLETADO' ? 'chip-green' : 'chip-amber'}`}>{item.estado}</span></td>
                    <td className="muted">{item.duracionMs} ms</td>
                    <td className="muted">{item.nodos || '-'}</td>
                    <td>
                      <button
                        className="btn-sm"
                        onClick={() => {
                          storage.setBatchId(item.idLote);
                          navigate('/download');
                        }}
                      >
                        Ver detalle
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {controller.error ? <div className="form-msg error">{controller.error}</div> : null}
        </div>
      </div>
    </div>
  );
}
