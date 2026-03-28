/**
 * app.js - Lógica del frontend para la demostración de JWT con Javalin 7.
 *
 * Este archivo maneja:
 * 1. Login y obtención del JWT
 * 2. Consulta de estudiantes con el JWT
 * 3. Consulta del endpoint /api-token con header X-Mi-Token
 * 4. Log visual de peticiones HTTP
 */

// Variable global para almacenar el JWT obtenido del login
let jwtToken = null;

// ==================== FUNCIONES PRINCIPALES ====================

/**
 * Realiza el login enviando las credenciales al servidor.
 * Si es exitoso, almacena el JWT para uso posterior.
 */
async function hacerLogin() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const resultadoDiv = document.getElementById('resultado-login');

    if (!username || !password) {
        mostrarResultado(resultadoDiv, 'Por favor complete ambos campos.', false);
        return;
    }

    agregarLog('request', `POST /login (username=${username})`);

    try {
        // Enviar credenciales como form-data (no JSON)
        const formData = new FormData();
        formData.append('username', username);
        formData.append('password', password);

        const response = await fetch('/login', {
            method: 'POST',
            body: formData
        });

        const data = await response.json();

        if (response.ok) {
            jwtToken = data.token;

            // Mostrar el token en el campo de la sección de estudiantes
            document.getElementById('jwt-token').value = jwtToken;
            document.getElementById('btn-estudiantes').disabled = false;

            // Calcular tiempo de expiración
            const expDate = new Date(data.expiresIn);
            const resultado = `Login exitoso!\n\nToken JWT:\n${jwtToken}\n\nExpira: ${expDate.toLocaleString()}`;
            mostrarResultado(resultadoDiv, resultado, true);
            agregarLog('response-ok', `200 OK - Token obtenido (expira: ${expDate.toLocaleTimeString()})`);
        } else {
            mostrarResultado(resultadoDiv, `Error ${response.status}: ${JSON.stringify(data, null, 2)}`, false);
            agregarLog('response-error', `${response.status} - ${data.error || 'Error de autenticación'}`);
        }
    } catch (error) {
        mostrarResultado(resultadoDiv, `Error de conexión: ${error.message}`, false);
        agregarLog('response-error', `Error: ${error.message}`);
    }
}

/**
 * Consulta la lista de estudiantes usando el JWT almacenado.
 * Envía el token en el header Authorization: Bearer <token>.
 */
async function consultarEstudiantes() {
    const resultadoDiv = document.getElementById('resultado-estudiantes');

    if (!jwtToken) {
        mostrarResultado(resultadoDiv, 'Primero debe hacer login para obtener un token JWT.', false);
        return;
    }

    agregarLog('request', `GET /api/estudiante (Authorization: Bearer ${jwtToken.substring(0, 20)}...)`);

    try {
        const response = await fetch('/api/estudiante', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${jwtToken}`
            }
        });

        if (response.ok) {
            const estudiantes = await response.json();

            // Construir tabla HTML con los resultados
            let html = `<p><strong>${estudiantes.length} estudiantes encontrados:</strong></p>`;
            html += '<table class="tabla-estudiantes">';
            html += '<thead><tr><th>#</th><th>Matrícula</th><th>Nombre</th></tr></thead>';
            html += '<tbody>';
            estudiantes.forEach((est, i) => {
                html += `<tr><td>${i + 1}</td><td>${est.id}</td><td>${est.nombre}</td></tr>`;
            });
            html += '</tbody></table>';

            resultadoDiv.innerHTML = html;
            resultadoDiv.className = 'resultado exito';
            resultadoDiv.classList.remove('hidden');

            agregarLog('response-ok', `200 OK - ${estudiantes.length} estudiantes recibidos`);
        } else {
            const data = await response.json();
            mostrarResultado(resultadoDiv, `Error ${response.status}: ${JSON.stringify(data, null, 2)}`, false);
            agregarLog('response-error', `${response.status} - ${data.message || 'Error al consultar'}`);
        }
    } catch (error) {
        mostrarResultado(resultadoDiv, `Error de conexión: ${error.message}`, false);
        agregarLog('response-error', `Error: ${error.message}`);
    }
}

/**
 * Consulta el endpoint /api-token/info usando el header X-Mi-Token.
 * Demuestra un mecanismo de autenticación alternativo al JWT.
 */
async function consultarApiToken() {
    const miToken = document.getElementById('mi-token').value;
    const resultadoDiv = document.getElementById('resultado-api-token');

    if (!miToken) {
        mostrarResultado(resultadoDiv, 'Por favor ingrese un valor para X-Mi-Token.', false);
        return;
    }

    agregarLog('request', `GET /api-token/info (X-Mi-Token: ${miToken})`);

    try {
        const response = await fetch('/api-token/info', {
            method: 'GET',
            headers: {
                'X-Mi-Token': miToken
            }
        });

        const data = await response.json();

        if (response.ok) {
            mostrarResultado(resultadoDiv, JSON.stringify(data, null, 2), true);
            agregarLog('response-ok', `200 OK - Acceso autorizado`);
        } else {
            mostrarResultado(resultadoDiv, `Error ${response.status}: ${JSON.stringify(data, null, 2)}`, false);
            agregarLog('response-error', `${response.status} - ${data.message || 'Token inválido'}`);
        }
    } catch (error) {
        mostrarResultado(resultadoDiv, `Error de conexión: ${error.message}`, false);
        agregarLog('response-error', `Error: ${error.message}`);
    }
}

// ==================== FUNCIONES AUXILIARES ====================

/**
 * Muestra un resultado en el div especificado con formato de éxito o error.
 */
function mostrarResultado(div, texto, esExito) {
    div.textContent = texto;
    div.className = `resultado ${esExito ? 'exito' : 'error'}`;
    div.classList.remove('hidden');
}

/**
 * Agrega una entrada al log visual de peticiones.
 */
function agregarLog(tipo, mensaje) {
    const logsDiv = document.getElementById('logs');
    const timestamp = new Date().toLocaleTimeString();
    const entry = document.createElement('div');
    entry.className = `log-entry ${tipo}`;
    entry.innerHTML = `<span class="timestamp">[${timestamp}]</span> ${mensaje}`;
    logsDiv.insertBefore(entry, logsDiv.firstChild);
}

/**
 * Limpia el log de peticiones.
 */
function limpiarLogs() {
    document.getElementById('logs').innerHTML = '';
}
