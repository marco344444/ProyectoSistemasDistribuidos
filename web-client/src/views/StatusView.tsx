import { useNavigate } from 'react-router-dom';
import { Sidebar } from '../components/Sidebar';
import { Topbar } from '../components/Topbar';
import { useStatusController } from '../controllers/useStatusController';

export function StatusView() {
  const navigate = useNavigate();
  const controller = useStatusController();

  return (
    <div className="layout">
      <Sidebar active="status" />
      <div className="content">
        <Topbar
          title={controller.idLote ? `Lote ${controller.idLote}` : 'Sin lote activo'}
          subtitle="Estado consultado en tiempo real"
          actions={
            <>
              <button className="btn-secondary" onClick={() => navigate('/upload')}>Volver</button>
              <span className={controller.uiEstado.className}>{controller.uiEstado.label}</span>
            </>
          }
        />

        <div className="body status-body">
          <div className="panel-left">
            <div className="metrics">
              <div className="metric"><div className="metric-label">TOTAL</div><div className="metric-val">{controller.estado?.total ?? '-'}</div></div>
              <div className="metric"><div className="metric-label">COMPLETADAS</div><div className="metric-val green">{controller.estado?.procesadas ?? '-'}</div></div>
              <div className="metric"><div className="metric-label">PROGRESO</div><div className="metric-val accent">{controller.progreso}%</div></div>
              <div className="metric"><div className="metric-label">ESTADO</div><div className="metric-val amber">{controller.estado?.estado ?? 'N/A'}</div></div>
            </div>

            <div className="progress-section">
              <div className="progress-header">
                <span className="progress-label">PROGRESO GENERAL</span>
                <span className="progress-pct">{controller.progreso}%</span>
              </div>
              <div className="progress-track"><div className="progress-bar" style={{ width: `${controller.progreso}%` }} /></div>
            </div>

            {controller.error ? <div className="form-msg error">{controller.error}</div> : null}
          </div>

          <div className="panel-right">
            <div className="consumo-card">
              <div className="consumo-title">Metricas de consumo</div>
              <div className="consumo-grid">
                <div className="consumo-item"><div className="consumo-label">CPU NUCLEOS</div><div className="consumo-val">{controller.metricas?.cpuNuclei ?? '-'}</div></div>
                <div className="consumo-item"><div className="consumo-label">MEMORIA MB</div><div className="consumo-val">{controller.metricas?.memoriaUsadaMb ?? '-'}</div></div>
                <div className="consumo-item"><div className="consumo-label">CARGA NODO</div><div className="consumo-val">{controller.metricas?.nodo.cargaActual ?? '-'}%</div></div>
                <div className="consumo-item"><div className="consumo-label">HILOS MAX</div><div className="consumo-val">{controller.metricas?.nodo.maxHilosParalelos ?? '-'}</div></div>
                <div className="consumo-item"><div className="consumo-label">TRABAJOS</div><div className="consumo-val">{controller.metricas?.nodo.trabajosProcesados ?? '-'}</div></div>
                <div className="consumo-item"><div className="consumo-label">REPLICA BD</div><div className="consumo-val">{controller.metricas?.replica.consistente ? 'OK' : 'N/A'}</div></div>
              </div>
              <div className="consumo-live">Actualizacion automatica cada 5s</div>
            </div>

            <div className="consumo-card">
              <div className="consumo-title">Ejecucion del lote</div>
              <div className="consumo-grid">
                <div className="consumo-item"><div className="consumo-label">DURACION</div><div className="consumo-val">{controller.estado?.duracionMs ?? 0} ms</div></div>
                <div className="consumo-item"><div className="consumo-label">TRANSFORMACIONES</div><div className="consumo-val">{controller.estado?.transformaciones?.join(', ') || 'N/A'}</div></div>
              </div>
              <div className="log-list">
                {(controller.estado?.logs || []).length > 0 ? (
                  controller.estado?.logs.map((linea, index) => (
                    <div key={`${linea}-${index}`} className="log-line">{linea}</div>
                  ))
                ) : (
                  <div className="muted">Aun no hay logs para este lote.</div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
