package ru.motohelp.app.model.location

import org.greenrobot.eventbus.EventBus

class MyEventLocationSettingsChange(val on:Boolean) {
    companion object {
        var globalState=false //Set this for first time
        /**
         * In some devices change event is called twice. We limit this with internal state.
         * В некоторых устройствах событие change вызывается дважды. Мы ограничиваем это внутренним состоянием
         */
        fun setChangeAndPost(_on:Boolean) {
            if (globalState !=_on) { //Send Just Change
                globalState = _on;
                EventBus.getDefault().post(MyEventLocationSettingsChange(_on))
            }
        }
    }
}