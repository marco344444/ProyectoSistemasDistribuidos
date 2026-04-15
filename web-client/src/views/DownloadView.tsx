import { useNavigate } from 'react-router-dom';
import { Sidebar } from '../components/Sidebar';
import { Topbar } from '../components/Topbar';
import { useDownloadController } from '../controllers/useDownloadController';

export function DownloadView() {
  const navigate = useNavigate();
  const controller = useDownloadController();

  return (
    <div className="layout">
      <Sidebar active="download" />
      <div className="content">
        <Topbar
          title={controller.idLote ? `Descargar - ${controller.idLote}` : 'Descargas'}
          subtitle={`${controller.summary.ready} de ${controller.summary.total} imagenes disponibles`}
          actions={
            <>
              <button className="btn-secondary" onClick={() => navigate('/status')}>Volver</button>
              <button
                className="btn-primary"
                onClick={() => {
                  (controller.data?.archivos || [])
                    .filter((file) => file.listo)
                    .forEach((file, index) => {
                      window.setTimeout(() => {
                        controller.downloadFile(file.nombre).catch((error) => {
                          // eslint-disable-next-line no-alert
                          window.alert((error as Error).message);
                        });
                      }, index * 150);
                    });
                }}
                disabled={controller.summary.ready === 0}
              >
                Descargar todo
              </button>
            </>
          }
        />

        <div className="body download-body">
          <div className="panel-left">
            <div className="batch-info">
              <div className="batch-item"><div className="batch-label">LOTE</div><div className="batch-val">{controller.idLote || '-'}</div></div>
              <div className="batch-sep" />
              <div className="batch-item"><div className="batch-label">COMPLETADAS</div><div className="batch-val" style={{ color: 'var(--green)' }}>{controller.summary.ready} / {controller.summary.total}</div></div>
              <div className="batch-sep" />
              <div className="batch-item"><div className="batch-label">TAMANO</div><div className="batch-val" style={{ fontSize: 13, fontWeight: 400, color: 'var(--text2)' }}>{controller.summary.totalKb} KB</div></div>
            </div>

            <div className="section-label">ARCHIVOS RESULTANTES</div>
            <div className="file-list">
              {(controller.data?.archivos || []).map((file) => {
                const ext = file.nombre.split('.').pop()?.toUpperCase() || 'IMG';
                return (
                  <div key={file.nombre} className={`file-row ${file.listo ? 'ready' : 'pending'}`}>
                    <div className={`file-ext ${file.listo ? '' : 'dim'}`}>{ext}</div>
                    <div className="file-info">
                      <div className={`file-name ${file.listo ? '' : 'dim'}`}>{file.nombre}</div>
                      <div className="file-meta">Resultado simulado <span className="file-size">{file.tamKb} KB</span></div>
                    </div>
                    <span className={`chip ${file.listo ? 'chip-green' : 'chip-gray'}`}>{file.listo ? 'Lista' : 'Pendiente'}</span>
                    <button
                      className={`btn-dl ${file.listo ? '' : 'disabled'}`}
                      disabled={!file.listo}
                      onClick={() => {
                        if (!file.listo) {
                          return;
                        }
                        controller.downloadFile(file.nombre).catch((error) => {
                          // eslint-disable-next-line no-alert
                          window.alert((error as Error).message);
                        });
                      }}
                    >
                      {file.listo ? 'Descargar' : 'No disponible'}
                    </button>
                  </div>
                );
              })}
            </div>
            {controller.error ? <div className="form-msg error">{controller.error}</div> : null}
          </div>

          <div className="panel-right">
            <div className="panel-right-title">Resumen</div>
            <div className="stat-cards">
              <div className="stat-card"><span className="stat-label">Archivos listos</span><span className="stat-val green">{controller.summary.ready}</span></div>
              <div className="stat-card"><span className="stat-label">Tamano total</span><span className="stat-val accent">{controller.summary.totalKb} KB</span></div>
              <div className="stat-card"><span className="stat-label">Pendientes</span><span className="stat-val" style={{ color: 'var(--amber)' }}>{controller.summary.pending}</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
