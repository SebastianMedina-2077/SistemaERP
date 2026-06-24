/**
 * Sistema de alertas Mamma Tomato
 * Basado en diseno Uiverse.io by akshat-patel28
 * Tipos: success | error | info | warning
 * Se expone en window.MammaTomatoAlert para que lo consuman scripts clasicos y modulos.
 */

window.MammaTomatoAlert = (() => {
    const DURACION_DEFAULT = 4000;
    const MAX_ALERTAS = 4;

    let contenedor = null;

    function inicializar() {
        if (document.getElementById('mt-alert-container')) {
            contenedor = document.getElementById('mt-alert-container');
            return;
        }
        contenedor = document.createElement('div');
        contenedor.id = 'mt-alert-container';
        contenedor.setAttribute('aria-live', 'polite');
        contenedor.setAttribute('aria-atomic', 'false');
        document.body.appendChild(contenedor);
    }

    const CONFIG = {
        success: {
            waveColor: '#04e4003a',
            iconBg:    '#04e40048',
            iconColor: '#269b24',
            textColor: '#269b24',
            iconPath:  'M256 48a208 208 0 1 1 0 416 208 208 0 1 1 0-416zm0 464A256 256 0 1 0 256 0a256 256 0 1 0 0 512zM369 209c9.4-9.4 9.4-24.6 0-33.9s-24.6-9.4-33.9 0l-111 111-47-47c-9.4-9.4-24.6-9.4-33.9 0s-9.4 24.6 0 33.9l64 64c9.4 9.4 24.6 9.4 33.9 0L369 209z',
            viewBox:   '0 0 512 512'
        },
        error: {
            waveColor: '#fc0c0c3a',
            iconBg:    '#fc0c0c48',
            iconColor: '#d10d0d',
            textColor: '#d10d0d',
            iconPath:  'M256 48a208 208 0 1 1 0 416 208 208 0 1 1 0-416zm0 464A256 256 0 1 0 256 0a256 256 0 1 0 0 512zM175 175c-9.4 9.4-9.4 24.6 0 33.9l47 47-47 47c-9.4 9.4-9.4 24.6 0 33.9s24.6 9.4 33.9 0l47-47 47 47c9.4 9.4 24.6 9.4 33.9 0s9.4-24.6 0-33.9l-47-47 47-47c9.4-9.4 9.4-24.6 0-33.9s-24.6-9.4-33.9 0l-47 47-47-47c-9.4-9.4-24.6-9.4-33.9 0z',
            viewBox:   '0 0 512 512'
        },
        info: {
            waveColor: '#4777ff3a',
            iconBg:    '#4777ff48',
            iconColor: '#124fff',
            textColor: '#124fff',
            iconPath:  'M13 7.5a1 1 0 1 1-2 0 1 1 0 0 1 2 0Zm-3 3.75a.75.75 0 0 1 .75-.75h1.5a.75.75 0 0 1 .75.75v4.25h.75a.75.75 0 0 1 0 1.5h-3a.75.75 0 0 1 0-1.5h.75V12h-.75a.75.75 0 0 1-.75-.75ZM12 1c6.075 0 11 4.925 11 11s-4.925 11-11 11S1 18.075 1 12 5.925 1 12 1ZM2.5 12a9.5 9.5 0 0 0 9.5 9.5 9.5 9.5 0 0 0 9.5-9.5A9.5 9.5 0 0 0 12 2.5 9.5 9.5 0 0 0 2.5 12Z',
            viewBox:   '0 0 24 24'
        },
        warning: {
            waveColor: '#ffa30d3a',
            iconBg:    '#ffa30d48',
            iconColor: '#db970e',
            textColor: '#db970e',
            iconPath:  'M236.8,188.09,149.35,36.22h0a24.76,24.76,0,0,0-42.7,0L19.2,188.09a23.51,23.51,0,0,0,0,23.72A24.35,24.35,0,0,0,40.55,224h174.9a24.35,24.35,0,0,0,21.33-12.19A23.51,23.51,0,0,0,236.8,188.09ZM222.93,203.8a8.5,8.5,0,0,1-7.48,4.2H40.55a8.5,8.5,0,0,1-7.48-4.2,7.59,7.59,0,0,1,0-7.72L120.52,44.21a8.75,8.75,0,0,1,15,0l87.45,151.87A7.59,7.59,0,0,1,222.93,203.8ZM120,144V104a8,8,0,0,1,16,0v40a8,8,0,0,1-16,0Zm20,36a12,12,0,1,1-12-12A12,12,0,0,1,140,180Z',
            viewBox:   '0 0 256 256'
        }
    };

    const WAVE_PATH = 'M0,256L11.4,240C22.9,224,46,192,69,192C91.4,192,114,224,137,234.7C160,245,183,235,206,213.3C228.6,192,251,160,274,149.3C297.1,139,320,149,343,181.3C365.7,213,389,267,411,282.7C434.3,299,457,277,480,250.7C502.9,224,526,192,549,181.3C571.4,171,594,181,617,208C640,235,663,277,686,256C708.6,235,731,149,754,122.7C777.1,96,800,128,823,165.3C845.7,203,869,245,891,224C914.3,203,937,117,960,112C982.9,107,1006,181,1029,197.3C1051.4,213,1074,171,1097,144C1120,117,1143,107,1166,133.3C1188.6,160,1211,224,1234,218.7C1257.1,213,1280,139,1303,133.3C1325.7,128,1349,192,1371,192C1394.3,192,1417,128,1429,96L1440,64L1440,320L1428.6,320C1417.1,320,1394,320,1371,320C1348.6,320,1326,320,1303,320C1280,320,1257,320,1234,320C1211.4,320,1189,320,1166,320C1142.9,320,1120,320,1097,320C1074.3,320,1051,320,1029,320C1005.7,320,983,320,960,320C937.1,320,914,320,891,320C868.6,320,846,320,823,320C800,320,777,320,754,320C731.4,320,709,320,686,320C662.9,320,640,320,617,320C594.3,320,571,320,549,320C525.7,320,503,320,480,320C457.1,320,434,320,411,320C388.6,320,366,320,343,320C320,320,297,320,274,320C251.4,320,229,320,206,320C182.9,320,160,320,137,320C114.3,320,91,320,69,320C45.7,320,23,320,11,320L0,320Z';

    const CROSS_PATH = 'M11.7816 4.03157C12.0062 3.80702 12.0062 3.44295 11.7816 3.2184C11.5571 2.99385 11.193 2.99385 10.9685 3.2184L7.50005 6.68682L4.03164 3.2184C3.80708 2.99385 3.44301 2.99385 3.21846 3.2184C2.99391 3.44295 2.99391 3.80702 3.21846 4.03157L6.68688 7.49999L3.21846 10.9684C2.99391 11.193 2.99391 11.557 3.21846 11.7816C3.44301 12.0061 3.80708 12.0061 4.03164 11.7816L7.50005 8.31316L10.9685 11.7816C11.193 12.0061 11.5571 12.0061 11.7816 11.7816C12.0062 11.557 12.0062 11.193 11.7816 10.9684L8.31322 7.49999L11.7816 4.03157Z';

    function escapar(texto) {
        const div = document.createElement('div');
        div.textContent = texto == null ? '' : String(texto);
        return div.innerHTML;
    }

    function mostrar(tipo, titulo, subtitulo = '', duracion = DURACION_DEFAULT) {
        inicializar();

        const alertasActuales = contenedor.querySelectorAll('.mt-alert-card');
        if (alertasActuales.length >= MAX_ALERTAS) {
            alertasActuales[0].remove();
        }

        const cfg = CONFIG[tipo] || CONFIG.info;
        const card = document.createElement('div');
        card.className = 'mt-alert-card mt-alert-entrando';
        card.setAttribute('role', 'alert');

        card.innerHTML = `
            <svg class="mt-alert-wave" viewBox="0 0 1440 320" xmlns="http://www.w3.org/2000/svg">
                <path d="${WAVE_PATH}" fill="${cfg.waveColor}" fill-opacity="1"></path>
            </svg>
            <div class="mt-alert-icon-wrap" style="background:${cfg.iconBg};">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="${cfg.viewBox}"
                     fill="currentColor" stroke="currentColor" stroke-width="0"
                     class="mt-alert-icon" style="color:${cfg.iconColor};">
                    <path d="${cfg.iconPath}"></path>
                </svg>
            </div>
            <div class="mt-alert-texto">
                <p class="mt-alert-titulo" style="color:${cfg.textColor};">${escapar(titulo)}</p>
                ${subtitulo ? `<p class="mt-alert-subtitulo">${escapar(subtitulo)}</p>` : ''}
            </div>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 15 15"
                 fill="none" stroke="currentColor" stroke-width="0"
                 class="mt-alert-cerrar" aria-label="Cerrar">
                <path fill="currentColor" clip-rule="evenodd" fill-rule="evenodd"
                      d="${CROSS_PATH}"></path>
            </svg>
        `;

        card.querySelector('.mt-alert-cerrar').addEventListener('click', () => cerrar(card));
        contenedor.appendChild(card);

        requestAnimationFrame(() => {
            card.classList.remove('mt-alert-entrando');
            card.classList.add('mt-alert-visible');
        });

        if (duracion > 0) {
            setTimeout(() => cerrar(card), duracion);
        }

        return card;
    }

    function cerrar(card) {
        if (!card || !card.parentNode) return;
        card.classList.remove('mt-alert-visible');
        card.classList.add('mt-alert-saliendo');
        card.addEventListener('transitionend', () => card.remove(), { once: true });
    }

    return {
        success: (titulo, subtitulo, duracion) => mostrar('success', titulo, subtitulo, duracion),
        error:   (titulo, subtitulo, duracion) => mostrar('error',   titulo, subtitulo, duracion),
        info:    (titulo, subtitulo, duracion) => mostrar('info',    titulo, subtitulo, duracion),
        warning: (titulo, subtitulo, duracion) => mostrar('warning', titulo, subtitulo, duracion)
    };
})();

