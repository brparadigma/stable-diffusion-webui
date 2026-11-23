# Plan Verano (app Android)

App personal con el plan de 10 semanas (9 dic 2026 – 12 feb 2027) para un defensor central:
sesiones por día, carga de tests que recalcula distancias de intermitentes (VAM) y pesos sugeridos,
y recordatorios 2 y 1 día antes de cada test y para cargar los resultados.

- `assets/index.html`: toda la interfaz (HTML/JS, funciona sin internet). Se puede probar en un navegador
  con `?hoy=2026-12-15` para simular la fecha.
- `src/`: actividad con WebView, puente JS (`Android.*`) y recordatorios con AlarmManager.
- `build.sh`: compila sin Android Studio usando aapt2, dx, zipalign y apksigner de Ubuntu.

Para instalar una versión nueva encima de la anterior hay que firmarla con la misma clave.
