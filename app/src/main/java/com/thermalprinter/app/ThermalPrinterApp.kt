package com.thermalprinter.app

import android.app.Application
import com.thermalprinter.app.data.AppData
import com.thermalprinter.app.printer.BluetoothPrinterManager
import com.thermalprinter.app.subscription.SubscriptionManager

class ThermalPrinterApp : Application() {
    lateinit var appData: AppData
        private set

    lateinit var printerManager: BluetoothPrinterManager
        private set

    lateinit var subscriptionManager: SubscriptionManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        appData = AppData(this)
        printerManager = BluetoothPrinterManager(this)
        subscriptionManager = SubscriptionManager(this)
    }

    companion object {
        lateinit var instance: ThermalPrinterApp
            private set
    }
}
