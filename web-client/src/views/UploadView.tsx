import { Sidebar } from '../components/Sidebar';
import { Topbar } from '../components/Topbar';
import { useUploadController } from '../controllers/useUploadController';

export function UploadView() {
  const controller = useUploadController();

  return (
    <div className="layout">
      <Sidebar active="upload" />
      <div className="content">
        <Topbar
          title="Nueva solicitud"
          subtitle="Carga imagenes y define las transformaciones"
          actions={
            <>
              <input
                className="search"
                type="number"
                min={1}
                max={controller.maxCantidad}
                value={controller.cantidad}
                onChange={(e) => controller.setCantidad(Number(e.target.value || 1))}
                title="Cantidad de imagenes"
              />
              <button className="btn-primary" onClick={controller.onSendBatch} disabled={controller.loading}>
                {controller.loading ? 'Enviando...' : 'Enviar lote'}
              </button>
            </>
          }
        />

        <div className="body upload-body">
          <div className="panel-left">
            <label className="dropzone" htmlFor="fileInput">
              <div className="dz-icon">+</div>
              <div className="dz-title">Arrastra tus imagenes aqui</div>
              <div className="dz-sub">JPG, PNG, TIF - Maximo 2000 por lote</div>
              <div className="btn-upload">Seleccionar archivos</div>
            </label>
            <input id="fileInput" type="file" accept=".jpg,.jpeg,.png,.tif,.tiff" multiple style={{ display: 'none' }} onChange={controller.onFileChange} />

            <div className="section-label">IMAGENES CARGADAS ({controller.files.length})</div>
            <div className="image-list">
              {controller.files.slice(0, 8).map((file, index) => (
                <div key={`${file.name}-${index}`} className="image-card selected">
                  <div className="img-header">
                    <div className="img-name">
                      <span className="ext-badge">{file.name.split('.').pop()?.toUpperCase() || 'IMG'}</span>
                      {file.name}
                    </div>
                    <button
                      type="button"
                      className="btn-remove-file"
                      onClick={() => controller.onRemoveFile(index)}
                      title="Eliminar imagen"
                    >
                      Eliminar
                    </button>
                  </div>
                </div>
              ))}
              {controller.files.length === 0 ? <div className="muted">No hay archivos seleccionados.</div> : null}
            </div>

            <div className="section-label upload-mt">TRANSFORMACIONES DEL BLOQUE</div>
            <div className="transform-grid">
              {controller.availableTransformations.map((item) => {
                const selected = controller.transformacionesSeleccionadas.includes(item.key);
                return (
                  <button
                    key={item.key}
                    type="button"
                    className={`transform-chip ${selected ? 'selected' : ''}`}
                    onClick={() => controller.onToggleTransformacion(item.key)}
                  >
                    {item.label}
                  </button>
                );
              })}
            </div>
            {controller.message ? <div className="form-msg error">{controller.message}</div> : null}
          </div>

          <div className="panel-right">
            <div className="panel-right-title">Resumen del lote</div>
            <div className="summary-card">
              <div className="summary-row"><span>Imagenes</span><span className="summary-val">{controller.files.length || controller.cantidad} archivos</span></div>
              <div className="summary-row"><span>Cantidad a enviar</span><span className="summary-val accent">{controller.cantidad}</span></div>
              <div className="summary-row"><span>Tamano total</span><span className="summary-val">{controller.totalSizeMb} MB</span></div>
              <div className="summary-row"><span>Transformaciones</span><span className="summary-val">{controller.transformacionesSeleccionadas.length}</span></div>
              <div className="summary-row"><span>Tiempo estimado</span><span className="summary-val">{controller.estimatedTime}</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
