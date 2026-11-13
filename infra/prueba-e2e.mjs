// Prueba de extremo a extremo contra el gateway de docker-compose.full.yml
import crypto from 'node:crypto';
const BASE = process.env.BASE || 'http://localhost';
const SECRETO_WEBHOOK = process.env.PAGOS_WEBHOOK_SECRET;
const sufijo = Date.now();
const dormir = (ms) => new Promise((r) => setTimeout(r, ms));
let fallos = 0;

async function api(metodo, ruta, { token, body, headers = {} } = {}) {
  const h = { ...headers };
  if (body !== undefined) h['Content-Type'] = 'application/json';
  if (token) h.Authorization = `Bearer ${token}`;
  const res = await fetch(BASE + ruta, { method: metodo, headers: h, body: body === undefined ? undefined : JSON.stringify(body) });
  const texto = await res.text();
  let json; try { json = texto ? JSON.parse(texto) : null; } catch { json = texto; }
  return { status: res.status, json };
}
function paso(nombre, ok, detalle) {
  if (!ok) fallos++;
  console.log(`${ok ? 'OK  ' : 'FALLO'} ${nombre}${detalle ? ' -> ' + (typeof detalle === 'string' ? detalle : JSON.stringify(detalle)).slice(0, 300) : ''}`);
}
const tokenDe = (j) => j?.accessToken ?? j?.token?.accessToken ?? j?.token ?? j?.access_token;
async function hasta(fn, ms = 60000) {
  const fin = Date.now() + ms; let ultimo;
  while (Date.now() < fin) { ultimo = await fn(); if (ultimo.ok) return ultimo; await dormir(2000); }
  return ultimo;
}

// 1. Operador
const opEmail = `op${sufijo}@tamaka.test`, turEmail = `tur${sufijo}@tamaka.test`, clave = 'ClaveSegura123';
let r = await api('POST', '/api/cuentas', { body: { email: opEmail, password: clave, nombre: 'Operador Prueba' } });
paso('registro operador', r.status === 201, r.status);
r = await api('POST', '/api/auth/login', { body: { email: opEmail, password: clave } });
paso('login operador', r.status === 200 && tokenDe(r.json), r.status);
const refreshOp = r.json?.refreshToken;
r = await api('POST', '/api/cuentas/me/modo-operador', { token: tokenDe(r.json) });
const tokOp = tokenDe(r.json);
paso('activar modo operador (token nuevo)', r.status === 200 && tokOp, r.status);

// refresh token rotado tras cambio de modo: el anterior debe quedar revocado
r = await api('POST', '/api/auth/refresh', { body: { refreshToken: refreshOp } });
paso('refresh anterior revocado tras cambio de modo', r.status === 401, r.status);

// 2. Comercio y servicio
r = await api('POST', '/api/comercios', { token: tokOp, body: { nombre: 'Tours Prueba', rnt: `RNT${sufijo}`, categoria: 'NATURALEZA', descripcion: 'e2e' } });
paso('crear comercio', r.status === 201, r.status === 201 ? '' : r.json);
const comercioId = r.json?.id;
r = await api('POST', `/api/comercios/${comercioId}/servicios`, { token: tokOp, body: { nombre: 'Tour Tayrona', precioBase: 100.00, minPersonas: 1, maxPersonas: 6, atributos: { duracion: '4h' } } });
paso('crear servicio', r.status === 201, r.status === 201 ? '' : r.json);
const servicioId = r.json?.id;
r = await api('GET', `/api/comercios/${comercioId}`);
paso('detalle comercio público', r.status === 200, r.status);

// 3. Turista
r = await api('POST', '/api/cuentas', { body: { email: turEmail, password: clave, nombre: 'Turista Prueba' } });
paso('registro turista', r.status === 201, r.status);
r = await api('POST', '/api/auth/login', { body: { email: turEmail, password: clave } });
const tokTur = tokenDe(r.json); const refreshTur = r.json?.refreshToken;
paso('login turista', r.status === 200 && tokTur, r.status);
r = await api('POST', '/api/auth/refresh', { body: { refreshToken: refreshTur } });
paso('refresh token turista', r.status === 200 && tokenDe(r.json), r.status);

