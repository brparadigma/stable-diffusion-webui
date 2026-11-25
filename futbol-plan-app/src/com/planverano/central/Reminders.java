package com.planverano.central;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/** Recordatorios fijos del plan: 2 y 1 día antes de cada test, y aviso para cargar resultados. */
final class Reminders {
    static final String CHANNEL = "tests";
    static final String ACTION = "com.planverano.central.REMIND";
    static final String PREFS = "plan";

    // id, fecha y hora local, título, texto, pestaña a abrir, test que debe estar cargado para omitirlo (o null)
    static final String[][] EVENTS = {
        {"1", "2026-12-08 20:00", "Mañana arranca el plan",
            "Semana 1: sesión A el miércoles y C el viernes. Anotá con qué peso hacés 8 repeticiones con RIR 2 en sentadilla, peso muerto y press de banco con barra.", "hoy", null},
        {"2", "2026-12-10 20:00", "Test en 2 días (sábado 12)",
            "Sprint 30 m, salto vertical y 2000 m. Buscá un lugar con 400 m medidos o usá el GPS, y llevá el celular para filmar el sprint.", "tests", null},
        {"3", "2026-12-11 20:00", "Mañana es el test",
            "Cená hidratos, tomá agua y dormí bien. Hacé la entrada en calor completa antes de empezar.", "tests", null},
        {"4", "2026-12-12 20:30", "Cargá los resultados del test",
            "Con tu tiempo de 2000 m y tus pesos se actualizan los intermitentes y las cargas del plan.", "tests", "t1"},
        {"5", "2026-12-13 12:00", "Faltan los resultados del test 1",
            "Cargalos en la app para que el plan calcule tus distancias y pesos.", "tests", "t1"},

        {"6", "2027-01-07 20:00", "Test en 2 días (sábado 9)",
            "Segundo test: sprint 30 m, salto vertical y 2000 m. Mismo lugar, mismo calzado y mismo horario que en diciembre.", "tests", null},
        {"7", "2027-01-08 20:00", "Mañana es el test",
            "Cená hidratos, tomá agua y dormí bien. Hoy nada de alcohol.", "tests", null},
        {"8", "2027-01-09 20:30", "Cargá los resultados del test",
            "Con los nuevos datos se recalculan los intermitentes de enero y los pesos sugeridos.", "tests", "t2"},
        {"9", "2027-01-10 12:00", "Faltan los resultados del test 2",
            "Cargalos en la app para actualizar el plan.", "tests", "t2"},

        {"10", "2027-02-04 20:00", "Test final en 2 días (sábado 6)",
            "Último test antes de la pretemporada: sprint 30 m, salto vertical y 2000 m.", "tests", null},
        {"11", "2027-02-05 20:00", "Mañana es el test final",
            "Cená hidratos, tomá agua y dormí bien.", "tests", null},
        {"12", "2027-02-06 20:30", "Cargá los resultados del test final",
            "Vas a ver cuánto mejoraste desde diciembre.", "tests", "t3"},
        {"13", "2027-02-07 12:00", "Faltan los resultados del test final",
            "Cargalos en la app para ver tu progreso completo.", "tests", "t3"},

        {"14", "2027-02-12 20:00", "Mañana arranca la pretemporada",
            "Hoy descanso. Dormí bien y llegá con ganas.", "hoy", null},
    };

    private Reminders() {}

    static void createChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Tests y recordatorios",
            NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Avisos antes de cada test y para cargar los resultados");
        nm.createNotificationChannel(ch);
    }

    static long when(String s) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        f.setTimeZone(TimeZone.getDefault());
        try {
            return f.parse(s).getTime();
        } catch (ParseException e) {
            return 0L;
        }
    }

    static void scheduleAll(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        long now = System.currentTimeMillis();
        for (int k = 0; k < EVENTS.length; k++) {
            String[] e = EVENTS[k];
            long t = when(e[1]);
            Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION).putExtra("idx", k);
            PendingIntent pi = PendingIntent.getBroadcast(c, Integer.parseInt(e[0]), i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            if (t > now) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
            } else {
                am.cancel(pi);
            }
        }
    }

    static boolean isDone(Context c, String testId) {
        return testId != null && c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("done_" + testId, false);
    }

    static void fire(Context c, int idx) {
        if (idx < 0 || idx >= EVENTS.length) return;
        String[] e = EVENTS[idx];
        if (isDone(c, e[5])) return;
        post(c, Integer.parseInt(e[0]), e[2], e[3], e[4]);
    }

    static void post(Context c, int id, String title, String text, String tab) {
        if (Build.VERSION.SDK_INT >= 33
            && c.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        createChannel(c);
        Intent open = new Intent(c, MainActivity.class)
            .putExtra("tab", tab)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, id, open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFF16704A)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new Notification.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build();
        c.getSystemService(NotificationManager.class).notify(id, n);
    }

    static String json(Context c) {
        StringBuilder sb = new StringBuilder("[");
        for (int k = 0; k < EVENTS.length; k++) {
            String[] e = EVENTS[k];
            if (k > 0) sb.append(',');
            sb.append("{\"when\":\"").append(e[1]).append("\",\"title\":\"").append(esc(e[2]))
              .append("\",\"test\":").append(e[5] == null ? "null" : "\"" + e[5] + "\"")
              .append(",\"skipped\":").append(isDone(c, e[5])).append('}');
        }
        return sb.append(']').toString();
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
