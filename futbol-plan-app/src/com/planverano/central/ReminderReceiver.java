package com.planverano.central;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Muestra un recordatorio o, tras reiniciar el teléfono o cambiar la hora, vuelve a programarlos. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (Reminders.ACTION.equals(i.getAction())) {
            Reminders.fire(c, i.getIntExtra("idx", -1));
        } else {
            Reminders.scheduleAll(c);
        }
    }
}
