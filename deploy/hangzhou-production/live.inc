client_max_body_size 6m;
# Historical payment-center integration is retired; never forward a callback.
location ^~ /api/payment/callbacks/ { return 410; }
location / {
    proxy_pass http://campushub-production-web:80;
    proxy_set_header Host ustc.suntomb.cn;
    proxy_set_header X-Forwarded-Proto https;
    proxy_set_header X-Forwarded-For $remote_addr;
    proxy_read_timeout 75s;
}