// 4. Reserva
const manana = new Date(Date.now() + 86400000).toISOString().slice(0, 10);
r = await api('POST', '/api/reservas', { token: tokTur, body: { servicioId, fechaReserva: manana, numeroPersonas: 2 } });
paso('crear reserva (consulta HTTP a comercios)', r.status === 201, r.status === 201 ? '' : r.json);
const reservaId = r.json?.id;
r = await api('POST', '/api/reservas', { token: tokTur, body: { servicioId, fechaReserva: manana, numeroPersonas: 9 } });
paso('rechaza personas fuera de rango', r.status === 409 || r.status === 400, r.status);

// 5. Operador acepta
r = await api('PATCH', `/api/operador/reservas/${reservaId}/respuesta`, { token: tokOp, body: { aceptada: true, valorAPagar: 200.00, observacion: 'ok' } });
paso('operador acepta', r.status === 200, r.status === 200 ? r.json?.estado : r.json);

// 6. Pagos debe recibir ReservaAceptada por el bus (outbox)
const ini = await hasta(async () => { const x = await api('POST', '/api/pagos', { token: tokTur, body: { reservaId } }); return { ok: x.status === 201 || x.status === 200, x }; });
paso('iniciar pago (evento ReservaAceptada llegó a pagos)', ini.ok, ini.ok ? '' : ini.x);
const pago = ini.x?.json;
const referencia = pago?.referenciaPasarela;
const monto = pago?.monto ?? pago?.valor ?? 200.00;

// 7. Webhook firmado
const montoTxt = Number(monto).toFixed(2);
const firma = crypto.createHmac('sha256', SECRETO_WEBHOOK).update(`${referencia}|APROBADO|${montoTxt}`).digest('hex');
r = await api('POST', '/api/pagos/webhook', { body: { referencia, estado: 'APROBADO', monto: montoTxt }, headers: { 'X-Firma-Pasarela': firma } });
paso('webhook aprobado', r.status >= 200 && r.status < 300, r.status === 200 || r.status === 204 ? r.status : r.json);
r = await api('POST', '/api/pagos/webhook', { body: { referencia, estado: 'APROBADO', monto: montoTxt }, headers: { 'X-Firma-Pasarela': 'mala' } });
paso('webhook con firma inválida rechazado', r.status === 401 || r.status === 403 || r.status === 400, r.status);

// 8. Reserva confirmada por el evento PagoAprobado
const conf = await hasta(async () => { const x = await api('GET', `/api/reservas/${reservaId}`, { token: tokTur }); return { ok: x.json?.estado === 'CONFIRMADA', x }; });
paso('reserva CONFIRMADA por PagoAprobado', conf.ok, conf.x?.json?.estado ?? conf.x);

// 9. Consumidores secundarios
const noti = await hasta(async () => { const x = await api('GET', '/api/notificaciones', { token: tokTur }); return { ok: x.status === 200 && Array.isArray(x.json) && x.json.length > 0, x }; });
paso('notificaciones del turista', noti.ok, noti.ok ? `${noti.x.json.length} avisos` : noti.x);
const reco = await hasta(async () => { const x = await api('GET', '/api/recomendaciones/servicios', { token: tokTur }); return { ok: x.status === 200 && Array.isArray(x.json) && x.json.map(String).includes(String(servicioId)), x }; });
paso('recomendaciones incluye el servicio (ServicioCreado)', reco.ok, reco.x?.json);
r = await api('GET', '/api/operador/pagos', { token: tokOp });
paso('pagos del operador', r.status === 200 && Array.isArray(r.json) && r.json.length > 0, r.status);
r = await api('GET', '/api/operador/indicadores', { token: tokOp });
paso('indicadores operador', r.status === 200, r.json);

// 10. Seguridad
r = await api('POST', '/internal/eventos', { body: {} });
paso('gateway bloquea /internal', r.status === 404, r.status);
r = await api('GET', '/api/reservas/mias');
paso('sin token -> 401', r.status === 401, r.status);
r = await api('GET', '/api/operador/reservas', { token: tokTur });
paso('turista en ruta de operador -> 403', r.status === 403, r.status);

console.log(fallos === 0 ? '\nE2E: TODO OK' : `\nE2E: ${fallos} FALLO(S)`);
process.exit(fallos ? 1 : 0);
