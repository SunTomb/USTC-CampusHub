root /srv/entry/site;
charset utf-8;
add_header Cache-Control "no-store" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "no-referrer" always;
add_header Content-Security-Policy "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'" always;
location = / { limit_except GET { deny all; } try_files /index.html =404; }
location ^~ /api/ { return 503; }
location / { return 404; }
