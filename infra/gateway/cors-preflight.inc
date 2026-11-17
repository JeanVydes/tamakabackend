# Preflight CORS: se responde aqui, sin tocar los microservicios (no implementan OPTIONS).
# Incluido DENTRO de cada location que proxe a un servicio: add_header dentro de "if"
# sustituye a los heredados (por eso ACAO se repite) y el "return 204" cierra la peticion.
# Peticion autenticada o JSON => preflight: el navegador necesita ACAO/Methods/Headers aqui.
if ($request_method = OPTIONS) {
    add_header Access-Control-Allow-Origin * always;
    add_header Access-Control-Allow-Methods "GET, POST, PUT, PATCH, DELETE, OPTIONS" always;
    add_header Access-Control-Allow-Headers "Authorization, Content-Type" always;
    add_header Access-Control-Max-Age       3600 always;
    return 204;
}