/**
 * Modal de confirmacion liviano y reutilizable (sin librerias externas).
 * mostrarModalConfirmacion({ titulo, mensaje, labelConfirmar, labelCancelar, peligro, onConfirmar })
 */
window.mostrarModalConfirmacion = (opciones = {}) => {
    const {
        titulo = 'Confirmar',
        mensaje = '',
        labelConfirmar = 'Confirmar',
        labelCancelar = 'Cancelar',
        peligro = false,
        onConfirmar
    } = opciones;

    const overlay = document.createElement('div');
    overlay.className = 'mt-confirm-overlay';

    const card = document.createElement('div');
    card.className = 'mt-confirm-card';
    card.setAttribute('role', 'dialog');
    card.setAttribute('aria-modal', 'true');

    const h = document.createElement('h3');
    h.className = 'mt-confirm-titulo';
    h.textContent = titulo;

    const p = document.createElement('p');
    p.className = 'mt-confirm-mensaje';
    p.textContent = mensaje;

    const acciones = document.createElement('div');
    acciones.className = 'mt-confirm-acciones';

    const btnCancelar = document.createElement('button');
    btnCancelar.type = 'button';
    btnCancelar.className = 'mt-confirm-btn mt-confirm-cancelar';
    btnCancelar.textContent = labelCancelar;

    const btnConfirmar = document.createElement('button');
    btnConfirmar.type = 'button';
    btnConfirmar.className = 'mt-confirm-btn ' + (peligro ? 'mt-confirm-peligro' : 'mt-confirm-confirmar');
    btnConfirmar.textContent = labelConfirmar;

    acciones.append(btnCancelar, btnConfirmar);
    card.append(h, p, acciones);
    overlay.appendChild(card);
    document.body.appendChild(overlay);

    requestAnimationFrame(() => overlay.classList.add('mt-confirm-visible'));

    function onKey(e) {
        if (e.key === 'Escape') cerrar();
    }

    function cerrar() {
        overlay.classList.remove('mt-confirm-visible');
        overlay.addEventListener('transitionend', () => overlay.remove(), { once: true });
        document.removeEventListener('keydown', onKey);
    }

    btnCancelar.addEventListener('click', cerrar);
    overlay.addEventListener('click', (e) => { if (e.target === overlay) cerrar(); });
    document.addEventListener('keydown', onKey);
    btnConfirmar.addEventListener('click', () => {
        cerrar();
        if (typeof onConfirmar === 'function') onConfirmar();
    });

    requestAnimationFrame(() => btnConfirmar.focus());
};
